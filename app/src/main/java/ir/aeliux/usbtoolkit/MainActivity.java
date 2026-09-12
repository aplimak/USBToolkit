package ir.aeliux.usbtoolkit;

import android.app.Activity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.topjohnwu.superuser.ipc.RootService;

import java.util.ArrayList;

import ir.aeliux.usbtoolkit.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private IUsbMassStorageService rootService;
    private boolean isBound = false;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            rootService = IUsbMassStorageService.Stub.asInterface(service);
            isBound = true;
            refresh();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            rootService = null;
            isBound = false;
            refresh();
        }
    };

    private final Handler handler = new Handler(Looper.getMainLooper());

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

                            refresh();
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

        binding.btnClearFiles.setOnClickListener(v -> {
            binding.layoutSelectedFiles.removeAllViews();
            refresh();
        });

        Intent intent = new Intent(this, UsbMassStorageService.class);
        RootService.bind(intent, serviceConnection);
    }

    private void refresh() {
        binding.containerSelectedFiles.setVisibility(binding.layoutSelectedFiles.getChildCount() > 0 ? View.VISIBLE : View.GONE);

        if (isBound) {
            try {
                rootService.isRunning(new IBooleanCallback.Stub() {
                    @Override
                    public void onResult(boolean result) throws RemoteException {
                        runOnUiThread(() -> {
                            binding.btnStart.setEnabled(!result);
                            binding.btnStop.setEnabled(result);
                        });
                    }
                });
            } catch (RemoteException e) {
                new AlertDialog.Builder(this).setMessage(e.toString()).show();
            }
        } else {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
            builder.setTitle("Error");
            builder.setMessage("This app needs root access to work.");
            builder.setCancelable(false);

            builder.setNegativeButton("Exit", (dialog, which) -> {
                dialog.dismiss();
                finishAffinity();
            });

            builder.show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            RootService.unbind(serviceConnection);
            isBound = false;
        }
    }
}