package ir.aeliux.usbtoolkit.usb;

import androidx.annotation.NonNull;

import ir.aeliux.usbtoolkit.shared.UsbFunctions;
import ir.aeliux.usbtoolkit.shared.dto.UsbgConfigAttrs;
import ir.aeliux.usbtoolkit.shared.dto.UsbgFunctionAttrsBase;
import ir.aeliux.usbtoolkit.shared.dto.UsbgGadgetAttrs;
import ir.aeliux.usbtoolkit.shared.dto.UsbgGadgetStrs;
import kotlin.NotImplementedError;

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

    public <A extends UsbgFunctionAttrsBase, F extends Function<A, F>>
    F createFunction(String instance, A attrs) {
        long ptr = Native.usbtkUsbCreateFunction(requireHandle(),
                                                 attrs.getType().ordinal(),
                                                 instance,
                                                 attrs);
        @SuppressWarnings("unchecked")
        F result = (F) wrapFunction(attrs.getType(), ptr);
        return result;
    }

    Function<?, ?> wrapFunction(UsbFunctions type, long ptr) {
        return switch (type) {
            case USBG_F_SERIAL, USBG_F_RNDIS, USBG_F_PHONET, USBG_F_FFS, USBG_F_EEM, USBG_F_NCM,
                 USBG_F_ACM, USBG_F_OBEX, USBG_F_ECM, USBG_F_SUBSET, USBG_F_MIDI, USBG_F_LOOPBACK,
                 USBG_F_HID, USBG_F_UAC2, USBG_F_UVC, USBG_F_PRINTER, USBG_F_9PFS ->
                    throw new NotImplementedError("Function type: " + type + " is not supported");
            case USBG_F_MASS_STORAGE -> new MassStorageFunction(ptr, this);
        };
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
