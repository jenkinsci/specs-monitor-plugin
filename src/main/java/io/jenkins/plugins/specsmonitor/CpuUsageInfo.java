package io.jenkins.plugins.specsmonitor;

import java.io.Serializable;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/** CPU usage of a node and of the agent process on it. */
@ExportedBean
public class CpuUsageInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String NOT_AVAILABLE = "N/A";

    private final double systemPercent;
    private final double processPercent;

    /**
     * @param systemPercent  CPU usage of the whole machine (or container) in percent, or a negative
     *                       value if unknown
     * @param processPercent CPU usage of the agent's JVM in percent of the whole machine, or a
     *                       negative value if unknown
     */
    public CpuUsageInfo(double systemPercent, double processPercent) {
        this.systemPercent = systemPercent;
        this.processPercent = processPercent;
    }

    @Exported
    public double getSystemPercent() {
        return systemPercent;
    }

    @Exported
    public double getProcessPercent() {
        return processPercent;
    }

    /** System CPU usage, e.g. {@code 37%}, or {@code N/A}. */
    public String getDisplay() {
        return percent(systemPercent);
    }

    /**
     * Tooltip text, e.g. {@code System: 37% • JVM: 2%}. Unknown parts are left out; empty if
     * nothing is known.
     */
    public String getTooltip() {
        StringBuilder sb = new StringBuilder();
        if (systemPercent >= 0) {
            sb.append("System: ").append(percent(systemPercent));
        }
        if (processPercent >= 0) {
            if (sb.length() > 0) {
                sb.append(" • ");
            }
            sb.append("JVM: ").append(percent(processPercent));
        }
        return sb.toString();
    }

    static String percent(double value) {
        return value < 0 ? NOT_AVAILABLE : Math.round(value) + "%";
    }

    @Override
    public String toString() {
        return getDisplay();
    }
}
