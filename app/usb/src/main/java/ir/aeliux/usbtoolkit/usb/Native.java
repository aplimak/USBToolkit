package ir.aeliux.usbtoolkit.usb;

import ir.aeliux.usbtoolkit.usb.dto.UsbgConfigAttrs;
import ir.aeliux.usbtoolkit.usb.dto.UsbgGadgetAttrs;
import ir.aeliux.usbtoolkit.usb.dto.UsbgGadgetStrs;

public class Native {
    static {
        System.loadLibrary("usb");
    }

    private Native() {}

    static native long createGadget(long ptrwConfigfs,
                                    String gadgetName,
                                    UsbgGadgetAttrs gadgetAtts,
                                    UsbgGadgetStrs gadgetStrs,
                                    UsbgConfigAttrs configAttrs,
                                    String configStrs);

    static native long openGadget(long ptrwConfigfs,
                                  String gadgetName);

    static native void deleteGadget(long ptrwGadget);

    static native long createFunction(long ptrwGadget,
                                      int functionType,
                                      String functionInstance,
                                      Object functionAttrs);

    static native long init(String configfsPath);

    static native void close(long handle);

    static native void ptrwClose(long handle);
}
