package ir.aeliux.usbtoolkit;

import androidx.annotation.NonNull;

import ir.aeliux.usbtoolkit.dto.UsbgConfigAttrs;
import ir.aeliux.usbtoolkit.dto.UsbgGadgetAttrs;
import ir.aeliux.usbtoolkit.dto.UsbgGadgetStrs;

public class Gadget extends PointerWrapper {
    private Gadget(long ptr, @NonNull Configfs parent) {
        super(ptr, parent);
    }

    public static Gadget create(Configfs configfs,
                                String gadgetName,
                                UsbgGadgetAttrs gadgetAtts,
                                UsbgGadgetStrs gadgetStrs,
                                UsbgConfigAttrs configAttrs,
                                String configStrs)
    {
        long ptr = Native.usbtkUsbCreateGadget(configfs.requireHandle(),
                                               gadgetName,
                                               gadgetAtts,
                                               gadgetStrs,
                                               configAttrs,
                                               configStrs);

        return new Gadget(ptr, configfs);
    }

    @Override
    protected void closeHandle(long ptr) {
        // Nothing to do.
    }

    @NonNull
    public Configfs getParent() {
        throwOnClosed();
        assert parent != null;
        return (Configfs) parent;
    }
}
