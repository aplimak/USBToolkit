package ir.aeliux.ptrw;

import dalvik.annotation.optimization.CriticalNative;

class Native {
    static {
        System.loadLibrary("ptrw");
    }

    private Native() {}

    static native void setFreeCallback(long handle, Runnable callback);

    static native void close(long handle);

    @CriticalNative
    static native boolean isOwnerThread(long handle);

    @CriticalNative
    static native boolean isClosed(long handle);
}
