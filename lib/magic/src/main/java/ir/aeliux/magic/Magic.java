package ir.aeliux.magic;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import ir.aeliux.ptrw.PointerWrapper;

public final class Magic extends PointerWrapper {
    private Magic(long ptr) {
        super(ptr, null);
    }

    public static Magic open(String dbPath) {
        return new Magic(Native.init(dbPath));
    }

    @Override
    protected void closeHandle(long ptr) {
        Native.release(ptr);
    }

    public MagicResult analyze(@NonNull String path) throws IllegalArgumentException {
        return Native.analyzeFile(requireHandle(), path);
    }
}
