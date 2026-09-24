package ir.aeliux.usbtoolkit;

import ir.aeliux.usbtoolkit.data.MagicResult;
import ir.aeliux.usbtoolkit.dto.UsbgConfigAttrs;
import ir.aeliux.usbtoolkit.dto.UsbgGadgetAttrs;
import ir.aeliux.usbtoolkit.dto.UsbgGadgetStrs;

public class Native {
    private Native() {}

    static {
        System.loadLibrary("usbtoolkit");
    }

    public static native void magicInit(String dbPath);
    public static native MagicResult magicAnalyzeFile(String path);
    public static native void magicRelease();

    private static native int usbCreateGadget(String          gadgetName,
                                              UsbgGadgetAttrs gadgetAtts,
                                              UsbgGadgetStrs  gadgetStrs,
                                              UsbgConfigAttrs configAttrs,
                                              String          configStrs);
}
