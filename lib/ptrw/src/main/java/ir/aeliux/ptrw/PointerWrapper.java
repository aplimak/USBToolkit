package ir.aeliux.ptrw;

import android.util.Log;

import androidx.annotation.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public abstract class PointerWrapper implements AutoCloseable {
    static private final String TAG = "PointerWrapper";

    protected Set<PointerWrapper> childrens = new HashSet<>();

    @Nullable
    protected PointerWrapper parent;

    /**
     * The pointer to the ptrw_t.
     */
    private long ptr;

    protected PointerWrapper(long ptr, @Nullable PointerWrapper parent) {
        Log.d(TAG, String.format("<init>(%s)", formatPointer(ptr)));
        if (ptr == 0) {
            throw new IllegalArgumentException("Pointer can't be 0");
        }
        this.ptr = ptr;

        Native.setFreeCallback(ptr, this::close);

        if (parent != null) {
            parent.childrens.add(this);
        }
        this.parent = parent;
    }

    /**
     * Try to get the pointer.
     *
     * @return The pointer.
     * @throws IllegalStateException if the wrapper is closed.
     */
    public long requireHandle() throws IllegalStateException {
        long p = ptr;
        Log.d(TAG, String.format("requireHandle(%s)", formatPointer(p)));
        throwOnClosed(p);
        return p;
    }

    /**
     * Converts pointer to a hex string.
     *
     * @param ptr Pointer.
     * @return Hexadecimal formatted pointer.
     */
    protected String formatPointer(long ptr) {
        return String.format("0x%x", ptr);
    }

    protected void throwOnClosed(long p) {
        if (isClosed(p)) {
            throw new IllegalStateException("PointerWrapper is closed");
        }
    }

    protected boolean isClosed(long p) {
        return p == 0;
    }

    protected void throwOnThreadChange(long p) {
        if (!Native.isOwnerThread(p)) {
            throw new IllegalStateException("PointerWrapper used from a non-owner thread");
        }
    }

    public void throwOnClosed() {
        long p = ptr;
        throwOnClosed(p);
    }

    public boolean isClosed() {
        long p = ptr;
        return isClosed(p);
    }

    @Override
    public final void close() {
        long p = ptr;
        Log.d(TAG, String.format("close(%s)", formatPointer(ptr)));
        throwOnThreadChange(p);

        for (PointerWrapper child : childrens) {
            child.close();
        }
        childrens.clear();

        if (p != 0) {
            ptr = 0;
            closeHandle(p);
            Native.close(p);
        }

        parent = null;
    }

    protected abstract void closeHandle(long ptr);

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PointerWrapper)) return false;
        PointerWrapper that = (PointerWrapper) o;
        return ptr == that.ptr;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(ptr);
    }
}
