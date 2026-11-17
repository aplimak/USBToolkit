package ir.aeliux.ptrw;

import androidx.annotation.Nullable;

class DummyPointerWrapper extends PointerWrapper {
    static {
        System.loadLibrary("ptrw");
    }

    public DummyPointerWrapper(long ptr, @Nullable PointerWrapper parent) {
        super(ptr, parent);
    }

    public static DummyPointerWrapper create() {
        return new DummyPointerWrapper(init(), null);
    }

    @Override
    protected void closeHandle(long ptr) {
        free(ptr);
    }

    private static native long init();
    private static native void free(long wptr);
}
