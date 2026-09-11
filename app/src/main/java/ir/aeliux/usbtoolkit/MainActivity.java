package ir.aeliux.usbtoolkit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;

import ir.aeliux.usbtoolkit.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private final ActivityResultLauncher<Intent> addMountFilesLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            ArrayList<String> paths = result.getData()
                                    .getStringArrayListExtra(FilePickerActivity.EXTRA_SELECTED_PATHS);
                            if (paths == null) return;

                            var container = binding.layoutSelectedFiles;

                            for (String path : paths) {
                                TextView tv = new TextView(this);
                                tv.setText(path);
                                tv.setPadding(16, 16, 16, 16);
                                container.addView(tv);
                            }

                            binding.containerSelectedFiles.setVisibility(View.VISIBLE);
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        int[] paddings = {
                binding.main.getPaddingLeft(),
                binding.main.getPaddingTop(),
                binding.main.getPaddingRight(),
                binding.main.getPaddingBottom()
        };
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    paddings[0] + systemBars.left,
                    paddings[1] + systemBars.top,
                    paddings[2] + systemBars.right,
                    paddings[3] + systemBars.bottom
            );
            return insets;
        });

        binding.btnAddFile.setOnClickListener(v -> {
            Intent intent = new Intent(this, FilePickerActivity.class);
            intent.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getAbsolutePath());
            intent.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, true);

            addMountFilesLauncher.launch(intent);
        });
    }
}