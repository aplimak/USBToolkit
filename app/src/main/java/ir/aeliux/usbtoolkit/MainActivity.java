package ir.aeliux.usbtoolkit;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.ipc.RootService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ir.aeliux.usbtoolkit.callback.IGadgetStateListCallback;
import ir.aeliux.usbtoolkit.callback.IStringListCallback;
import ir.aeliux.usbtoolkit.callback.IUsbMassStorageCallback;
import ir.aeliux.usbtoolkit.callback.IBooleanCallback;
import ir.aeliux.usbtoolkit.data.GadgetState;
import ir.aeliux.usbtoolkit.data.MassStorageConfig;
import ir.aeliux.usbtoolkit.databinding.ActivityMainBinding;
import ir.aeliux.usbtoolkit.ipc.IUsbMassStorageService;
import ir.aeliux.usbtoolkit.ipc.UsbMassStorageService;
import ir.aeliux.usbtoolkit.util.LoadingDialog;
import ir.aeliux.usbtoolkit.util.Message;
import ir.aeliux.usbtoolkit.widget.MaterialItem;

public class MainActivity extends BaseActivity {
    private final int DIALOG_INIT = 1;
    private final int DIALOG_MASS_STORAGE = 2;

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

    private boolean isRunning = false;

    private ActivityMainBinding binding;
    private final ActivityResultLauncher<Intent> addMountFilesLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            ArrayList<String> paths = result.getData()
                                    .getStringArrayListExtra(FilePickerActivity.EXTRA_SELECTED_PATHS);
                            if (paths == null) return;

                            var container = binding.secFiles;

