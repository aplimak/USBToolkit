package ir.aeliux.usbtoolkit;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Run concurrent jobs.
 */
public final class Jobs {
    private static final ExecutorService pool = Executors.newFixedThreadPool(4);
    private static final Handler main = new Handler(Looper.getMainLooper());

    public static void run(Runnable background, Runnable onDone) {
        pool.execute(() -> {
            try { background.run(); }
            finally { if (onDone != null) main.post(onDone); }
        });
    }
}
