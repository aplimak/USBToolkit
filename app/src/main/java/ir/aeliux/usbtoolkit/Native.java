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
        int ret = usbtkUsbCreateGadget(
                gadgetName,
                gadgetAtts,
                gadgetStrs,
                configAttrs,
                configStrs
        );

        if (ret != 0) {
            // it's bad, but we can't just skip it, so throw a general exception
            throw new RuntimeException("Native method returned with code " + ret);
        }
    }

    public static native void magicInit(String dbPath);
    public static native MagicResult magicAnalyzeFile(String path);
    public static native void magicRelease();

    private static native int usbtkUsbCreateGadget(String          gadgetName,
                                              UsbgGadgetAttrs gadgetAtts,
                                              UsbgGadgetStrs  gadgetStrs,
                                              UsbgConfigAttrs configAttrs,
                                              String          configStrs);
}
