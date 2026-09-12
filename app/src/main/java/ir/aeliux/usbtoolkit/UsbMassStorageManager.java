package ir.aeliux.usbtoolkit;

import android.annotation.SuppressLint;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages a USB Mass Storage gadget on Android via configfs.
 * <p>
 * This class must be executed with root privileges (UID 0) because it directly
 * manipulates files under /config. It provides high‑level operations to:
 * <ul>
 *     <li>Set up a new mass storage gadget from one or more image files.</li>
 *     <li>Detect and query the state of a previously created gadget.</li>
 *     <li>Tear down (unbind and remove) the gadget.</li>
 *     <li>Query various USB‑related system information.</li>
 * </ul>
 * All methods throw {@link UsbGadgetException} with descriptive messages on failure.
 * The class is restart‑resistant: it uses a fixed gadget name so that a new instance
 * can detect an existing gadget and either take over or clean it up.
 */
@SuppressLint("MissingPermission")
public class UsbMassStorageManager {
    private static final String TAG = "UsbMassStorageManager";

    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------
    private static final String CONFIGFS_TYPE = "configfs";
    private static final String GADGETS_BASE = "usb_gadget";
    static final String GADGET_NAME = "mass_storage_gadget";   // fixed name for state detection
    private static final String FUNCTION_NAME = "mass_storage.0";
    private static final String CONFIG_NAME = "c.1";

    // USB IDs (example values from the original script)
    private static final int VENDOR_ID = 0x1d6b;
    private static final int PRODUCT_ID = 0x0104;
    private static final int BCD_USB = 0x0100;

    // Device class settings
    private static final int DEVICE_CLASS = 0xEF;      // Miscellaneous
    private static final int DEVICE_SUBCLASS = 2;
    private static final int DEVICE_PROTOCOL = 1;

    // String descriptors
    private static final String MANUFACTURER = "SAP Team";
    private static final String PRODUCT = "StickDroid";

    // Configuration attributes
    private static final int MAX_POWER_MA = 120;
    private static final int BM_ATTRIBUTES = 0x80;    // self‑powered

    private UsbMassStorageManager() {
        assertRoot("[constructor]");
    }

    /**
     * Single chokepoint for the root check.
     * Call this at the top of every public method AND the constructor.
     */
    private static void assertRoot(String where) {
        int uid = android.os.Process.myUid();
        if (uid != 0) {
            String msg = "UsbMassStorageManager." + where
                    + " called from non-root process"
                    + " (uid=" + uid
                    + ", pid=" + android.os.Process.myPid()
                    + ")";

            Log.e(TAG, msg, new SecurityException("caller trace"));

            throw new SecurityException(msg);
        }
    }

    // ------------------------------------------------------------------------
    // Configuration Object
    // ------------------------------------------------------------------------

    /**
     * Immutable configuration for a mass storage gadget setup.
     * Use the {@link Builder} to create an instance.
     */
    public static class MassStorageConfig {
        public final List<Path> imagePaths;
        public final boolean cdrom;
        public final boolean readOnly;
        public final boolean removable;

        private MassStorageConfig(Builder builder) {
            this.imagePaths = Collections.unmodifiableList(new ArrayList<>(builder.imagePaths));
            this.cdrom = builder.cdrom;
            this.readOnly = builder.readOnly;
            this.removable = builder.removable;
        }

        public static class Builder {
            private final List<Path> imagePaths = new ArrayList<>();
            private boolean cdrom = false;
            private boolean readOnly = false;
            private boolean removable = true;

            /**
             * Adds an image file (will be resolved to an absolute path during setup).
             */
            public Builder addImage(String image) {
                return addImage(new File(image).toPath());
            }

            /**
             * Adds an image file (will be resolved to an absolute path during setup).
             */
            public Builder addImage(Path image) {
                imagePaths.add(Objects.requireNonNull(image, "image path cannot be null"));
                return this;
            }

            public Builder setCdrom(boolean cdrom) {
                this.cdrom = cdrom;
                return this;
            }

            public Builder setReadOnly(boolean readOnly) {
                this.readOnly = readOnly;
                return this;
            }

            public Builder setRemovable(boolean removable) {
                this.removable = removable;
                return this;
            }

