package ir.aeliux.usbtoolkit.usb;

public class NativeLib {

    // Used to load the 'usb' library on application startup.
    static {
        System.loadLibrary("usb");
    }

    /**
     * A native method that is implemented by the 'usb' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