                            for (String path : paths) {
                                MaterialItem item = new MaterialItem(this);
                                item.setTitle(path);
                                item.setClickable(true);
                                item.setFocusable(true);
                                item.setOnClickListener(this::handleFileClick);
                                container.addView(item);
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

        setupToolbar(binding.toolbar);
        applyWindowInsets(binding.main);

        if (Shell.cmd("ls /data/adb").exec().getCode() > 0) {
            showRootRequiredError();
            return;
        }

        LoadingDialog.show(this, DIALOG_INIT, "Initializing");

        binding.doAction.setOnClickListener(v -> {
            if (isRunning) {
                doUmount();
            } else {
                doMount();
            }
        });

        binding.btnAddFile.setOnClickListener(v -> {
            Intent intent = new Intent(this, FilePickerActivity.class);
            intent.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getAbsolutePath());
            intent.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, true);

            addMountFilesLauncher.launch(intent);
        });

        LoadingDialog.updateMessage("Waiting for Root Service");

        Intent intent = new Intent(this, UsbMassStorageService.class);
        RootService.bind(intent, serviceConnection);
    }

    private void refresh() {
        if (isBound) {
            try {
                rootRefresh();
            } catch (RemoteException e) {
                showRootServiceConnectionLostError();
            }
        } else {
            showRootRequiredError();
        }

        if (LoadingDialog.getDialogId() == DIALOG_INIT && LoadingDialog.isShowing()) {
            LoadingDialog.dismiss();
        }
    }

    private void rootRefresh() throws RemoteException {
        rootService.isRunning(new IBooleanCallback.Stub() {
            @Override
            public void onResult(boolean result) throws RemoteException {
                runOnUiThread(() -> {
                    isRunning = result;
                    setEnabledRecursively(binding.secFiles.getContentContainer(), !result);
                    binding.secFiles.getContentContainer().setAlpha(result ? 0.5f : 1);
                    setEnabledRecursively(binding.secSettings.getContentContainer(), !result);
                    binding.secSettings.getContentContainer().setAlpha(result ? 0.5f : 1);

                    if (isRunning) {
                        binding.doAction.setImageResource(R.drawable.ic_stop);
                    } else {
                        binding.doAction.setImageResource(R.drawable.ic_play_arrow);
                    }
                    binding.doAction.setVisibility(View.VISIBLE);
                });
            }
        });
        rootService.supportsConfigfs(new IBooleanCallback.Stub() {
            @Override
            public void onResult(boolean result) {
                if (result) return;
                runOnUiThread(() -> {
                    showFatalError("ConfigFS either not supported or not mounted.");
                });
            }
        });
        rootService.getUdcList(new IStringListCallback.Stub() {
            @Override
            public void onResult(List<String> result) {
                runOnUiThread(() -> {
                    if (result == null || result.isEmpty()) {
                        showFatalError("No UDC is found.");
                        return;
                    }
                    CharSequence[] currentDropdownEntries = null;
                    try {
                        currentDropdownEntries = binding.selUdc.getDropdownEntries();
                    } catch (IllegalStateException ignored) {}
                    if (contentsEqual(result, currentDropdownEntries != null ? Arrays.asList(currentDropdownEntries) : new ArrayList<>())) return;
                    binding.selUdc.setDropdownEntries(result.toArray(new CharSequence[0]));
                });
            }
        });
        rootService.getGadgetStateList(new IGadgetStateListCallback.Stub() {
            @Override
            public void onResult(List<GadgetState> result) throws RemoteException {
                runOnUiThread(() -> {
                    if (result == null || result.isEmpty()) {
                        binding.secGadgets.setVisibility(View.GONE);
                        return;
                    }
                    binding.secGadgets.setVisibility(View.VISIBLE);
                    var container = binding.secGadgets.getContentContainer();
                    container.removeAllViews();

                    for (GadgetState gadget : result) {
                        MaterialItem item = new MaterialItem(MainActivity.this);
                        item.setTitle(gadget.name);
                        item.setIconResource(R.drawable.ic_gadget);
                        item.setOnClickListener((v) -> {
                            Intent intent = GadgetDetailsActivity.intent(MainActivity.this, gadget);
                            startActivity(intent);
                        });
                        container.addView(item);
                    }
                });
            }

            @Override
            public void onError(String error) throws RemoteException {

            }
        });
    }

    public static boolean contentsEqual(List<? extends CharSequence> a,
                                        List<? extends CharSequence> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            CharSequence x = a.get(i);
            CharSequence y = b.get(i);
            if (x == null || y == null) {
                if (x != y) return false;
            } else if (!x.toString().equals(y.toString())) {
                return false;
            }
        }
        return true;
    }

    private void doUmount() {
        LoadingDialog.show(this, DIALOG_MASS_STORAGE, "Processing");

        try {
            rootService.stop(getUsbMassStorageCallback());
        } catch (RemoteException e) {
            showRootServiceConnectionLostError();
        }
    }

    private void doMount() {
        MassStorageConfig.Builder builder = new MassStorageConfig.Builder();
        boolean hasFiles = false;
        for (int i = 1; i < binding.secFiles.getContentContainer().getChildCount(); i++) {
            MaterialItem item = (MaterialItem) binding.secFiles.getContentContainer().getChildAt(i);
            builder.addImage(item.getText().toString());
            hasFiles = true;
        }

        if (!hasFiles) {
            Message.snack("At least one file required");
            return;
        }

        MassStorageConfig config = builder.setReadOnly(binding.schReadonly.isChecked())
                                    .setCdrom(binding.schCdrom.isChecked())
                                    .setRemovable(binding.schRemovable.isChecked())
                                    .setUdc((String) binding.selUdc.getSelectedItem())
                                    .build();

        LoadingDialog.show(this, DIALOG_MASS_STORAGE, "Processing");

        try {
            rootService.start(config, getUsbMassStorageCallback());
        } catch (RemoteException e) {
            showRootServiceConnectionLostError();
        }
    }

    @NonNull
    private IUsbMassStorageCallback.Stub getUsbMassStorageCallback() {
        return new IUsbMassStorageCallback.Stub() {
            @Override
            public void onStepStart(String stepName) {
                runOnUiThread(() -> {
                    LoadingDialog.updateMessage(stepName);
                });
            }

            @Override
            public void onStepComplete(String stepName) {
            }

            @Override
            public void onStepFailed(String stepName, String error) {
                runOnUiThread(() -> {
                    AlertDialog dialog = new MaterialAlertDialogBuilder(MainActivity.this)
                            .setMessage("Error occurred on step: " + stepName + " - " + error)
                            .setNegativeButton("OK", (d, w) -> {
                                d.dismiss();
                            })
                            .setCancelable(false)
                            .create();

                    dialog.show();
                });
            }

            @Override
            public void onEnd(boolean result) {
                runOnUiThread(() -> {
                    refresh();
                    LoadingDialog.dismiss();
                    if (result) {
                        Message.snack("Operation completed successfully");
                    }
                });
            }
        };
    }

    private void handleFileClick(View view) {
        ViewGroup parent = (ViewGroup) view.getParent();
        parent.removeView(view);
        refresh();
    }

    public static void setEnabledRecursively(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                setEnabledRecursively(group.getChildAt(i), enabled);
            }
        }
    }

    private void showRootServiceConnectionLostError() {
        showFatalError("Connection to the root service is lost, you must restart the app.");
    }

    private void showRootRequiredError() {
        showFatalError("This app needs root access to work.");
    }

    private void showFatalError(String text) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Error");
        builder.setMessage(text);
        builder.setCancelable(false);

        builder.setNegativeButton("Exit", (dialog, which) -> {
            dialog.dismiss();
            finishAffinity();
        });

        builder.show();
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