            public MassStorageConfig build() {
                if (imagePaths.isEmpty()) {
                    throw new IllegalArgumentException("At least one image path is required");
                }
                return new MassStorageConfig(this);
            }
        }
    }

    // ------------------------------------------------------------------------
    // Data Classes for State Information
    // ------------------------------------------------------------------------

    /**
     * Represents a snapshot of a gadget's state inside configfs.
     */
    public static class GadgetState {
        public final Path gadgetPath;
        public final boolean bound;
        public final String boundUdc;
        public final int vendorId;
        public final int productId;
        public final String manufacturer;
        public final String product;
        public final String serialNumber;
        public final Map<String, LunState> luns;

        GadgetState(Path gadgetPath, boolean bound, String boundUdc,
                    int vendorId, int productId, String manufacturer,
                    String product, String serialNumber,
                    Map<String, LunState> luns) {
            this.gadgetPath = gadgetPath;
            this.bound = bound;
            this.boundUdc = boundUdc;
            this.vendorId = vendorId;
            this.productId = productId;
            this.manufacturer = manufacturer;
            this.product = product;
            this.serialNumber = serialNumber;
            this.luns = Collections.unmodifiableMap(luns);
        }
    }

    /**
     * Represents the state of a single LUN (Logical Unit Number).
     */
    public static class LunState {
        public final int lunNumber;
        public final Path file;
        public final boolean cdrom;
        public final boolean readOnly;
        public final boolean removable;

        LunState(int lunNumber, Path file, boolean cdrom, boolean readOnly, boolean removable) {
            this.lunNumber = lunNumber;
            this.file = file;
            this.cdrom = cdrom;
            this.readOnly = readOnly;
            this.removable = removable;
        }
    }

    // ------------------------------------------------------------------------
    // Progress Callback Interface
    // ------------------------------------------------------------------------

    /**
     * Callback for receiving step‑by‑step progress during setup/teardown.
     */
    public interface ProgressCallback {
        /**
         * Called before starting a step.
         * @param stepName a short description of the step (e.g., "Create gadget directory").
         */
        void onStepStart(String stepName);

        /**
         * Called after a step completed successfully.
         * @param stepName the name of the step that finished.
         */
        void onStepComplete(String stepName);

        /**
         * Called if a step fails.
         * @param stepName the name of the step that failed.
         * @param error    the exception that caused the failure.
         */
        void onStepFailed(String stepName, UsbGadgetException error);
    }

    // ------------------------------------------------------------------------
    // Public Query Methods (USB Related)
    // ------------------------------------------------------------------------

    /**
     * Returns the mount point of configfs (usually {@code /config}).
     *
     * @return the configfs mount point as a {@link Path}.
     * @throws UsbGadgetException if configfs is not mounted or cannot be read.
     */
    public static Path getConfigfsMountPoint() throws UsbGadgetException {
        try (Stream<String> lines = Files.lines(new File("/proc/mounts").toPath())) {
            Optional<String> configfsLine = lines
                    .filter(line -> line.split("\\s+")[2].equals(CONFIGFS_TYPE))
                    .findFirst();
            if (configfsLine.isPresent()) {
                String[] parts = configfsLine.get().split("\\s+");
                return new File(parts[1]).toPath();  // mount point is the second field
            }
        } catch (IOException e) {
            throw new UsbGadgetException("Failed to read /proc/mounts", e);
        }
        throw new UsbGadgetException("configfs is not mounted");
    }

    /**
     * Lists available USB Device Controllers (UDC), excluding the 'dummy' controller.
     *
     * @return a sorted list of UDC names.
     * @throws UsbGadgetException if the UDC directory cannot be read.
     */
    public static List<String> getUdcList() throws UsbGadgetException {
        Path udcDir = new File("/sys/class/udc").toPath();
        if (!Files.exists(udcDir)) {
            throw new UsbGadgetException("UDC directory does not exist: " + udcDir);
        }
        try (Stream<Path> paths = Files.list(udcDir)) {
            return paths
                    .map(p -> p.getFileName().toString())
                    .filter(name -> !"dummy".equalsIgnoreCase(name))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UsbGadgetException("Failed to list UDC devices", e);
        }
    }

