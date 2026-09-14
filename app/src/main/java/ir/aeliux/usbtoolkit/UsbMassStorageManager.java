package ir.aeliux.usbtoolkit;

import android.annotation.SuppressLint;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ir.aeliux.usbtoolkit.data.GadgetState;
import ir.aeliux.usbtoolkit.data.LunState;
import ir.aeliux.usbtoolkit.data.MassStorageConfig;

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
    public static final String GADGET_NAME = "mass_storage_gadget";   // fixed name for state detection
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
                    .filter(name -> !name.contains("dummy"))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UsbGadgetException("Failed to list UDC devices", e);
        }
    }

    /**
     * Lists existing USB Gadgets.
     *
     * @return a sorted list of USB Gadget names.
     * @throws UsbGadgetException if the Gadgets directory cannot be read.
     */
    public static List<String> getGadgetList(Path configfs) throws UsbGadgetException {
        Path gadgetPath = configfs.resolve(GADGETS_BASE);
        if (!Files.exists(gadgetPath)) {
            throw new UsbGadgetException("gadgets directory does not exist: " + gadgetPath);
        }
        try (Stream<Path> paths = Files.list(gadgetPath)) {
            return paths
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UsbGadgetException("Failed to list Gadgets", e);
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
     * Removes a gadget completely from configfs using configfs‑legal operations
     * (only {@code rmdir} and unlink of symlinks). Idempotent: calling it on a
     * non‑existent gadget does nothing.
     * <p>
     * While the gadget is being torn down, {@code sys.usb.config} is temporarily
     * set to {@code none} so init does not race us by re‑binding {@code g1}.
     * The previous value is restored in a {@code finally} block.
     *
     * @param configfs   the configfs mount point.
     * @param gadgetName the name of the gadget to remove.
     * @param callback   optional progress callback; may be {@code null}.
     * @throws UsbGadgetException if any step fails.
     */
    public static void cleanupGadget(Path configfs, String gadgetName,
                                     ProgressCallback callback) throws UsbGadgetException {
        Path gadgetPath = configfs.resolve(GADGETS_BASE).resolve(gadgetName);
        if (!Files.exists(gadgetPath)) {
            return;
        }

        // Step 1: Unbind from UDC
        step(callback, "Unbind gadget", () -> {
            Path udcFile = gadgetPath.resolve("UDC");
            if (Files.exists(udcFile)) {
                writeConfigfsString(udcFile, "");
            }
        });

        // Step 2: Remove configurations
        //   - unlink function symlinks inside each config
        //   - rmdir config/strings/0x409, then config/strings
        //   - rmdir config
        step(callback, "Remove configurations", () -> {
            Path configsBase = gadgetPath.resolve("configs");
            if (!Files.exists(configsBase)) return;

            try (DirectoryStream<Path> configs = Files.newDirectoryStream(configsBase)) {
                for (Path configDir : configs) {
                    // 2a. Unlink symlinks (e.g. mass_storage.0) — they block rmdir.
                    try (DirectoryStream<Path> entries = Files.newDirectoryStream(configDir)) {
                        for (Path entry : entries) {
                            if (Files.isSymbolicLink(entry)) {
                                Files.delete(entry);
                            }
                        }
                    }

                    // 2b. rmdir strings/0x409, then strings/
                    Path stringsDir = configDir.resolve("strings");
                    if (Files.exists(stringsDir)) {
                        rmdirChildren(stringsDir);     // removes 0x409
                    }

                    // 2c. rmdir config itself. Attributes (MaxPower, bmAttributes,
                    //     configuration, …) disappear with the directory.
                    Files.delete(configDir);
                }
            }
        });

        // Step 3: Remove functions
        //   - rmdir each lun.N
        //   - rmdir the function directory (attributes vanish with it)
        step(callback, "Remove functions", () -> {
            Path functionsBase = gadgetPath.resolve("functions");
            if (!Files.exists(functionsBase)) return;

            try (DirectoryStream<Path> functions = Files.newDirectoryStream(functionsBase)) {
                for (Path functionDir : functions) {
                    try {
                        rmdirChildren(functionDir);        // removes lun.0, lun.1, …
                    } catch (FileSystemException ignored) {}
                    Files.delete(functionDir);         // rmdir mass_storage.0
                }
            }
        });

        // Step 4: Remove strings
        //   - rmdir strings/0x409, then strings
        step(callback, "Remove strings", () -> {
            Path stringsDir = gadgetPath.resolve("strings");
            if (!Files.exists(stringsDir)) return;
            rmdirChildren(stringsDir);                 // removes 0x409
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
                Files.createDirectories(lunPath);

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
            Path linkPath   = configDir.resolve(FUNCTION_NAME);
            Path targetPath = gadgetPath.resolve("functions")
                    .resolve(FUNCTION_NAME)
                    .toAbsolutePath()
                    .normalize();
            Files.createSymbolicLink(linkPath, targetPath);
        });

        // Step 5: Bind to first available UDC
        step(callback, "Bind to UDC", () -> {
            var gadgetUdc = gadgetPath.resolve("UDC");
            var selectedUdc = config.udc;

            StringBuilder existingUdcs = new StringBuilder("echo unbinding");

            try (var gadgets = Files.list(gadgetPath.getParent())) {
                var gadgetsUdc = gadgets.map(g -> g.resolve("UDC"))
                        .filter(path -> {
                            try {
                                return readConfigfsString(path).equals(selectedUdc);
                            } catch (UsbGadgetException e) {
                                return false;
                            }
                        })
                        .collect(Collectors.toList());

                for (Path gUdc : gadgetsUdc) {
                    existingUdcs.append(String.format(" && echo > '%s'", gUdc));
                }
            }

            // Android likes to mess with us, so we do it faster than android could ever react
            String shellCmd = String.format("%s && echo %s >%s", existingUdcs, selectedUdc, gadgetUdc);
            Log.d(TAG, "Executing: sh -c " + shellCmd);
            Process process = new ProcessBuilder("sh", "-c", shellCmd).start();
            process.waitFor();
            String err = readStream(process.getErrorStream());
            String out = readStream(process.getInputStream());
            Log.d(TAG, "exec Result:\nErrorStream: " + err + "\nOutputStream: " + out);
            int exitCode = process.exitValue();
            if (exitCode > 0) {
                throw new UsbGadgetException("UDC binding atomic process failed with exit code: " + exitCode);
            }
        });

        // Return the final state
        return getGadgetState(configfs, GADGET_NAME);
    }

    static String readStream(InputStream in) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[4096];
        int n;
        while ((n = r.read(buf)) != -1) sb.append(buf, 0, n);
        return sb.toString();
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
        if (radix == 16 && (str.startsWith("0x") || str.startsWith("0X"))) {
            str = str.substring(2);
        }
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
        writeConfigfsString(path,(radix == 16 ? "0x" : "") + Integer.toString(value, radix));
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
     * Removes all immediate child <em>directories</em> and <em>symlinks</em> of
     * {@code dir}, but never touches regular files.
     * <p>
     * This is required for configfs: attribute files inside a configfs directory
     * (e.g. {@code configuration}, {@code MaxPower}, {@code ro}, {@code file})
     * cannot be unlinked individually — the kernel returns {@code EPERM}.
     * The only valid removal operation is {@code rmdir} on the containing
     * directory, after which the kernel discards the attributes.
     *
     * @param dir the directory whose children should be removed.
     * @throws IOException if any {@code rmdir}/unlink fails.
     */
    private static void rmdirChildren(Path dir) throws IOException {
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(dir)) {
            for (Path entry : entries) {
                if (Files.isSymbolicLink(entry)) {
                    Files.delete(entry);                              // unlink symlink
                } else if (Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS)) {
                    Files.delete(entry);                              // rmdir (must already be empty)
                }
                // Regular files (configfs attributes) are intentionally left alone.
            }
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
            return getSystemProperty("ro.boot.serialno");
        } catch (Exception e) {
            return "0123456789ABCDEF";
        }
    }

    private static String getSystemProperty(String key) {
        String value = null;
        try {
            Class<?> systemPropertiesClass = Class.forName("android.os.SystemProperties");
            Method getMethod = systemPropertiesClass.getMethod("get", String.class);
            value = (String) getMethod.invoke(null, key);
        } catch (Exception e) {
            // Handle exceptions (e.g., class not found, method not found)
            e.printStackTrace();
        }
        return value;
    }
}