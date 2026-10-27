package io.jenkins.plugins.specsmonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.model.Computer;
import net.sf.json.JSONObject;
import org.htmlunit.Page;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Reads the monitor data through the REST API. Jenkins exports the data of every monitor, so a
 * data class without {@code @ExportedBean} breaks the API of the whole node list.
 */
@WithJenkins
class RemoteApiTest {

    @Test
    void nodeListApiExportsMonitorData(JenkinsRule j) throws Exception {
        Computer computer = j.jenkins.toComputer();
        ColumnTestSupport.awaitData(CpuMonitor.DescriptorImpl.class, computer);
        ColumnTestSupport.awaitData(CpuUsageMonitor.DescriptorImpl.class, computer);
        ColumnTestSupport.awaitData(GpuMonitor.DescriptorImpl.class, computer);
        ColumnTestSupport.awaitData(MemoryMonitor.DescriptorImpl.class, computer);
        ColumnTestSupport.awaitData(JvmMemoryMonitor.DescriptorImpl.class, computer);
        ColumnTestSupport.awaitData(UptimeMonitor.DescriptorImpl.class, computer);
        ConnectionInfo connection = ColumnTestSupport.awaitData(ConnectionMonitor.DescriptorImpl.class, computer);

        JSONObject data = j.getJSON("computer/api/json?depth=1")
                .getJSONObject()
                .getJSONArray("computer")
                .getJSONObject(0)
                .getJSONObject("monitorData");

        assertTrue(data.getJSONObject(CpuMonitor.class.getName()).getInt("threads") > 0);
        assertTrue(data.getJSONObject(CpuUsageMonitor.class.getName()).getDouble("systemPercent") >= 0);
        assertNotNull(data.getJSONObject(GpuMonitor.class.getName()).getString("name"));
        assertTrue(data.getJSONObject(MemoryMonitor.class.getName()).getLong("totalBytes") > 0);
        assertTrue(data.getJSONObject(JvmMemoryMonitor.class.getName()).getLong("totalBytes") > 0);
        assertTrue(data.getJSONObject(UptimeMonitor.class.getName()).has("seconds"));
        assertEquals(
                "Built-in",
                data.getJSONObject(ConnectionMonitor.class.getName()).getString("display"));

        // The IP address is only shown on the Remote Access page, which needs Configure permission.
        String ip = connection.getIp();
        if (ip != null) {
            Page xml = j.createWebClient().goTo("computer/api/xml?depth=1", "application/xml");
            assertFalse(xml.getWebResponse().getContentAsString().contains(ip));
        }
    }
}
