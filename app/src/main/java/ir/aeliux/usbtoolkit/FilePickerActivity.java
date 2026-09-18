package ir.aeliux.usbtoolkit;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.topjohnwu.superuser.ipc.RootService;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import ir.aeliux.usbtoolkit.callback.IRootFileCallback;
import ir.aeliux.usbtoolkit.data.FileEntry;
import ir.aeliux.usbtoolkit.databinding.ActivityFilePickerBinding;
import ir.aeliux.usbtoolkit.ipc.IRootFileService;
import ir.aeliux.usbtoolkit.ipc.RootFileService;
import ir.aeliux.usbtoolkit.util.FileEntryAdapter;
import ir.aeliux.usbtoolkit.util.Message;

public class FilePickerActivity extends BaseActivity {

    public static final String EXTRA_SELECTED_PATHS = "selected_paths";
    public static final String EXTRA_ALLOW_MULTIPLE = "allow_multiple";
    public static final String EXTRA_START_PATH = "start_path";
    public static final String EXTRA_ALLOWED_EXTENSIONS = "allowed_extensions";

    public static final String ROOT_PATH = "/";
    public static final String HOME_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();


    private ActivityFilePickerBinding binding;
    private FileEntryAdapter adapter;

    private IRootFileService rootService;
    private boolean isBound = false;

    private String currentPath = "/";
    private final Set<String> selectedPaths = new HashSet<>();
    private boolean allowMultiple = true;
    private Set<String> allowedExtensions = null;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            rootService = IRootFileService.Stub.asInterface(service);
            isBound = true;
            loadDirectory();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            rootService = null;
            isBound = false;
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityFilePickerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar(binding.toolbar);
        applyWindowInsets(binding.main);

        allowMultiple = getIntent().getBooleanExtra(EXTRA_ALLOW_MULTIPLE, true);

        ArrayList<String> extList = getIntent().getStringArrayListExtra(EXTRA_ALLOWED_EXTENSIONS);
        if (extList != null) {
            allowedExtensions = new HashSet<>();
            for (String e : extList) {
                allowedExtensions.add(e.toLowerCase(Locale.ROOT));
            }
        }

        String start = getIntent().getStringExtra(EXTRA_START_PATH);
        if (start != null) currentPath = start;

        setupRecyclerView();
        setupButtons();
        bindRootService();
        updateCounter();
    }

    private void updateCounter() {
        binding.toolbar.setSubtitle(getString(R.string.n_file_selected, selectedPaths.size()));
    }

    private void setupRecyclerView() {
        adapter = new FileEntryAdapter(
                entry -> { // onDirectoryClick
                    changeDirectory(entry.getAbsolutePath());
                },
                (entry, checked) -> { // onFileSelect (checkbox)
                    if (checked) selectedPaths.add(entry.getAbsolutePath());
                    else selectedPaths.remove(entry.getAbsolutePath());
                    updateCounter();
                },
                entry -> { // onFileClick (row)
                    boolean wasSelected = selectedPaths.contains(entry.getAbsolutePath());
                    if (!allowMultiple) {
                        selectedPaths.clear();
                        adapter.deselectAll();
                    }
                    boolean newState = !wasSelected;
                    if (newState) selectedPaths.add(entry.getAbsolutePath());
                    else selectedPaths.remove(entry.getAbsolutePath());
                    adapter.setSelected(entry.getAbsolutePath(), newState);
                    updateCounter();
                },
                allowMultiple,
                allowedExtensions
        );

        binding.recyclerFiles.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerFiles.setAdapter(adapter);
    }

    private void setupButtons() {
        binding.actionConfirm.setOnClickListener(v -> {
            if (selectedPaths.isEmpty()) {
                Message.snack(getString(R.string.error_no_file));
                return;
            }
            Intent result = new Intent();
            result.putStringArrayListExtra(
                    EXTRA_SELECTED_PATHS,
                    new ArrayList<>(selectedPaths)
            );
            setResult(Activity.RESULT_OK, result);
            finish();
        });

        binding.btnUp.setOnClickListener(v -> {
            File parent = new File(currentPath).getParentFile();
            if (parent != null) {
                changeDirectory(parent.getAbsolutePath());
            }
        });

        binding.btnRoot.setOnClickListener(v -> changeDirectory(ROOT_PATH));

        binding.btnHome.setOnClickListener(v -> changeDirectory(HOME_PATH));
    }

    private void changeDirectory(String path) {
        if (!path.equals(currentPath)) {
            currentPath = path;
            loadDirectory();
        }
    }

    private void bindRootService() {
        Intent intent = new Intent(this, RootFileService.class);
        RootService.bind(intent, serviceConnection);
    }

    private void loadDirectory() {
        showLoading(true);
        binding.tvCurrentPath.setText(currentPath);

        boolean hasParent = new File(currentPath).getParentFile() != null;
        binding.btnUp.setEnabled(hasParent);
        binding.btnRoot.setEnabled(!ROOT_PATH.equals(currentPath));
        binding.btnHome.setEnabled(!HOME_PATH.equals(currentPath));

        IRootFileService service = rootService;
        if (service == null) {
            showLoading(false);
            Toast.makeText(this, R.string.error_no_binder, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            service.listFiles(currentPath, new IRootFileCallback.Stub() {
                @Override
                public void onFileList(List<FileEntry> entries) {
                    runOnUiThread(() -> adapter.submitList(new ArrayList<>(entries), () -> {
                        showLoading(false);
                        binding.tvEmpty.setVisibility(
                                entries.isEmpty() ? View.VISIBLE : View.GONE);
                        adapter.restoreSelection(selectedPaths);
                    }));
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(FilePickerActivity.this,
                                message, Toast.LENGTH_LONG).show();
                    });
                }
            });
        } catch (RemoteException e) {
            showLoading(false);
            Toast.makeText(this, getString(R.string.error_binder, e.getMessage()),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.recyclerFiles.setVisibility(loading ? View.GONE : View.VISIBLE);
        binding.btnUp.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        binding.btnRoot.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        binding.btnHome.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
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