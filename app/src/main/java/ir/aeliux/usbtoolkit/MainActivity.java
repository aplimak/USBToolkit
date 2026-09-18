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
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.ipc.RootService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import ir.aeliux.usbtoolkit.callback.IGadgetStateListCallback;
import ir.aeliux.usbtoolkit.callback.IStringListCallback;
import ir.aeliux.usbtoolkit.callback.IUsbMassStorageCallback;
import ir.aeliux.usbtoolkit.callback.IBooleanCallback;
import ir.aeliux.usbtoolkit.data.GadgetState;
import ir.aeliux.usbtoolkit.data.MassStorageConfig;
import ir.aeliux.usbtoolkit.databinding.ActivityMainBinding;
import ir.aeliux.usbtoolkit.ipc.IUsbMassStorageService;
import ir.aeliux.usbtoolkit.ipc.UsbMassStorageService;
import ir.aeliux.usbtoolkit.util.Jobs;
import ir.aeliux.usbtoolkit.util.LoadingDialog;
import ir.aeliux.usbtoolkit.util.Message;
import ir.aeliux.usbtoolkit.util.Views;
import ir.aeliux.usbtoolkit.viewmodel.MassStorageViewModel;
import ir.aeliux.usbtoolkit.widget.MaterialItem;

public class MainActivity extends BaseActivity {
    private final String TAG = "MainActivity";
    private final int DIALOG_INIT = 1;
    private final int DIALOG_MASS_STORAGE = 2;

