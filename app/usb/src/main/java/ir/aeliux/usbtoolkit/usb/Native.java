package ir.aeliux.usbtoolkit.usb;

import ir.aeliux.usbtoolkit.data.MagicResult;
import ir.aeliux.usbtoolkit.shared.dto.UsbgConfigAttrs;
import ir.aeliux.usbtoolkit.shared.dto.UsbgGadgetAttrs;
import ir.aeliux.usbtoolkit.shared.dto.UsbgGadgetStrs;

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

    static native void usbtkUsbDeleteGadget(long ptrwGadget);

    static native long usbtkUsbCreateFunction(long ptrwGadget,
                                              int functionType,
                                              String functionInstance,
                                              Object functionAttrs);

    static native long usbtkUsbInit(String configfsPath);

    static native void usbtkUsbClose(long handle);

    static native void usbtkPtrwClose(long handle);

    public static native void magicInit(String dbPath);

    public static native MagicResult magicAnalyzeFile(String path);

    public static native void magicRelease();
}
