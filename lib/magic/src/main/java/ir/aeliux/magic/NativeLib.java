package ir.aeliux.magic;

public class NativeLib {

    // Used to load the 'magic' library on application startup.
    static {
        System.loadLibrary("magic");
    }

    /**
     * A native method that is implemented by the 'magic' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
