package ir.aeliux.usbtoolkit;

import ir.aeliux.usbtoolkit.data.MagicResult;
import ir.aeliux.usbtoolkit.dto.UsbgConfigAttrs;
import ir.aeliux.usbtoolkit.dto.UsbgGadgetAttrs;
import ir.aeliux.usbtoolkit.dto.UsbgGadgetStrs;

public class Native {
    static {
        System.loadLibrary("usbtoolkit");
    }

    private Native() {}

    static native long usbtkUsbCreateGadget(long ptrwConfigfs,
                                            String gadgetName,
                                            UsbgGadgetAttrs gadgetAtts,
                                            UsbgGadgetStrs gadgetStrs,
                                            UsbgConfigAttrs configAttrs,
                                            String configStrs);

    static native long usbtkUsbOpenGadget(long ptrwConfigfs,
                                          String gadgetName);

    static native long usbtkUsbInit(String configfsPath);

    static native void usbtkUsbClose(long handle);

    public static native void magicInit(String dbPath);

    public static native MagicResult magicAnalyzeFile(String path);

    public static native void magicRelease();
}
