package ir.aeliux.usbtoolkit;

import ir.aeliux.usbtoolkit.dto.function.UsbgMassStorageAttrs;

public final class MassStorageFunction
        extends Function<UsbgMassStorageAttrs, MassStorageFunction> {

    MassStorageFunction(long ptr, Gadget parent) { super(ptr, parent); }

    // no FACTORY field, no static block, nothing
}
