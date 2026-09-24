package ir.aeliux.usbtoolkit;

import android.util.Log;

import ir.aeliux.usbtoolkit.data.MagicResult;
import ir.aeliux.usbtoolkit.dto.UsbgConfigAttrs;
import ir.aeliux.usbtoolkit.dto.UsbgGadgetAttrs;
import ir.aeliux.usbtoolkit.dto.UsbgGadgetStrs;

public class Native {
    private static final String TAG = "Native";
    private Native() {}

    static {
        System.loadLibrary("usbtoolkit");
    }

    public static void usbCreateGadget(String         gadgetName,
                                      UsbgGadgetAttrs gadgetAtts,
                                      UsbgGadgetStrs  gadgetStrs,
                                      UsbgConfigAttrs configAttrs,
                                      String          configStrs) {
        Log.d(TAG, "Calling native method usbtkUsbCreateGadget");
        usbtkUsbCreateGadget(
                gadgetName,
                gadgetAtts,
                gadgetStrs,
                configAttrs,
                configStrs
        );
    }

    public static void usbSetConfigfsPath(String configfsPath) {
        Log.d(TAG, "Calling native method usbtkUsbSetConfigfsPath");
        usbtkUsbSetConfigfsPath(configfsPath);
    }

    private static void usbThrow(int returnCode) {
        if (returnCode != 0) {
            // it's bad, but we can't just skip it, so throw a general exception
            throw new RuntimeException("Native method returned with code " + returnCode);
        }
    }

    public static native void magicInit(String dbPath);
    public static native MagicResult magicAnalyzeFile(String path);
    public static native void magicRelease();

    private static native void usbtkUsbCreateGadget(String          gadgetName,
                                              UsbgGadgetAttrs gadgetAtts,
                                              UsbgGadgetStrs  gadgetStrs,
                                              UsbgConfigAttrs configAttrs,
                                              String          configStrs);

    private static native void usbtkUsbSetConfigfsPath(String configfsPath);
}
