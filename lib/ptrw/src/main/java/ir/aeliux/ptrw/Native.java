package ir.aeliux.ptrw;

class Native {
    static {
        System.loadLibrary("ptrw");
    }

    private Native() {}

    static native void setFreeCallback(long handle, Runnable callback);

    static native void close(long handle);
}