    /**
     * Checks whether a gadget with the given name exists in configfs.
     *
     * @param configfs   the configfs mount point.
     * @param gadgetName the name of the gadget.
     * @return true if the gadget directory exists.
     */
    public static boolean gadgetExists(Path configfs, String gadgetName) {
        return Files.exists(configfs.resolve(GADGETS_BASE).resolve(gadgetName));
    }

    /**
     * Checks if a gadget is currently bound to a UDC.
     *
     * @param gadgetPath the path to the gadget directory.
     * @return true if the UDC file contains a non‑empty controller name.
     * @throws UsbGadgetException if the UDC file cannot be read.
     */
    public static boolean isGadgetBound(Path gadgetPath) throws UsbGadgetException {
        Path udcFile = gadgetPath.resolve("UDC");
        if (!Files.exists(udcFile)) return false;
        try {
            String content = readString(udcFile.toFile()).trim();
            return !content.isEmpty();
        } catch (IOException e) {
            throw new UsbGadgetException("Failed to read UDC file: " + udcFile, e);
        }
    }

    /**
     * Obtains a detailed snapshot of a gadget's current state.
     *
     * @param configfs   the configfs mount point.
     * @param gadgetName the name of the gadget to inspect.
     * @return a {@link GadgetState} object containing all relevant information.
     * @throws UsbGadgetException if the gadget does not exist or cannot be parsed.
     */
    public static GadgetState getGadgetState(Path configfs, String gadgetName) throws UsbGadgetException {
        Path gadgetPath = configfs.resolve(GADGETS_BASE).resolve(gadgetName);
        if (!Files.exists(gadgetPath)) {
            throw new UsbGadgetException("Gadget does not exist: " + gadgetPath);
        }

        boolean bound = isGadgetBound(gadgetPath);
        String boundUdc = bound ? readConfigfsString(gadgetPath.resolve("UDC")) : null;
        int vendorId = readConfigfsInt(gadgetPath.resolve("idVendor"), 16);
        int productId = readConfigfsInt(gadgetPath.resolve("idProduct"), 16);

        // Read string descriptors (if present)
        String manufacturer = null, product = null, serial = null;
        Path stringsBase = gadgetPath.resolve("strings/0x409");
        if (Files.exists(stringsBase)) {
            manufacturer = readConfigfsString(stringsBase.resolve("manufacturer"));
            product = readConfigfsString(stringsBase.resolve("product"));
            serial = readConfigfsString(stringsBase.resolve("serialnumber"));
        }

        // Parse LUNs from the mass_storage function
        Map<String, LunState> luns = new LinkedHashMap<>();
        Path functionPath = gadgetPath.resolve("functions").resolve(FUNCTION_NAME);
        if (Files.exists(functionPath)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(functionPath, "lun.*")) {
                for (Path lunPath : stream) {
                    String name = lunPath.getFileName().toString();
                    int lunNumber = Integer.parseInt(name.substring(4)); // after "lun."
                    boolean cdrom = parseBoolean(readConfigfsString(lunPath.resolve("cdrom")));
                    boolean ro = parseBoolean(readConfigfsString(lunPath.resolve("ro")));
                    boolean removable = parseBoolean(readConfigfsString(lunPath.resolve("removable")));
                    String fileStr = readConfigfsString(lunPath.resolve("file"));
                    Path file = fileStr.isEmpty() ? null : new File(fileStr).toPath();
                    LunState lun = new LunState(lunNumber, file, cdrom, ro, removable);
                    luns.put(name, lun);
                }
            } catch (IOException | NumberFormatException e) {
                throw new UsbGadgetException("Failed to parse LUN information", e);
            }
        }

