package ir.aeliux.usbtoolkit;

import android.app.Activity;
import android.app.Application;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.lang.ref.WeakReference;

public final class LoadingDialog {

    private static AlertDialog currentDialog;
    private static TextView currentText;
    private static int dialogId;
    private static WeakReference<Activity> hostActivityRef;
    private static boolean initialized = false;

    private LoadingDialog() {} // no instances

    /** Call this once from your Application.onCreate() to enable auto-cleanup. */
    public static void init(Application app) {
        if (initialized) return;
        initialized = true;
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityDestroyed(Activity activity) {
                // If the activity hosting the dialog dies, kill the dialog to prevent a leak
                if (hostActivityRef != null && hostActivityRef.get() == activity) {
                    dismissImmediately();
                }
            }
            @Override public void onActivityCreated(Activity a, Bundle b) {}
            @Override public void onActivityStarted(Activity a) {}
            @Override public void onActivityResumed(Activity a) {}
            @Override public void onActivityPaused(Activity a) {}
            @Override public void onActivityStopped(Activity a) {}
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
        });
    }

    /** Show (or replace) the loading dialog. Safe to call from any thread. */
    public static void show(Activity activity, int id, String message) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        activity.runOnUiThread(() -> showInternal(activity, id, message));
    }

    /** Update the message on the currently-showing dialog. Safe to call from any thread. */
    public static void updateMessage(String message) {
        Activity host = hostActivityRef != null ? hostActivityRef.get() : null;
        if (host == null) return;
        host.runOnUiThread(() -> {
            if (currentText != null) currentText.setText(message);
        });
    }

    /** Dismiss the dialog. Safe to call from any thread, and safe if nothing is showing. */
    public static void dismiss() {
        Activity host = hostActivityRef != null ? hostActivityRef.get() : null;
        if (host != null) host.runOnUiThread(LoadingDialog::dismissImmediately);
        else dismissImmediately();
    }

    public static boolean isShowing() {
        return currentDialog != null && currentDialog.isShowing();
    }

    public static int getDialogId() {
        return dialogId;
    }

    private static void showInternal(Activity activity, int id, String message) {
        // If a dialog is already up, just update its text instead of flickering
        if (currentDialog != null && currentDialog.isShowing()
                && hostActivityRef != null && hostActivityRef.get() == activity && dialogId == id) {
            if (message != null && currentText != null) currentText.setText(message);
            return;
        }

        // Otherwise, kill any stale dialog from another activity
        dismissImmediately();

        dialogId = id;
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_loading, null);
        currentText = view.findViewById(R.id.progress_text);
        if (message != null) currentText.setText(message);

        currentDialog = new MaterialAlertDialogBuilder(activity)
                .setView(view)
                .setCancelable(false)
                .create();

        currentDialog.show();
        hostActivityRef = new WeakReference<>(activity);
    }

    private static void dismissImmediately() {
        if (currentDialog != null) {
            if (currentDialog.isShowing()) {
                try { currentDialog.dismiss(); } catch (Exception ignored) {}
            }
            currentDialog = null;
        }
        dialogId = -1;
        currentText = null;
        hostActivityRef = null;
    }
}
