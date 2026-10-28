# Specs Monitor

## Introduction

Specs Monitor adds columns to the Jenkins node list that show what each build machine is made of and
how it is set up: hardware, uptime, and how to reach the machine. This makes it easy to see which
agents are fast or slow without opening each one.

| Monitor | Shows | Example |
| --- | --- | --- |
| [CPU](#cpu-monitor) | CPU model, hardware threads and benchmark score | `i7-13700K (16 threads)` |
| [CPU Usage](#cpu-usage-monitor) | Current CPU usage, with a warning when the node is busy | `37%` |
| [Free RAM](#memory-monitor) | Available memory, with a warning when it runs low | `15.6 GiB` |
| [GPU](#gpu-monitor) | Graphics processor | `NVIDIA GeForce RTX 4070` |
| [Free JVM Memory](#jvm-memory-monitor) | Free heap of the node's JVM, with a warning when it runs low | `1.6 GiB` |
| [Uptime](#uptime-monitor) | Time since the machine last booted | `3d 4h` |
| [Connection](#connection-monitor) | How the node connects, plus its IP address | `WebSocket (192.168.1.20)` |

Hover over a value to see more detail, for example the full CPU name or the exact amount of memory.
Every monitor runs on the node itself, so it reports that node and not the controller. Each one can
be switched on separately, and none of them can take a node offline.

Together with the Connection monitor, the plugin also adds a [Remote access](#remote-access) link to
every node, so you can find a machine's IP address and connect to it over Remote Desktop or SSH.

### Supported systems

- **Operating systems:** Windows, Linux (x86, ARM and POWER) and macOS. The hardware and uptime monitors
  read the information with PowerShell on Windows, `/proc` and `lscpu` on Linux, and `sysctl` on macOS.
  On other systems, such as AIX or Solaris, or if a value cannot be determined, the monitor shows `N/A`.
- **Jenkins:** 2.541.3 or newer.
- **Nodes:** the built-in node and agents, however they connect: WebSocket, inbound (JNLP/TCP), SSH or
  a command launcher.
- **Configuration:** from the UI under **Manage Jenkins → Nodes → Configure Monitors**, or with
  Configuration as Code (see [Getting started](#getting-started)).

## Screenshots

<div align="center">
  <table>
    <tr>
      <td width="50%" align="center">
        <img src="docs/images/ScreenshotMonitorTable.png" alt="Jenkins node list showing CPU and memory monitor columns" width="296" />
        <br />
        Node list with the CPU and Free RAM columns enabled
      </td>
      <td width="50%" align="center">
        <img src="docs/images/ScreenshotMonitorData.png" alt="Monitor details showing CPU name, threads, and memory values" width="296" />
        <br />
        Example monitor data for a Jenkins node
      </td>
    </tr>
  </table>
</div>

## CPU monitor

A separate monitor, **CPU**, shows the CPU model and the number of hardware threads (logical processors) of every node as a
column, for example `i7-13700K (16 threads)`. Long vendor strings such as
`13th Gen Intel(R) Core(TM) i9-13900H` are shortened to `i9-13900H`. When a CPU model matches one in
the bundled Blender benchmark dataset, the column also shows the median benchmark score for that
hardware, from [Blender Open Data](https://opendata.blender.org/benchmarks/query/?compute_type=CPU&blender_version=5.2.0&group_by=device_name).
The bundled `src/main/resources/io/jenkins/plugins/specsmonitor/CpuMonitor/blender_open_data.csv`
contains the Blender device name and median score.

- Windows: PowerShell, with a registry fallback
- Linux: `lscpu`, with `/proc/cpuinfo` as a fallback, including x86, ARM (aarch64) and POWER
- macOS: `sysctl`

On other systems, or if the CPU name cannot be determined, the monitor shows `N/A`.

## CPU usage monitor

The **CPU Usage** monitor shows how busy the machine of every node is, for example `37%`. The usage
is measured on the node over one second each time the monitor refreshes, so it is a snapshot. When it
is at or above a configurable percentage (default 99%), it is shown in yellow with a warning symbol. Set the
threshold in the monitor's configuration (`warnAbovePercent` in Configuration as Code); 100 turns the
highlight off.

Hovering over the value shows, for example, `System: 37% • JVM: 2%`:

- **System:** usage of the whole machine, all processes included
- **JVM:** the share used by the Jenkins agent process itself. Build steps run as separate
  processes, so a high system value with a low JVM value means builds or other programs are busy.

Inside a container, the usage is relative to the container's CPU limit.

For graphs of CPU usage over time, the [Monitoring plugin](https://plugins.jenkins.io/monitoring/)
is a good complement.

## Memory monitor

A second monitor, **Free RAM**, shows the available or free memory (RAM) of every node as a
separate column, for example `15.6 GiB`. Sizes use binary units (1 GiB = 1024 MiB). Hovering over the value shows how much memory
is free out of the total (for example `43.5 GiB free of 63.7 GiB`) at the time of the last refresh. When the value drops below a configurable percentage of the
total (default 10%), it is shown in yellow with a warning symbol. Set the threshold in the
monitor's configuration (`warnBelowPercent` in Configuration as Code).

- Windows: `Win32_ComputerSystem.TotalPhysicalMemory` (PowerShell)
- Linux: `MemTotal` from `/proc/meminfo`
- macOS: `sysctl hw.memsize`
- Other systems and failed lookups: the value reported by the JVM, or `N/A`

The Linux value is the memory the kernel can use, which is slightly less than the installed amount.

## JVM memory monitor

The **Free JVM Memory** monitor shows the free memory of the Java virtual machine running on every
node, for example `1.6 GiB`. Free memory is the maximum heap size minus the heap in use. Hovering
over the value shows how much is free out of the maximum (for example `1.6 GiB free of 2 GiB`) at
the time of the last refresh. When free memory drops below a configurable percentage of the
maximum heap (default 10%), the value is shown in yellow with a warning symbol. Set the threshold
in the monitor's configuration (`warnBelowPercent` in Configuration as Code).

## Uptime monitor

The **Uptime** monitor shows how long the machine of every node has been running since its last boot,
for example `3d 4h`. Hovering over the value shows the full duration. It reads PowerShell on
Windows, `/proc/uptime` on Linux and `sysctl kern.boottime` on macOS. Otherwise, or if the lookup
fails, it shows the uptime of the agent process.

## Connection monitor

The **Connection** monitor shows how each node is connected to the controller: `WebSocket` or `JNLP`
(inbound agents over the TCP agent port), `SSH`, `Command`, or `Built-in` for the controller itself.
The IP address of the node follows in parentheses, for example `WebSocket (192.168.1.20)`. It is
the address the node uses for its default route, falling back to a private IPv4 address of its
network interfaces. The value is only reported while the node is online. Other launch methods are shown by their class name.

**Find a machine's IP and connect to it in seconds.** The node list shows the IP address of every
node right next to how it is connected, so there is no need to look it up in a hostname list, a
cloud console or on the machine itself. From there, open the node's page and use the
[Remote access](#remote-access) link to start a Remote Desktop session (Windows) or an SSH session
(Linux and macOS) with that address.

## Remote access

Each node's page has a **Remote access** link in its sidebar (requires the *Configure* permission on
the node). For Windows nodes it offers **Download .rdp file**, which opens in Remote Desktop Connection, and
**Open rdp:// link**, which needs a client that handles `rdp://` (for example the Microsoft Remote
Desktop or Windows App client; the built-in Windows client does not). For Linux and macOS nodes it offers **Connect with SSH**, an `ssh://`
link that opens the SSH client registered on your computer, plus the `ssh` command to copy. The
address is the one the node itself reports (see the
[Connection monitor](#connection-monitor)). While a node is offline, the last address it reported
is shown. Credentials are never stored or sent by Jenkins.

Below the connection options, the page lists the **Connected users**: the users who currently have
a session on the node, so you can see whether someone is logged in before you connect and take
over their machine. Windows nodes report them with `query user` (user, session such as `rdp-tcp#0`,
and state such as `Active`), Linux and macOS nodes with `who` (user, terminal and login time; a user
logged in more than once is listed once with all terminals). The list is read from the node when the
page opens, so it is only available while the node is connected to Jenkins.

## Storage

Each node's page also has a **Storage** link in its sidebar (requires the *Extended Read* permission
on the node). It lists the drives and volumes the node can use: mount point or drive letter, device,
file system, size and free space (with the percentage). The volume that holds the node's working
directory, where builds run, is marked **Builds**, and read-only volumes are flagged. This complements
Jenkins' own free disk space column, which only reports that one volume.

- Windows: every drive letter, including mapped network drives
- Linux and macOS: every mounted file system, including network shares, Docker volumes and the
  container's root file system
- The **Drive** column shows `SSD` (with `NVMe` where known) or `HDD` when the node can tell: from
  `Get-PhysicalDisk` on Windows, `/sys/block/*/queue/rotational` on Linux and `diskutil info` on macOS.
  The column is empty when the node does not say, which is common for network shares, containers and
  virtual disks. Some VMs also report a virtual disk as an HDD whatever hardware is behind it.
- A volume that does not answer within a few seconds, such as a dead network share, is left out, so
  one bad mount cannot block the page
- Kernel and container internals (`proc`, `tmpfs`, `squashfs` and similar) and drives without media are
  left out

The list is read from the node when the page opens, so it is only available while the node is connected
to Jenkins. In a container, the root file system reports the free space of the host disk behind it.

## GPU monitor

The **GPU** monitor shows the first graphics processor model returned by the OS inventory for each
node: Windows Management Instrumentation, `lspci` on Linux, or `system_profiler` on macOS. If the
OS inventory is unavailable, it shows the first result from `nvidia-smi`. Nodes without detectable
GPU information show `N/A`.

## Getting started

1. Install the plugin.
2. Go to **Manage Jenkins → Nodes → Configure Monitors** (gear icon).
3. Enable **CPU**, **CPU Usage**, **Free RAM**, **Free JVM Memory**, **GPU**, **Uptime** and/or **Connection**.
4. A column appears in the node list for each monitor you enabled, for example `i7-13700K (16 threads)`
   for CPU or `15.6 GiB` for Free RAM. Hover over a value for more detail.

### Configuration as Code

```yaml
jenkins:
  nodeMonitors:
    - cpuMonitor
    - cpuUsageMonitor:
        warnAbovePercent: 99
    - memoryMonitor
    - jvmMemoryMonitor
    - gpuMonitor
    - uptimeMonitor
    - connectionMonitor
```

## Issues

Report issues and enhancements on the
[GitHub issue tracker](https://github.com/jenkinsci/specs-monitor-plugin/issues).

## Contributing

Refer to the Jenkins
[contribution guidelines](https://github.com/jenkinsci/.github/blob/master/CONTRIBUTING.md).

To try the plugin locally:

```
mvn hpi:run
```

## License

Licensed under MIT, see [LICENSE.md](LICENSE.md).
