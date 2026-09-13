package ir.aeliux.usbtoolkit;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

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
}
