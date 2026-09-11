// IRootFileCallback.aidl
package ir.aeliux.usbtoolkit;

import ir.aeliux.usbtoolkit.FileEntry;
import java.util.List;

oneway interface IRootFileCallback {
    void onFileList(in List<FileEntry> entries);
    void onError(String message);
}