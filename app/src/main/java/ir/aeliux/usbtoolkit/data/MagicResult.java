package ir.aeliux.usbtoolkit.data;

public final class MagicResult {
    public final String mimeType;
    public final String description;
    public final String encoding;
    public final String error;      // null on success

    public MagicResult(String mimeType, String description,
                       String encoding, String error) {
        this.mimeType    = mimeType;
        this.description = description;
        this.encoding    = encoding;
        this.error       = error;
    }

    public boolean isOk() { return error == null; }
}
