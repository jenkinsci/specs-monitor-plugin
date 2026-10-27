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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
     * How long the lookup of all volumes may take. A dead network mount does not fail but blocks,
     * so every volume is read on its own thread and the slow ones are left out.
     */
    static final long TIMEOUT_SECONDS = 12;

    /**
     * Lists the volumes of the machine this runs on.
     *
     * @param workspacePath the working directory of the node, used to mark the volume builds run on,
     *     or {@code null}
     */
    static List<Volume> collect(String workspacePath) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        ExecutorService executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "specs-monitor storage lookup");
            t.setDaemon(true);
            return t;
        });
        try {
            Future<FileStore> workspace = executor.submit(() -> storeOf(workspacePath));
            List<Future<Volume>> lookups = new ArrayList<>();
            if (os.startsWith("windows")) {
                Future<Map<String, String>> types = executor.submit(DriveTypes::forWindows);
                for (File root : File.listRoots()) {
                    String letter = root.getPath().substring(0, 1).toUpperCase(Locale.ROOT);
                    lookups.add(executor.submit(() -> read(
                            Files.getFileStore(root.toPath()),
                            root.getPath(),
                            await(workspace, deadline),
                            (mount, device) -> {
                                Map<String, String> byLetter = await(types, deadline);
                                return byLetter == null ? null : byLetter.get(letter);
                            })));
                }
            } else {
                boolean mac = os.startsWith("mac");
                BiFunction<String, String, String> driveType =
                        (mount, device) -> mac ? DriveTypes.forMac(mount, device) : DriveTypes.forLinux(device);
                for (FileStore store : FileSystems.getDefault().getFileStores()) {
                    lookups.add(executor.submit(
                            () -> read(store, mountPoint(store.toString()), await(workspace, deadline), driveType)));
                }
            }
            List<Volume> volumes = new ArrayList<>();
            for (Future<Volume> lookup : lookups) {
                Volume volume = await(lookup, deadline);
                if (volume != null) {
                    volumes.add(volume);
                }
            }
            return volumes;
        } finally {
            // Threads blocked on a dead mount cannot be interrupted, but they are daemons.
            executor.shutdownNow();
        }
    }

    /** Reads one volume, or returns {@code null} if it is not storage, such as {@code proc}. */
    private static Volume read(
            FileStore store, String mount, FileStore workspaceStore, BiFunction<String, String, String> driveType)
            throws IOException {
        boolean workspace = store.equals(workspaceStore);
        long total = store.getTotalSpace();
        // The working directory is always shown, even on tmpfs, as in many container agents.
        if (total <= 0 || (!workspace && isPseudoFilesystem(store.type())) || !Files.isDirectory(Path.of(mount))) {
            return null;
        }
        return new Volume(
                mount,
                store.name(),
                store.type(),
                driveType.apply(mount, store.name()),
                total,
                store.getUsableSpace(),
                store.isReadOnly(),
                workspace);
    }

    /**
     * The result of a lookup, or {@code null} if it failed or did not finish in time. Drives without
     * media, such as card readers, fail, and dead network mounts do not finish.
     */
    private static <T> T await(Future<T> future, long deadline) {
        try {
            return future.get(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException e) {
            future.cancel(true);
        }
        return null;
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
