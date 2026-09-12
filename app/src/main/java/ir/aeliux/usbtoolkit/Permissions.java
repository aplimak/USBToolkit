package ir.aeliux.usbtoolkit;

import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class Permissions {
    private static final Map<Integer, Result> pending = new HashMap<>();
    private static final AtomicInteger nextCode = new AtomicInteger(1000);

    public interface Result {
        void onResult(boolean allGranted);
    }

    public static void request(Activity a, Result cb, String... perms) {
        // Check if already granted
        boolean ok = true;
        for (String p : perms)
            if (ContextCompat.checkSelfPermission(a, p) != PackageManager.PERMISSION_GRANTED)
                ok = false;
        if (ok) { cb.onResult(true); return; }

        int code = nextCode.incrementAndGet();
        pending.put(code, cb);
        ActivityCompat.requestPermissions(a, perms, code);
    }

    /** Call this from your BaseActivity.onRequestPermissionsResult */
    public static void handle(int requestCode, int[] grantResults) {
        Result cb = pending.remove(requestCode);
        if (cb == null) return;
        boolean all = grantResults.length > 0;
        for (int r : grantResults) if (r != PackageManager.PERMISSION_GRANTED) all = false;
        cb.onResult(all);
    }
}
