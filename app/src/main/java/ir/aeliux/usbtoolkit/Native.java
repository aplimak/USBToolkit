package ir.aeliux.usbtoolkit;

import ir.aeliux.usbtoolkit.data.MagicResult;

public class Native {
    private Native() {}

    static {
        System.loadLibrary("usbtoolkit");
    }

    public static native void magicInit(String dbPath);
    public static native MagicResult magicAnalyzeFile(String path);
    public static native void magicRelease();
}
