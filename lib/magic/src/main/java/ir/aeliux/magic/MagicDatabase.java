package ir.aeliux.magic;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class MagicDatabase {
    public static void extract(Context ctx) throws IOException {
        File mgc = new File(getPath(ctx));
        if (mgc.exists() && mgc.length() > 0) return;

        try (InputStream in = ctx.getAssets().open("magic.mgc");
             OutputStream out = new FileOutputStream(mgc)) {
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
    }

    public static String getPath(Context ctx) {
        return new File(ctx.getFilesDir(), "magic.mgc").getAbsolutePath();
    }
}
