package ir.aeliux.usbtoolkit.usb;

import ir.aeliux.usbtoolkit.usb.dto.UsbgFunctionAttrsBase;

public abstract class Function<A extends UsbgFunctionAttrsBase, F extends Function<A, F>>
        extends PointerWrapper
{
    protected Function(long ptr, Gadget parent) {
        super(ptr, parent);
    }

    @Override
    protected void closeHandle(long ptr) {
        // Managed by usbg_state
    }
}
