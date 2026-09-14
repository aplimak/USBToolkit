// IRootFileService.aidl
package ir.aeliux.usbtoolkit.ipc;

import ir.aeliux.usbtoolkit.callback.IRootFileCallback;
import ir.aeliux.usbtoolkit.callback.IRootFileExistCallback;
import ir.aeliux.usbtoolkit.data.FileEntry;

oneway interface IRootFileService {
    // List the contents of a directory
    void listFiles(String path, IRootFileCallback callback);

    // Get metadata for a single path
    void getFileInfo(String path, IRootFileCallback callback);

    // Check if a path exists
    void exists(String path, IRootFileExistCallback callback);
}