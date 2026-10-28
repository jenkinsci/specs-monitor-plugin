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
    private final double peakSystemPercent;

    /**
     * @param systemPercent  CPU usage of the whole machine (or container) in percent, or a negative
     *                       value if unknown
     * @param processPercent CPU usage of the agent's JVM in percent of the whole machine, or a
     *                       negative value if unknown
     */
    public CpuUsageInfo(double systemPercent, double processPercent) {
        this(systemPercent, processPercent, -1);
    }

    /**
     * @param peakSystemPercent the highest system CPU usage of the individual samples that
     *                          {@code systemPercent} is the mean of, or a negative value if unknown
     */
    public CpuUsageInfo(double systemPercent, double processPercent, double peakSystemPercent) {
        this.systemPercent = systemPercent;
        this.processPercent = processPercent;
        this.peakSystemPercent = peakSystemPercent;
    }

    @Exported
    public double getSystemPercent() {
        return systemPercent;
    }

    @Exported
    public double getProcessPercent() {
        return processPercent;
    }

    @Exported
    public double getPeakSystemPercent() {
        return peakSystemPercent;
    }

    /** System CPU usage, e.g. {@code 37%}, or {@code N/A}. */
    public String getDisplay() {
        return percent(systemPercent);
    }

    /**
     * Tooltip text, e.g. {@code System: 37% (peak 91%) • JVM: 2%}. The peak is only shown when it
     * differs from the average. Unknown parts are left out; empty if nothing is known.
     */
    public String getTooltip() {
        StringBuilder sb = new StringBuilder();
        if (systemPercent >= 0) {
            sb.append("System: ").append(percent(systemPercent));
            if (peakSystemPercent >= 0 && Math.round(peakSystemPercent) != Math.round(systemPercent)) {
                sb.append(" (peak ").append(percent(peakSystemPercent)).append(')');
            }
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
