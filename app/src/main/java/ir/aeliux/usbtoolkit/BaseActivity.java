package ir.aeliux.usbtoolkit;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;

import ir.aeliux.usbtoolkit.util.Permissions;

public class BaseActivity extends AppCompatActivity {
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Permissions.handle(requestCode, grantResults);
    }

    protected void setupToolbar(MaterialToolbar toolbar) {
        boolean isRoot = isTaskRoot()
                || (getIntent().getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0;

        if (isRoot) {
            toolbar.setNavigationIcon(null);
        } else {
            toolbar.setNavigationOnClickListener(v -> {
                setResult(Activity.RESULT_CANCELED);
                finish();
            });
        }
    }

    protected void applyWindowInsets(View view) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);

            return insets;
        });
    }
}
