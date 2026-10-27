package io.jenkins.plugins.specsmonitor;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

/** Reads the drives and mounted storage a node has access to, such as local disks, network shares and volumes. */
public final class StorageVolumes {

    /** File systems that expose kernel or container internals and not storage. */
    private static final Set<String> PSEUDO_TYPES = Set.of(
            "proc",
            "sysfs",
            "devtmpfs",
            "devpts",
            "tmpfs",
            "ramfs",
            "cgroup",
            "cgroup2",
            "securityfs",
            "debugfs",
            "tracefs",
            "pstore",
            "bpf",
            "configfs",
            "fusectl",
            "mqueue",
            "hugetlbfs",
            "autofs",
            "binfmt_misc",
            "squashfs",
            "efivarfs",
            "nsfs",
            "devfs");

    private StorageVolumes() {}

    /** A drive or mounted volume. */
    public static final class Volume implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String mount;
        private final String name;
        private final String type;
        private final String driveType;
        private final long totalBytes;
        private final long freeBytes;
        private final boolean readOnly;
        private final boolean workspace;

        Volume(
                String mount,
                String name,
                String type,
                String driveType,
                long totalBytes,
                long freeBytes,
                boolean readOnly,
                boolean workspace) {
            this.mount = mount;
            this.name = name;
            this.type = type;
            this.driveType = driveType;
            this.totalBytes = totalBytes;
            this.freeBytes = freeBytes;
            this.readOnly = readOnly;
            this.workspace = workspace;
        }

        /** Drive letter or mount point, such as {@code C:\} or {@code /data}. */
        public String getMount() {
            return mount;
        }

        /** Device or volume label, such as {@code /dev/sda1}, {@code //nas/share} or {@code Data}. */
        public String getName() {
            return name;
        }

        /** File system, such as {@code NTFS}, {@code ext4} or {@code overlay}. */
        public String getType() {
            return type;
        }

        /** {@code SSD} or {@code HDD}, or {@code null} if the node does not tell, as for network shares. */
        public String getDriveType() {
            return driveType;
        }

        public boolean isReadOnly() {
            return readOnly;
        }

        /** Whether the working directory of the node, where builds run, is on this volume. */
        public boolean isWorkspace() {
            return workspace;
        }

        public String getTotalDisplay() {
            return MemoryInfo.format(totalBytes);
        }

        /** Free space, with the share of the total, such as {@code 120.5 GiB (24%)}. */
        public String getFreeDisplay() {
            if (freeBytes < 0) {
                return MemoryInfo.format(freeBytes);
            }
            return MemoryInfo.format(freeBytes) + (totalBytes > 0 ? " (" + percentFree() + "%)" : "");
        }

        public int percentFree() {
            return totalBytes > 0 ? (int) Math.round(100.0 * freeBytes / totalBytes) : 0;
        }
    }

    /**
     * Lists the volumes of the machine this runs on.
     *
     * @param workspacePath the working directory of the node, used to mark the volume builds run on,
     *     or {@code null}
     */
    static List<Volume> collect(String workspacePath) {
        FileStore workspaceStore = storeOf(workspacePath);
        boolean windows =
                System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows");
        boolean mac = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("mac");
        List<Volume> volumes = new ArrayList<>();
        if (windows) {
            Map<String, String> types = DriveTypes.forWindows();
            for (File root : File.listRoots()) {
                String letter = root.getPath().substring(0, 1).toUpperCase(Locale.ROOT);
                add(volumes, root.toPath(), root.getPath(), workspaceStore, (mount, device) -> types.get(letter));
            }
        } else {
            BiFunction<String, String, String> driveType =
                    (mount, device) -> mac ? DriveTypes.forMac(mount) : DriveTypes.forLinux(device);
            for (FileStore store : FileSystems.getDefault().getFileStores()) {
                add(volumes, store, mountPoint(store.toString()), workspaceStore, driveType);
            }
        }
        return volumes;
    }

    private static void add(
            List<Volume> volumes,
            Path root,
            String mount,
            FileStore workspaceStore,
            BiFunction<String, String, String> driveType) {
        try {
            add(volumes, Files.getFileStore(root), mount, workspaceStore, driveType);
        } catch (IOException | RuntimeException e) {
            // Drives without media, such as card readers, cannot be read.
        }
    }

    private static void add(
            List<Volume> volumes,
            FileStore store,
            String mount,
            FileStore workspaceStore,
            BiFunction<String, String, String> driveType) {
        try {
            long total = store.getTotalSpace();
            if (total <= 0 || isPseudoFilesystem(store.type()) || !Files.isDirectory(Path.of(mount))) {
                return;
            }
            volumes.add(new Volume(
                    mount,
                    store.name(),
                    store.type(),
                    driveType.apply(mount, store.name()),
                    total,
                    store.getUsableSpace(),
                    store.isReadOnly(),
                    store.equals(workspaceStore)));
        } catch (IOException | RuntimeException e) {
            // A stale network mount can fail, which must not hide the other volumes.
        }
    }

    private static FileStore storeOf(String path) {
        if (path == null) {
            return null;
        }
        try {
            return Files.getFileStore(Path.of(path));
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    static boolean isPseudoFilesystem(String type) {
        return type == null || PSEUDO_TYPES.contains(type.toLowerCase(Locale.ROOT));
    }

    /** Extracts the mount point from {@code FileStore.toString()}, which is {@code /mnt/data (/dev/sdb1)}. */
    static String mountPoint(String fileStoreDescription) {
        int end = fileStoreDescription.lastIndexOf(" (");
        return end > 0 ? fileStoreDescription.substring(0, end) : fileStoreDescription;
    }
}
