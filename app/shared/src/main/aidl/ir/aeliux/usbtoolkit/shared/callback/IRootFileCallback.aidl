// IRootFileCallback.aidl
package ir.aeliux.usbtoolkit.shared.callback;

import ir.aeliux.usbtoolkit.shared.dto.FileEntry;

oneway interface IRootFileCallback {
    void onFileList(in List<FileEntry> entries);
    void onError(String message);
}
