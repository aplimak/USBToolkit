package ir.aeliux.usbtoolkit;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.ipc.RootService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import ir.aeliux.usbtoolkit.ipc.KeepAliveRootService;
import ir.aeliux.usbtoolkit.util.App;
import ir.aeliux.usbtoolkit.util.LoadingDialog;

public class MainApplication extends Application {
    private final String TAG = "MainApplication";
    private final ServiceConnection serviceConnection = new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "Keep-Alive Root Service started");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "Keep-Alive Root Service stopped");
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        installCrashHandler();
        Shell.enableVerboseLogging = BuildConfig.DEBUG;
        Shell.setDefaultBuilder(Shell.Builder.create()
                .setFlags(Shell.FLAG_MOUNT_MASTER)
                .setTimeout(10));
        App.init(this);
        LoadingDialog.init(this);

        Log.i(TAG, "Binding Keep-Alive Root Service");
        Intent intent = new Intent(this, KeepAliveRootService.class);
        RootService.bind(intent, serviceConnection);

        extractMagic();
    }

    private void installCrashHandler() {
        Thread.UncaughtExceptionHandler def = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            try {
                // write stack trace to a file in getFilesDir(), or send to your logger
                File log = new File(getFilesDir(), "crash_" + System.currentTimeMillis() + ".txt");
                try (PrintWriter pw = new PrintWriter(new FileWriter(log))) {
                    pw.println("Thread: " + thread.getName());
                    ex.printStackTrace(pw);
                }
            } catch (Throwable ignored) {}
            if (def != null) {
                def.uncaughtException(thread, ex); // let Android do its normal thing
            }
        });
    }

    private void extractMagic() {
        File mgc = new File(getFilesDir(), "magic.mgc");

        if (!mgc.exists() || mgc.length() == 0) {
            try (InputStream in  = getAssets().open("magic.mgc");
                 OutputStream out = new FileOutputStream(mgc)) {
                byte[] buf = new byte[16384];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
