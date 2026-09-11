package ir.aeliux.usbtoolkit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.color.DynamicColors;

import java.util.ArrayList;

import ir.aeliux.usbtoolkit.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding bindings;
    private final ActivityResultLauncher<Intent> pickFilesLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            ArrayList<String> paths = result.getData()
                                    .getStringArrayListExtra(FilePickerActivity.EXTRA_SELECTED_PATHS);
                            if (paths == null) return;

                            for (String path : paths) {
                                Log.d("Picker", "Selected: " + path);
                            }
                        }
                    });

    private void launchPicker() {
        Intent intent = new Intent(this, FilePickerActivity.class);
        intent.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());
        intent.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, true);

        /* ArrayList<String> exts = new ArrayList<>();
        exts.add("conf");
        exts.add("txt");
        exts.add("log");
        intent.putStringArrayListExtra(FilePickerActivity.EXTRA_ALLOWED_EXTENSIONS, exts); */

        pickFilesLauncher.launch(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        bindings = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(bindings.getRoot());
        int[] paddings = {
                bindings.main.getPaddingLeft(),
                bindings.main.getPaddingTop(),
                bindings.main.getPaddingRight(),
                bindings.main.getPaddingBottom()
        };
        ViewCompat.setOnApplyWindowInsetsListener(bindings.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    paddings[0] + systemBars.left,
                    paddings[1] + systemBars.top,
                    paddings[2] + systemBars.right,
                    paddings[3] + systemBars.bottom
            );
            return insets;
        });

        bindings.btnAddFile.setOnClickListener(v -> {
            launchPicker();
        });
    }
}