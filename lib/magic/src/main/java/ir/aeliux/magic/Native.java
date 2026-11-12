package ir.aeliux.magic;

class Native {
    static {
        System.loadLibrary("magic");
    }

    private Native() {}

    static native long init(String dbPath);

    static native MagicResult analyzeFile(long magic, String path);

    static native void release(long magic);
}
