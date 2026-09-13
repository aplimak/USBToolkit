// IRootFileCallback.aidl
package ir.aeliux.usbtoolkit.callback;

import ir.aeliux.usbtoolkit.data.FileEntry;

oneway interface IRootFileCallback {
    void onFileList(in List<FileEntry> entries);
    void onError(String message);
}