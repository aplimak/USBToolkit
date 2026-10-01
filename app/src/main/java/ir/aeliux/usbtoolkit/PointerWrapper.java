package ir.aeliux.usbtoolkit;

import androidx.annotation.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public abstract class PointerWrapper implements AutoCloseable {
    protected Set<PointerWrapper> childrens = new HashSet<>();

    @Nullable
    protected PointerWrapper parent;

    /**
     * 0 == closed. Never exposed outside this class.
     */
    private long ptr;

    protected PointerWrapper(long ptr, @Nullable PointerWrapper parent) {
        if (ptr == 0) {
            throw new IllegalArgumentException("Pointer can't be 0");
        }
        this.ptr = ptr;

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
    protected long requireHandle() throws IllegalStateException {
        long p = ptr;
        throwOnClosed(p);
        return p;
    }

    protected void throwOnClosed(long p) {
        if (isClosed(p)) {
            throw new IllegalStateException("Configfs is closed");
        }
    }

    protected boolean isClosed(long p) {
        return p == 0;
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
        for (PointerWrapper child : childrens) {
            child.close();
        }
        childrens.clear();

        long p = ptr;
        if (p != 0) {
            ptr = 0;
            closeHandle(p);
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
