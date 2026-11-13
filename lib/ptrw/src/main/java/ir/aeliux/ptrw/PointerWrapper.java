package ir.aeliux.ptrw;

import android.util.Log;

import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import ir.aeliux.ptrw.BuildConfig;

public abstract class PointerWrapper implements AutoCloseable {
    static private final String TAG = "PointerWrapper";
    private static final boolean DEBUG = BuildConfig.DEBUG;

    private final Set<PointerWrapper> children =
            Collections.newSetFromMap(new IdentityHashMap<>());

    @Nullable
    private PointerWrapper parent;

    /**
     * The pointer to the ptrw_t.
     */
    private long ptr;

    protected PointerWrapper(long ptr, @Nullable PointerWrapper parent) {
        if (DEBUG) Log.d(TAG, String.format("<init>(%s)", formatPointer(ptr)));
        if (ptr == 0) {
            throw new IllegalArgumentException("Pointer can't be 0");
        }
        this.ptr = ptr;

        try {
            Native.setFreeCallback(ptr, this::close);
        } catch (RuntimeException | Error e) {
            // It's safe as close checks everything before doing any calls.
            try {
                close();
            } catch (RuntimeException | Error closeEx) {
                e.addSuppressed(closeEx);   // don't lose the original
            }
            throw e;
        }

        if (parent != null) {
            parent.children.add(this);
        }
        this.parent = parent;
    }

    protected final @Nullable PointerWrapper getParent() {
        return parent;
    }

    /**
     * Try to get the pointer.
     *
     * @return The pointer.
     * @throws IllegalStateException if the wrapper is closed or accessed from non-owner thread.
     */
    public final long requireHandle() throws IllegalStateException {
        long p = ptr;
        if (DEBUG) Log.d(TAG, String.format("requireHandle(%s)", formatPointer(p)));
        throwOnClosed(p);
        throwOnThreadChange(p);
        return p;
    }

    /**
     * Converts pointer to a hex string.
     *
     * @param ptr Pointer.
     * @return Hexadecimal formatted pointer.
     */
    protected final String formatPointer(long ptr) {
        return "0x" + Long.toHexString(ptr);
    }

    protected final void throwOnClosed(long p) {
        if (isClosed(p)) {
            throw new IllegalStateException("PointerWrapper is closed");
        }
    }

    protected final void throwOnThreadChange(long p) {
        if (!Native.isOwnerThread(p)) {
            throw new IllegalStateException("PointerWrapper used from a non-owner thread");
        }
    }

    protected final boolean isClosed(long p) {
        return Native.isClosed(p);
    }

    protected final void throwOnClosed() {
        long p = ptr;
        throwOnClosed(p);
    }

    public final boolean isClosed() {
        long p = ptr;
        return isClosed(p);
    }

    @Override
    public final void close() {
        long p = ptr;
        if (DEBUG) Log.d(TAG, String.format("close(%s)", formatPointer(ptr)));

        if (p == 0) return;
        throwOnThreadChange(p);

        if (!children.isEmpty()) {
            PointerWrapper[] kids = children.toArray(new PointerWrapper[0]);
            children.clear();
            for (PointerWrapper child : kids) {
                try {child.close();} catch (Exception e) {
                    Log.w(TAG, "Error while closing child PointerWrapper", e);
                }
            }
        }

        // it must be here as childrens may need our pointer.
        ptr = 0;

        // We can't afford any failures.
        try {
            if (!isClosed(p)) {
                // Inner pointer still not freed.
                closeHandle(p);
            }
        } finally {
            try {
                Native.close(p);
            } finally {
                detachFromParent();
            }
        }
    }

    private void detachFromParent() {
        PointerWrapper par = parent;
        if (par != null) {
            par.children.remove(this);
            parent = null;
        }
    }

    protected abstract void closeHandle(long ptr);
}