    private IUsbMassStorageService rootService;
    private boolean isBound = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        private final LifecycleEventObserver refreshLifecycleCallback = (LifecycleEventObserver) (lifecycleOwner, event) -> {
            if (this.refreshCalled) return;
            if (event == Lifecycle.Event.ON_START) {
                refresh();
                this.refreshCalled = true;
            }
        };
        private boolean refreshCalled = false;
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "UsbMassStorageService connected");
            rootService = IUsbMassStorageService.Stub.asInterface(service);
            isBound = true;
            var lc = getLifecycle();
            if (lc.getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                refresh();
                return;
            }
            if (!refreshCalled) {
                lc.addObserver(refreshLifecycleCallback);
            }
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

    private ActivityMainBinding binding;
    private MassStorageViewModel model;
    private final ActivityResultLauncher<Intent> addMountFilesLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            ArrayList<String> paths = result.getData()
                                    .getStringArrayListExtra(FilePickerActivity.EXTRA_SELECTED_PATHS);
                            if (paths == null || paths.isEmpty()) return;

                            model.setFilePaths(paths.stream()
                                                    .distinct()
                                                    .collect(Collectors.toList()));
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

        model = new ViewModelProvider(this).get(MassStorageViewModel.class);
        boolean firstLaunch = true;
        if (savedInstanceState != null) {
            firstLaunch = false;
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

        binding.schReadonly.setOnCheckedChangeListener((v, checked) -> {
            model.setReadonly(checked);
        });
        model.getReadonly().observe(this, (value) -> {
            binding.schReadonly.setChecked(value);
        });

        binding.schCdrom.setOnCheckedChangeListener((v, checked) -> {
            model.setCdrom(checked);
        });
        model.getCdrom().observe(this, (value) -> {
            binding.schCdrom.setChecked(value);
        });

        binding.schRemovable.setOnCheckedChangeListener((v, checked) -> {
            model.setRemovable(checked);
        });
        model.getRemovable().observe(this, (value) -> {
            binding.schRemovable.setChecked(value);
        });

        binding.selUdc.setOnDropdownItemSelectedListener((v, index) -> {
            String value = binding.selUdc.getSelectedItem().toString();
            model.setUdc(value);
        });
        model.getUdc().observe(this, (value) -> {
            var items = binding.selUdc.getDropdownEntries();
            if (items.length == 0) return;
            int index = Arrays.asList(items).indexOf(value);
            binding.selUdc.setSelectedItem(Math.max(0, index));
        });

        model.getFilePaths().observe(this, (value) -> {
            LinearLayout container = binding.secFiles.getContentContainer();
            container.removeViews(1, container.getChildCount() - 1);

            for (String path : value) {
                MaterialItem item = new MaterialItem(this);
                item.setTitle(path);
                item.setClickable(true);
                item.setFocusable(true);
                item.setOnClickListener((v) -> {
                    model.setFilePaths(Objects.requireNonNull(model.getFilePaths().getValue()).stream()
                            .filter(p -> !path.equals(p))
                            .collect(Collectors.toList()));
                });
                container.addView(item);
            }
        });

        model.getGadgets().observe(this, (value) -> {
            if (value.isEmpty()) {
                binding.secGadgets.setVisibility(View.GONE);
                return;
            }
            binding.secGadgets.setVisibility(View.VISIBLE);
            var container = binding.secGadgets.getContentContainer();
            container.removeAllViews();

            for (GadgetState gadget : value) {
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
        });

        if (firstLaunch) LoadingDialog.updateMessage(getString(R.string.binder_waiting));

        Intent intent = new Intent(this, UsbMassStorageService.class);
        RootService.bind(intent, serviceConnection);
    }

    private void refresh() {
        if (binding == null || !getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) return;

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
            public void onResult(boolean result) {
                runOnUiThread(() -> {
                    if (binding == null) return;
                    isRunning = result;
                    Views.setEnabledRecursively(binding.secFiles.getContentContainer(), !result);
                    binding.secFiles.getContentContainer().setAlpha(result ? 0.5f : 1);
                    Views.setEnabledRecursively(binding.secSettings.getContentContainer(), !result);
                    binding.secSettings.getContentContainer().setAlpha(result ? 0.5f : 1);

                    if (isRunning) {
                        binding.doAction.setText(getString(R.string.stop));
                        binding.doAction.setIconResource(R.drawable.ic_stop);
                        binding.doAction.extend();
                    } else {
                        binding.doAction.setText(getString(R.string.start));
                        binding.doAction.setIconResource(R.drawable.ic_play_arrow);
                        binding.doAction.shrink();
                    }
                    binding.doAction.setVisibility(View.VISIBLE);
                });
            }
        });
        rootService.supportsConfigfs(new IBooleanCallback.Stub() {
            @Override
            public void onResult(boolean result) {
                if (binding == null || result) return;
                runOnUiThread(() -> showFatalError(getString(R.string.error_no_configfs)));
            }
        });
        rootService.getUdcList(new IStringListCallback.Stub() {
            @Override
            public void onResult(List<String> result) {
                runOnUiThread(() -> {
                    if (binding == null) return;
                    if (result == null || result.isEmpty()) {
                        showFatalError(getString(R.string.error_no_udc));
                        return;
                    }
                    binding.selUdc.setDropdownEntries(result.toArray(new CharSequence[0]));
                    String currentDefault = model.getUdc().getValue();
                    if (currentDefault == null || currentDefault.isEmpty()) return;
                    int index = result.indexOf(currentDefault);
                    if (index > -1) {
                        binding.selUdc.setSelectedItem(index);
                    } else {
                        model.setUdc(result.get(0));
                    }
                });
            }
        });
        rootService.getGadgetStateList(new IGadgetStateListCallback.Stub() {
            @Override
            public void onResult(List<GadgetState> result) {
                runOnUiThread(() -> {
                    if (binding == null) return;
                    model.setGadgets(result == null ? new ArrayList<>() : result);
                });
            }

            @Override
            public void onError(String error) {}
        });
    }

    private void doMount() {
        List<String> files = Objects.requireNonNull(model.getFilePaths().getValue());
        if (files.isEmpty()) {
            Message.snack(getString(R.string.error_no_file));
            return;
        }

        MassStorageConfig.Builder builder = new MassStorageConfig.Builder();

        files.forEach(builder::addImage);

        MassStorageConfig config = builder.setReadOnly(Objects.requireNonNull(model.getReadonly().getValue()))
                .setCdrom(Objects.requireNonNull(model.getCdrom().getValue()))
                .setRemovable(Objects.requireNonNull(model.getRemovable().getValue()))
                .setUdc(Objects.requireNonNull(model.getUdc().getValue()))
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