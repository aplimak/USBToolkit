package ir.aeliux.usbtoolkit.usb;

import java.io.IOException;

import ir.aeliux.usbtoolkit.usb.dto.UsbgConfigAttrs;
import ir.aeliux.usbtoolkit.usb.dto.UsbgGadgetAttrs;
import ir.aeliux.usbtoolkit.usb.dto.UsbgGadgetStrs;

public class Configfs extends PointerWrapper {
    private Configfs(long ptr) {
        super(ptr, null);
    }

    public static Configfs open(String configfsPath) throws IOException {
        long p = Native.init(configfsPath);
        return new Configfs(p);
    }

    @Override
    protected void closeHandle(long ptr) {
        Native.close(ptr);
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
