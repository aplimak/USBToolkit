package ir.aeliux.usbtoolkit;

/**
 * Thrown when any operation on the USB gadget fails.
 */
public class UsbGadgetException extends Exception {
    public UsbGadgetException(String message) {
        super(message);
    }

    public UsbGadgetException(String message, Throwable cause) {
        super(message, cause);
    }
}
