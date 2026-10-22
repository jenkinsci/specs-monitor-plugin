package io.jenkins.plugins.specsmonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.slaves.DumbSlave;
import hudson.slaves.OfflineCause;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class RemoteAccessActionTest {

    @Test
    void actionIsAddedToComputer(JenkinsRule j) {
        assertTrue(j.jenkins.toComputer().getActions(RemoteAccessAction.class).size() > 0);
    }

    @Test
    void targetOfBuiltInNodeIsReported(JenkinsRule j) {
        RemoteAccessAction action = j.jenkins.toComputer().getAction(RemoteAccessAction.class);

        RemoteTarget target = action.getTarget();

        assertNotNull(target);
        assertNotNull(target.getFamily());
    }

    @Test
    void offlineNodeFallsBackToLastCollectedAddress(JenkinsRule j) throws Exception {
        DumbSlave agent = j.createOnlineSlave();
        ColumnTestSupport.enable(new ConnectionMonitor());
        ConnectionInfo info = ColumnTestSupport.awaitData(ConnectionMonitor.DescriptorImpl.class, agent.toComputer());
        assertNotNull(info.getIp(), "the agent reported no address");

        agent.toComputer().setTemporaryOfflineCause(new OfflineCause.UserCause(null, "test"));
        agent.toComputer().disconnect(null).get();

        RemoteTarget target =
                agent.toComputer().getAction(RemoteAccessAction.class).getTarget();

        assertNotNull(target);
        assertEquals(info.getIp(), target.getIp());
    }

    @Test
    void pageRenders(JenkinsRule j) throws Exception {
        try (JenkinsRule.WebClient wc = j.createWebClient()) {
            HtmlPage page = wc.goTo("computer/(built-in)/remote-access/");

            assertEquals("Remote Access", page.getElementsByTagName("h1").get(0).getTextContent());
        }
    }
}
