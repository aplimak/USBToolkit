package ir.aeliux.magic;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class MagicDatabase {
    private static final String DB_NAME = "magic.mgc";
    private static final int DB_VERSION = BuildConfig.DB_VERSION;
    private static final String TAG = "MagicDatabase";

    public static void extract(Context ctx) throws IOException {
        SharedPreferences prefs = ctx.getSharedPreferences(BuildConfig.LIBRARY_PACKAGE_NAME,
                                                           Context.MODE_PRIVATE);
        int extractedDbVersion = prefs.getInt("db_version", 0);
        File mgc = new File(getPath(ctx));
        if (extractedDbVersion == DB_VERSION && mgc.exists() && mgc.length() > 0) {
            return;
        }

        Log.i(TAG, "Extracting Magic Database version " + DB_VERSION);

        try (InputStream in = ctx.getAssets().open(DB_NAME);
             OutputStream out = new FileOutputStream(mgc)) {
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }

        prefs.edit().putInt("db_version", DB_VERSION).commit();
    }

    public static String getPath(Context ctx) {
        return new File(ctx.getCacheDir(), DB_NAME).getAbsolutePath();
    }
}
