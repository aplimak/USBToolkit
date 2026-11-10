package ir.aeliux.magic;

public class Native {
    static {
        System.loadLibrary("magic");
    }

    private Native() {}

    public static native void magicInit(String dbPath);

    public static native MagicResult magicAnalyzeFile(String path);

    public static native void magicRelease();
}
