package ir.aeliux.usbtoolkit;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.ipc.RootService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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
import ir.aeliux.usbtoolkit.util.Views;
import ir.aeliux.usbtoolkit.widget.MaterialItem;

public class MainActivity extends BaseActivity {
    private final String TAG = "MainActivity";
    private final int DIALOG_INIT = 1;
    private final int DIALOG_MASS_STORAGE = 2;

    private IUsbMassStorageService rootService;
    private boolean isBound = false;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "UsbMassStorageService connected");
            rootService = IUsbMassStorageService.Stub.asInterface(service);
            isBound = true;
            refresh();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "UsbMassStorageService disconnected");
            rootService = null;
            isBound = false;
            refresh();
        }
    };

    private boolean isRunning = false;

    private final String STATE_FILES = "state_files";
    private final String STATE_GADGETS = "state_gadgets";

    private final HashSet<String> filesList = new HashSet<>();
    private List<GadgetState> gadgets = new ArrayList<>();

    private ActivityMainBinding binding;
    private final ActivityResultLauncher<Intent> addMountFilesLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            ArrayList<String> paths = result.getData()
                                    .getStringArrayListExtra(FilePickerActivity.EXTRA_SELECTED_PATHS);
                            if (paths == null || paths.isEmpty()) return;

                            filesList.addAll(paths);
                            refreshFiles();
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

        boolean firstLaunch = true;
        if (savedInstanceState != null) {
            firstLaunch = false;
            restoreInstance(savedInstanceState);
        }

        if (Shell.cmd("ls /data/adb").exec().getCode() > 0) {
            showRootRequiredError();
            return;
        }

        if (firstLaunch) LoadingDialog.show(this, DIALOG_INIT, getString(R.string.initializing));

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

        if (firstLaunch) LoadingDialog.updateMessage(getString(R.string.binder_waiting));

        Intent intent = new Intent(this, UsbMassStorageService.class);
        RootService.bind(intent, serviceConnection);
    }

    private void refresh() {
        if (binding == null) return;

        if (isBound) {
            try {
                rootRefresh();
            } catch (RemoteException e) {
                showRootServiceConnectionLostError();
            }
        } else {
            showRootRequiredError();
        }

        refreshFiles();
        refreshGadgets();

        if (LoadingDialog.getDialogId() == DIALOG_INIT && LoadingDialog.isShowing()) {
            LoadingDialog.dismiss();
        }
    }

    private void rootRefresh() throws RemoteException {
        rootService.isRunning(new IBooleanCallback.Stub() {
            @Override
            public void onResult(boolean result) {
                runOnUiThread(() -> {
                    isRunning = result;
                    Views.setEnabledRecursively(binding.secFiles.getContentContainer(), !result);
                    binding.secFiles.getContentContainer().setAlpha(result ? 0.5f : 1);
                    Views.setEnabledRecursively(binding.secSettings.getContentContainer(), !result);
                    binding.secSettings.getContentContainer().setAlpha(result ? 0.5f : 1);

                    if (isRunning) {
                        binding.doAction.setContentDescription(getString(R.string.stop));
                        binding.doAction.setImageResource(R.drawable.ic_stop);
                    } else {
                        binding.doAction.setContentDescription(getString(R.string.start));
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
                runOnUiThread(() -> showFatalError(getString(R.string.error_no_configfs)));
            }
        });
        rootService.getUdcList(new IStringListCallback.Stub() {
            @Override
            public void onResult(List<String> result) {
                runOnUiThread(() -> {
                    if (result == null || result.isEmpty()) {
                        showFatalError(getString(R.string.error_no_udc));
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
            public void onResult(List<GadgetState> result) {
                runOnUiThread(() -> {
                    if (result == null) {
                        gadgets.clear();
                    } else {
                        gadgets = result;
                    }
                    refreshGadgets();
                });
            }

            @Override
            public void onError(String error) {}
        });
    }

    private void refreshGadgets() {
        if (gadgets == null || gadgets.isEmpty()) {
            binding.secGadgets.setVisibility(View.GONE);
            return;
        }
        binding.secGadgets.setVisibility(View.VISIBLE);
        var container = binding.secGadgets.getContentContainer();
        container.removeAllViews();

        for (GadgetState gadget : gadgets) {
            MaterialItem item = new MaterialItem(MainActivity.this);
            item.setTitle(gadget.name);
            if (gadget.bound) {
                item.setSubtitle(getString(R.string.bound));
            }
            item.setIconResource(R.drawable.ic_gadget);
            item.setOnClickListener((v) -> {
                Intent intent = GadgetDetailsActivity.intent(MainActivity.this, gadget);
                startActivity(intent);
            });
            container.addView(item);
        }
    }

    private void refreshFiles() {
        LinearLayout container = binding.secFiles.getContentContainer();
        container.removeViews(1, container.getChildCount() - 1);

        for (String path : filesList) {
            MaterialItem item = new MaterialItem(this);
            item.setTitle(path);
            item.setClickable(true);
            item.setFocusable(true);
            item.setOnClickListener((v) -> {
                filesList.remove(path);
                refreshFiles();
            });
            container.addView(item);
        }
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

    private void doMount() {
        if (filesList.isEmpty()) {
            Message.snack(getString(R.string.error_no_file));
            return;
        }

        MassStorageConfig.Builder builder = new MassStorageConfig.Builder();

        filesList.forEach(builder::addImage);

        MassStorageConfig config = builder.setReadOnly(binding.schReadonly.isChecked())
                .setCdrom(binding.schCdrom.isChecked())
                .setRemovable(binding.schRemovable.isChecked())
                .setUdc((String) binding.selUdc.getSelectedItem())
                .build();

        LoadingDialog.show(this, DIALOG_MASS_STORAGE, getString(R.string.processing));

        try {
            rootService.start(config, getUsbMassStorageCallback());
        } catch (RemoteException e) {
            showRootServiceConnectionLostError();
        }
    }

    private void doUmount() {
        LoadingDialog.show(this, DIALOG_MASS_STORAGE, getString(R.string.processing));

        try {
            rootService.stop(getUsbMassStorageCallback());
        } catch (RemoteException e) {
            showRootServiceConnectionLostError();
        }
    }

    @NonNull
    private IUsbMassStorageCallback.Stub getUsbMassStorageCallback() {
        return new IUsbMassStorageCallback.Stub() {
            @Override
            public void onStepStart(String stepName) {
                runOnUiThread(() -> LoadingDialog.updateMessage(stepName));
            }

            @Override
            public void onStepComplete(String stepName) {
            }

            @Override
            public void onStepFailed(String stepName, String error) {
                runOnUiThread(() -> {
                    AlertDialog dialog = new MaterialAlertDialogBuilder(MainActivity.this)
                            .setMessage(getString(R.string.error_step, stepName, error))
                            .setNegativeButton(R.string.ok, (d, w) -> d.dismiss())
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
                        Message.snack(getString(R.string.op_success));
                    }
                });
            }
        };
    }

    private void showRootServiceConnectionLostError() {
        showFatalError(getString(R.string.error_binder_lost));
    }

    private void showRootRequiredError() {
        showFatalError(getString(R.string.error_root_required));
    }

    private void showFatalError(String text) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.error);
        builder.setMessage(text);
        builder.setCancelable(false);

        builder.setNegativeButton(R.string.exit, (dialog, which) -> {
            dialog.dismiss();
            finishAffinity();
        });

        builder.show();
    }

    private void restoreInstance(@NonNull Bundle savedInstanceState) {
        Log.d(TAG, "restoreInstance");
        ArrayList<String> saved_files = savedInstanceState.getStringArrayList(STATE_FILES);
        if (saved_files != null) {
            filesList.addAll(saved_files);
            refreshFiles();
        }
        GadgetState[] retrivedGadgets;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            retrivedGadgets = savedInstanceState.getParcelableArray(STATE_GADGETS, GadgetState.class);
        } else {
            retrivedGadgets = (GadgetState[]) savedInstanceState.getParcelableArray(STATE_GADGETS);
        }

        if (retrivedGadgets != null) {
            gadgets.addAll(Arrays.asList(retrivedGadgets));
            refreshGadgets();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        Log.d(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(STATE_FILES, new ArrayList<>(filesList));
        outState.putParcelableArray(STATE_GADGETS, gadgets.toArray(new GadgetState[0]));
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        binding = null;
        if (isBound) {
            RootService.unbind(serviceConnection);
            isBound = false;
        }
    }
}