package ir.aeliux.usbtoolkit;

import ir.aeliux.usbtoolkit.dto.UsbgConfigAttrs;
import ir.aeliux.usbtoolkit.dto.UsbgGadgetAttrs;
import ir.aeliux.usbtoolkit.dto.UsbgGadgetStrs;

public class Configfs extends PointerWrapper {
    private Configfs(long ptr) {
        super(ptr, null);
    }

    public static Configfs open(String configfsPath) {
        long p = Native.usbtkUsbInit(configfsPath);
        return new Configfs(p);
    }

    @Override
    protected void closeHandle(long ptr) {
        Native.usbtkUsbClose(ptr);
    }

    public Gadget createGadget(String gadgetName,
                               UsbgGadgetAttrs gadgetAtts,
                               UsbgGadgetStrs gadgetStrs,
                               UsbgConfigAttrs configAttrs,
                               String configStrs)
    {
        return Gadget.create(this,
                             gadgetName,
                             gadgetAtts,
                             gadgetStrs,
                             configAttrs,
                             configStrs);
    }

    public Gadget OpenGadget(String gadgetName)
    {
        return Gadget.open(this,
                           gadgetName);
    }
}
