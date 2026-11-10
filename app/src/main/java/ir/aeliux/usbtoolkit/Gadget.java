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

    public static Gadget open(Configfs configfs,
                              String gadgetName) throws IllegalArgumentException
    {
        long ptr = Native.usbtkUsbOpenGadget(configfs.requireHandle(),
                                             gadgetName);

        return new Gadget(ptr, configfs);
    }

    public void delete() {
        Native.usbtkUsbDeleteGadget(requireHandle());
        close();
    }

    @Override
    protected void closeHandle(long ptr) {
        // Nothing to do. gadgets are part of usbg_state memory so it is closed by Configfs close call.
    }

    @NonNull
    public Configfs getParent() {
        throwOnClosed();
        assert parent != null;
        return (Configfs) parent;
    }
}
