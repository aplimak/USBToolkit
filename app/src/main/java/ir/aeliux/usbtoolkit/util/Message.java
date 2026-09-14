package ir.aeliux.usbtoolkit.util;

import android.app.Activity;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

public final class Message {
    private static Snackbar current;

    public static void snack(String text) {
        Activity a = App.activity();
        if (a == null) { toast(text); return; }
        a.runOnUiThread(() -> {
            if (current != null) current.dismiss(); // replace, don't stack
            View root = a.findViewById(android.R.id.content);
            current = Snackbar.make(root, text, Snackbar.LENGTH_SHORT);
            current.show();
        });
    }

    public static void snack(String text, String action, Runnable onAction) {
        Activity a = App.activity();
        if (a == null) return;
        a.runOnUiThread(() -> {
            if (current != null) current.dismiss();
            View root = a.findViewById(android.R.id.content);
            current = Snackbar.make(root, text, Snackbar.LENGTH_LONG);
            if (action != null) current.setAction(action, v -> onAction.run());
            current.show();
        });
    }

    public static void toast(String text) {
        Activity a = App.activity();
        if (a == null) return;
        Toast.makeText(a, text, Toast.LENGTH_SHORT).show();
    }
}
