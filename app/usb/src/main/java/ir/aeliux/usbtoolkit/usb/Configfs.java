package ir.aeliux.usbtoolkit.usb;

import ir.aeliux.usbtoolkit.shared.dto.UsbgConfigAttrs;
import ir.aeliux.usbtoolkit.shared.dto.UsbgGadgetAttrs;
import ir.aeliux.usbtoolkit.shared.dto.UsbgGadgetStrs;

public class Configfs extends PointerWrapper {
    private Configfs(long ptr) {
        super(ptr, null);
    }

    public static Configfs open(String configfsPath) {
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
