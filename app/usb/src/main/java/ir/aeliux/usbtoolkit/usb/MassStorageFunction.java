package ir.aeliux.usbtoolkit.usb;

import ir.aeliux.usbtoolkit.usb.dto.function.UsbgMassStorageAttrs;

public final class MassStorageFunction
        extends Function<UsbgMassStorageAttrs, MassStorageFunction>
{

    MassStorageFunction(long ptr, Gadget parent) {super(ptr, parent);}

    // no FACTORY field, no static block, nothing
}
