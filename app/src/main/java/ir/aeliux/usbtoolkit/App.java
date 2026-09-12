package ir.aeliux.usbtoolkit;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

public final class App {
    private static WeakReference<Activity> current = new WeakReference<>(null);

    public static void init(Application app) {
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityResumed(Activity a) {
                current = new WeakReference<>(a);
            }
            @Override public void onActivityPaused(Activity a) {
                if (current.get() == a) current = new WeakReference<>(null);
            }
            @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
            @Override public void onActivityStarted(@NonNull Activity activity) {}
            @Override public void onActivityStopped(@NonNull Activity activity) {}
            @Override public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}
            @Override public void onActivityDestroyed(@NonNull Activity activity) {}
        });
    }

    public static Activity activity() { return current.get(); }
}
