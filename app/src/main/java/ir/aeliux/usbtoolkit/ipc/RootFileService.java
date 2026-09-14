package ir.aeliux.usbtoolkit.ipc;

import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.Nullable;

import com.topjohnwu.superuser.ipc.RootService;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import ir.aeliux.usbtoolkit.IRootFileService;
import ir.aeliux.usbtoolkit.callback.IRootFileCallback;
import ir.aeliux.usbtoolkit.callback.IRootFileExistCallback;
import ir.aeliux.usbtoolkit.data.FileEntry;

public class RootFileService extends RootService {

    private final IRootFileService.Stub binder = new IRootFileService.Stub() {

        @Override
        public void listFiles(String path, IRootFileCallback callback) {
            try {
                File dir = new File(path);
                if (!dir.exists()) {
                    callback.onError("Path does not exist: " + path);
                    return;
                }
                if (!dir.isDirectory()) {
                    callback.onError("Not a directory: " + path);
                    return;
                }

                File[] children = dir.listFiles();
                List<FileEntry> entries = new ArrayList<>();
                if (children != null) {
                    for (File child : children) {
                        entries.add(new FileEntry(
                                child.getName(),
                                child.getAbsolutePath(),
                                child.isDirectory(),
                                child.isDirectory() ? 0L : child.length(),
                                child.lastModified()
                        ));
                    }
                }

                entries.sort(new Comparator<FileEntry>() {
                    @Override
                    public int compare(FileEntry a, FileEntry b) {
                        if (a.isDirectory() != b.isDirectory()) {
                            return a.isDirectory() ? -1 : 1;
                        }
                        return a.getName().compareToIgnoreCase(b.getName());
                    }
                });

                callback.onFileList(entries);
            } catch (RemoteException e) {
                // Callback process died; nothing we can do
            } catch (Exception e) {
                try {
                    callback.onError("Error listing files: " + e.getMessage());
                } catch (RemoteException ignored) { }
            }
        }

        @Override
        public void getFileInfo(String path, IRootFileCallback callback) {
            try {
                File file = new File(path);
                if (!file.exists()) {
                    callback.onError("File does not exist: " + path);
                    return;
                }
                List<FileEntry> single = new ArrayList<>(1);
                single.add(new FileEntry(
                        file.getName(),
                        file.getAbsolutePath(),
                        file.isDirectory(),
                        file.length(),
                        file.lastModified()
                ));
                callback.onFileList(single);
            } catch (RemoteException e) {
                // ignore
            } catch (Exception e) {
                try {
                    callback.onError("Error: " + e.getMessage());
                } catch (RemoteException ignored) { }
            }
        }

        @Override
        public void exists(String path, IRootFileExistCallback callback) {
            boolean exists = new File(path).exists();
            try {
                callback.OnResult(exists);
            } catch (RemoteException e) {
                // Ignore
            }
        }

    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}