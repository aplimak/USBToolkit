package ir.aeliux.usbtoolkit;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.topjohnwu.superuser.ipc.RootService;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import ir.aeliux.usbtoolkit.databinding.ActivityFilePickerBinding;

public class FilePickerActivity extends AppCompatActivity {

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

        /*
        final TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true);
        final int baseHeight = TypedValue.complexToDimensionPixelSize(
                tv.data, getResources().getDisplayMetrics());

        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            lp.height = baseHeight + systemBars.top;
            v.setLayoutParams(lp);
            return insets;
        });
        */

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);

            ViewGroup.LayoutParams lp = binding.statusBarScrim.getLayoutParams();
            lp.height = systemBars.top;
            binding.statusBarScrim.setLayoutParams(lp);

            return insets;
        });

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

        setupToolbar();
        setupRecyclerView();
        setupButtons();
        bindRootService();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> {
            setResult(Activity.RESULT_CANCELED);
            finish();
        });

        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_select) {
                Intent result = new Intent();
                result.putStringArrayListExtra(
                        EXTRA_SELECTED_PATHS,
                        new ArrayList<>(selectedPaths)
                );
                setResult(Activity.RESULT_OK, result);
                finish();
                return true;
            }
            return false;
        });

        updateConfirmButton();
    }

    private void setupRecyclerView() {
        adapter = new FileEntryAdapter(
                entry -> { // onDirectoryClick
                    changeDirectory(entry.getAbsolutePath());
                },
                (entry, checked) -> { // onFileSelect (checkbox)
                    if (checked) selectedPaths.add(entry.getAbsolutePath());
                    else selectedPaths.remove(entry.getAbsolutePath());
                    updateConfirmButton();
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
                    updateConfirmButton();
                },
                allowMultiple,
                allowedExtensions
        );

        binding.recyclerFiles.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerFiles.setAdapter(adapter);
    }

    private void setupButtons() {
        binding.btnUp.setOnClickListener(v -> {
            File parent = new File(currentPath).getParentFile();
            if (parent != null) {
                changeDirectory(parent.getAbsolutePath());
            }
        });

        binding.btnRoot.setOnClickListener(v -> {
            changeDirectory(ROOT_PATH);
        });

        binding.btnHome.setOnClickListener(v -> {
            changeDirectory(HOME_PATH);
        });
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
            Toast.makeText(this, "Root service not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            service.listFiles(currentPath, new IRootFileCallback.Stub() {
                @Override
                public void onFileList(List<FileEntry> entries) {
                    runOnUiThread(() -> {
                        adapter.submitList(new ArrayList<>(entries), () -> {
                            showLoading(false);
                            binding.tvEmpty.setVisibility(
                                    entries.isEmpty() ? View.VISIBLE : View.GONE);
                            adapter.restoreSelection(selectedPaths);
                        });
                    });
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
            Toast.makeText(this, "IPC error: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void updateConfirmButton() {
        android.view.MenuItem item = binding.toolbar.getMenu().findItem(R.id.action_select);
        if (item == null) return;
        item.setTitle("Select (" + selectedPaths.size() + ")");
        item.setEnabled(!selectedPaths.isEmpty());
    }

    private void showLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.recyclerFiles.setVisibility(loading ? View.GONE : View.VISIBLE);
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