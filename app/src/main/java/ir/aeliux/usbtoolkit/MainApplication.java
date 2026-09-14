package ir.aeliux.usbtoolkit;

import android.app.Application;

import com.topjohnwu.superuser.Shell;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import ir.aeliux.usbtoolkit.util.App;
import ir.aeliux.usbtoolkit.util.LoadingDialog;

public class MainApplication extends Application {
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
    }

    public void installCrashHandler() {
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
}
