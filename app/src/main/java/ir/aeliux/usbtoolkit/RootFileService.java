package ir.aeliux.usbtoolkit;

import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.Nullable;

import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.ipc.RootService;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
                                child.lastModified(),
                                getPermissions(child.getAbsolutePath())
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
                        file.lastModified(),
                        getPermissions(path)
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
        public boolean exists(String path) {
            return new File(path).exists();
        }

        @Override
        public String getMountInfo(String path) {
            Shell.Result result = Shell.cmd("mount | grep '" + path + "'").exec();
            if (result.isSuccess()) {
                return joinLines(result.getOut());
            }
            return "No mount info found";
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private static String joinLines(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) sb.append('\n');
            sb.append(lines.get(i));
        }
        return sb.toString();
    }

    private String getPermissions(String path) {
        // Single-quote the path so spaces / special chars are safe
        Shell.Result result = Shell.cmd("stat -c '%A' '" + path + "' 2>/dev/null").exec();
        if (result.isSuccess() && !result.getOut().isEmpty()) {
            return result.getOut().get(0).trim();
        }
        File file = new File(path);
        String r = file.canRead() ? "r" : "-";
        String w = file.canWrite() ? "w" : "-";
        String x = file.canExecute() ? "x" : "-";
        return r + w + x + "------";
    }
}