        return new GadgetState(gadgetPath, bound, boundUdc, vendorId, productId,
                manufacturer, product, serial, luns);
    }

    /**
     * Convenience method to detect if our own gadget (with the fixed name) is present.
     *
     * @return the {@link GadgetState} of the gadget, or null if it does not exist.
     * @throws UsbGadgetException if an error occurs while reading the state.
     */
    public static GadgetState detectExistingGadget() throws UsbGadgetException {
        Path configfs = getConfigfsMountPoint();
        if (gadgetExists(configfs, GADGET_NAME)) {
            return getGadgetState(configfs, GADGET_NAME);
        }
        return null;
    }

    // ------------------------------------------------------------------------
    // Cleanup / Teardown
    // ------------------------------------------------------------------------

    /**
     * Removes a gadget completely from configfs.
     * If the gadget is bound, it is first unbound. The operation is idempotent:
     * calling it on a non‑existent gadget does nothing.
     *
     * @param configfs   the configfs mount point.
     * @param gadgetName the name of the gadget to remove.
     * @throws UsbGadgetException if any step fails.
     */
    public static void cleanupGadget(Path configfs, String gadgetName) throws UsbGadgetException {
        cleanupGadget(configfs, gadgetName, null);
    }

    /**
     * Overloaded cleanup with progress callback.
     */
    public static void cleanupGadget(Path configfs, String gadgetName,
                                     ProgressCallback callback) throws UsbGadgetException {
        Path gadgetPath = configfs.resolve(GADGETS_BASE).resolve(gadgetName);
        if (!Files.exists(gadgetPath)) {
            return; // already clean
        }

        // Step 1: Unbind if bound
        step(callback, "Unbind gadget", () -> {
            Path udcFile = gadgetPath.resolve("UDC");
            if (Files.exists(udcFile)) {
                writeConfigfsString(udcFile, "");
            }
        });

        // Step 2: Remove configuration directories and symlinks
        step(callback, "Remove configurations", () -> {
            Path configsBase = gadgetPath.resolve("configs");
            if (Files.exists(configsBase)) {
                try (DirectoryStream<Path> configs = Files.newDirectoryStream(configsBase)) {
                    for (Path configDir : configs) {
                        // Remove symlinks and subdirectories inside config dir
                        try (DirectoryStream<Path> entries = Files.newDirectoryStream(configDir)) {
                            for (Path entry : entries) {
                                if (Files.isSymbolicLink(entry) || Files.isDirectory(entry)) {
                                    deleteRecursively(entry);
                                } else if (Files.isRegularFile(entry)) {
                                    Files.delete(entry);
                                }
                            }
                        }
                        Files.delete(configDir);
                    }
                }
            }
        });

        // Step 3: Remove functions
        step(callback, "Remove functions", () -> {
            Path functionsBase = gadgetPath.resolve("functions");
            if (Files.exists(functionsBase)) {
                try (DirectoryStream<Path> functions = Files.newDirectoryStream(functionsBase)) {
                    for (Path functionDir : functions) {
                        deleteRecursively(functionDir);
                    }
                }
            }
        });

        // Step 4: Remove strings
        step(callback, "Remove strings", () -> {
            Path stringsBase = gadgetPath.resolve("strings");
            if (Files.exists(stringsBase)) {
                deleteRecursively(stringsBase);
            }
        });

        // Step 5: Delete gadget directory
        step(callback, "Delete gadget directory", () -> {
            Files.delete(gadgetPath);
        });
    }

    // ------------------------------------------------------------------------
    // Setup
    // ------------------------------------------------------------------------

    /**
     * Sets up a new mass storage gadget from scratch.
     * If a gadget with the fixed name already exists, it is first cleaned up.
     * The method performs the following steps (each reported via callback):
     * <ol>
     *     <li>Locate configfs and clean any previous instance.</li>
     *     <li>Create gadget directory and set USB IDs/class.</li>
     *     <li>Set string descriptors (manufacturer, product, serial).</li>
     *     <li>Create the mass_storage function and add LUNs.</li>
     *     <li>Create a configuration and link the function.</li>
     *     <li>Bind the gadget to the first available UDC.</li>
     * </ol>
     *
     * @param config   the configuration describing images and options.
     * @param callback optional progress callback; may be null.
     * @return the {@link GadgetState} of the newly created gadget.
     * @throws UsbGadgetException if any step fails. On failure, an attempt is made
     *                            to clean up partial state.
     */
    public static GadgetState setupMassStorage(MassStorageConfig config,
                                               ProgressCallback callback) throws UsbGadgetException {
        Path configfs = getConfigfsMountPoint();

        // Step 0: Clean existing instance (if any)
        step(callback, "Clean previous gadget", () -> {
            cleanupGadget(configfs, GADGET_NAME, callback);
        });

        Path gadgetPath = configfs.resolve(GADGETS_BASE).resolve(GADGET_NAME);

        // Step 1: Create gadget directory and IDs/class
        step(callback, "Create gadget and set IDs", () -> {
            Files.createDirectories(gadgetPath);
            writeConfigfsInt(gadgetPath.resolve("idVendor"), VENDOR_ID, 16);
            writeConfigfsInt(gadgetPath.resolve("idProduct"), PRODUCT_ID, 16);
            writeConfigfsInt(gadgetPath.resolve("bcdUSB"), BCD_USB, 16);
            writeConfigfsInt(gadgetPath.resolve("bDeviceClass"), DEVICE_CLASS, 16);
            writeConfigfsInt(gadgetPath.resolve("bDeviceSubClass"), DEVICE_SUBCLASS, 16);
            writeConfigfsInt(gadgetPath.resolve("bDeviceProtocol"), DEVICE_PROTOCOL, 16);
        });

        // Step 2: String descriptors
        step(callback, "Set string descriptors", () -> {
            Path stringsDir = gadgetPath.resolve("strings/0x409");
            Files.createDirectories(stringsDir);
            writeConfigfsString(stringsDir.resolve("serialnumber"), getSerialNumber());
            writeConfigfsString(stringsDir.resolve("manufacturer"), MANUFACTURER);
            writeConfigfsString(stringsDir.resolve("product"), PRODUCT);
        });

        // Step 3: Create mass storage function and LUNs
        step(callback, "Create mass storage function and LUNs", () -> {
            Path functionPath = gadgetPath.resolve("functions").resolve(FUNCTION_NAME);
            Files.createDirectories(functionPath);

            int lunIndex = 0;
            for (Path image : config.imagePaths) {
                // Resolve image to an absolute, existing path
                Path resolvedImage;
                try {
                    resolvedImage = image.toRealPath();
                    if (!Files.isRegularFile(resolvedImage)) {
                        throw new UsbGadgetException("Image file does not exist or is not a regular file: " + resolvedImage);
                    }
                } catch (IOException e) {
                    throw new UsbGadgetException("Failed to resolve image path: " + image, e);
                }

                Path lunPath = functionPath.resolve("lun." + lunIndex);
                Files.createDirectory(lunPath);

                // Order matters: set ro, removable, cdrom BEFORE writing the file
                writeConfigfsBoolean(lunPath.resolve("ro"), config.readOnly);
                writeConfigfsBoolean(lunPath.resolve("removable"), config.removable);
                writeConfigfsBoolean(lunPath.resolve("cdrom"), config.cdrom);
                writeConfigfsString(lunPath.resolve("file"), resolvedImage.toString());

                lunIndex++;
            }
        });

        // Step 4: Create configuration and link function
        step(callback, "Create configuration and link function", () -> {
            Path configDir = gadgetPath.resolve("configs").resolve(CONFIG_NAME);
            Path configStringsDir = configDir.resolve("strings/0x409");
            Files.createDirectories(configStringsDir);
            writeConfigfsString(configStringsDir.resolve("configuration"), "Mass Storage config");
            writeConfigfsInt(configDir.resolve("MaxPower"), MAX_POWER_MA, 10);
            writeConfigfsInt(configDir.resolve("bmAttributes"), BM_ATTRIBUTES, 16);

            // Create symlink: configs/c.1/mass_storage.0 -> ../../functions/mass_storage.0
            Path linkPath = configDir.resolve(FUNCTION_NAME);
            Files.createSymbolicLink(linkPath, new File("../../functions/" + FUNCTION_NAME).toPath());
        });

        // Step 5: Bind to first available UDC
        step(callback, "Bind to UDC", () -> {
            List<String> udcs = getUdcList();
            if (udcs.isEmpty()) {
                throw new UsbGadgetException("No UDC available for binding");
            }
            writeConfigfsString(gadgetPath.resolve("UDC"), udcs.get(0));
        });

        // Return the final state
        return getGadgetState(configfs, GADGET_NAME);
    }

    /**
     * Convenience overload without callback.
     */
    public static GadgetState setupMassStorage(MassStorageConfig config) throws UsbGadgetException {
        return setupMassStorage(config, null);
    }

    // ------------------------------------------------------------------------
    // USB Reset (optional)
    // ------------------------------------------------------------------------

    /**
     * Resets the USB gadget stack by invoking the Android {@code svc} command.
     * This can help re‑enable the default gadget after our custom one is removed.
     * <p>
     * This method uses {@link Runtime#exec(String)} to run
     * {@code svc usb resetUsbGadget} and {@code svc usb resetUsbPort}.
     *
     * @throws UsbGadgetException if the commands fail.
     */
    public static void resetUsbGadget() throws UsbGadgetException {
        try {
            Process p1 = Runtime.getRuntime().exec("svc usb resetUsbGadget");
            p1.waitFor();
            Process p2 = Runtime.getRuntime().exec("svc usb resetUsbPort");
            p2.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new UsbGadgetException("Failed to reset USB gadget", e);
        }
    }

    // ------------------------------------------------------------------------
    // Private Helper Methods
    // ------------------------------------------------------------------------

    /**
     * Reads a string from a configfs file and trims whitespace.
     */
    private static String readConfigfsString(Path path) throws UsbGadgetException {
        try {
            if (!Files.exists(path)) {
                throw new UsbGadgetException("File not found: " + path);
            }
            return readString(path.toFile()).trim();
        } catch (IOException e) {
            throw new UsbGadgetException("Failed to read " + path, e);
        }
    }

    public static String readString(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[8192];
        try (Reader reader = new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8)) {
            int n;
            while ((n = reader.read(buf)) != -1) {
                sb.append(buf, 0, n);
            }
        }
        return sb.toString();
    }

    /**
     * Reads an integer from a configfs file using the given radix.
     */
    private static int readConfigfsInt(Path path, int radix) throws UsbGadgetException {
        String str = readConfigfsString(path);
        try {
            return Integer.parseInt(str, radix);
        } catch (NumberFormatException e) {
            throw new UsbGadgetException("Invalid integer at " + path + ": " + str, e);
        }
    }

    /**
     * Writes a string to a configfs file without a trailing newline.
     */
    private static void writeConfigfsString(Path path, String value) throws UsbGadgetException {
        try {
            Files.write(path, value.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UsbGadgetException("Failed to write '" + value + "' to " + path, e);
        }
    }

    /**
     * Writes an integer to a configfs file in the given radix (e.g., 16 for hex).
     */
    private static void writeConfigfsInt(Path path, int value, int radix) throws UsbGadgetException {
        writeConfigfsString(path, Integer.toString(value, radix));
    }

    /**
     * Writes a boolean as "1" or "0" to a configfs file.
     */
    private static void writeConfigfsBoolean(Path path, boolean value) throws UsbGadgetException {
        writeConfigfsString(path, value ? "1" : "0");
    }

    /**
     * Parses a boolean from a configfs string: "1", "y", "yes" are true; "0", "n", "no" are false.
     */
    private static boolean parseBoolean(String value) {
        return value.equalsIgnoreCase("1") || value.equalsIgnoreCase("y") || value.equalsIgnoreCase("yes");
    }

    /**
     * Deletes a file or directory recursively.
     */
    private static void deleteRecursively(Path path) throws UsbGadgetException {
        if (!Files.exists(path)) return;
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UsbGadgetException("Failed to delete " + path, e);
        }
    }

    /**
     * Executes a step while notifying the progress callback.
     * If the step throws an exception, it is wrapped in a {@link UsbGadgetException}
     * and the callback is informed about the failure.
     */
    private static void step(ProgressCallback callback, String stepName,
                             StepAction action) throws UsbGadgetException {
        if (callback != null) callback.onStepStart(stepName);
        try {
            action.run();
            if (callback != null) callback.onStepComplete(stepName);
        } catch (UsbGadgetException e) {
            if (callback != null) callback.onStepFailed(stepName, e);
            throw e;
        } catch (Exception e) {
            UsbGadgetException wrapped = new UsbGadgetException("Error during step: " + stepName, e);
            if (callback != null) callback.onStepFailed(stepName, wrapped);
            throw wrapped;
        }
    }

    /**
     * Functional interface for a step that can throw checked exceptions.
     */
    @FunctionalInterface
    private interface StepAction {
        void run() throws Exception;
    }

    /**
     * Retrieves the device serial number, falling back to a default if unavailable.
     */
    private static String getSerialNumber() {
        try {
            return Build.getSerial();
        } catch (Exception e) {
            return "0123456789ABCDEF";
        }
    }
}