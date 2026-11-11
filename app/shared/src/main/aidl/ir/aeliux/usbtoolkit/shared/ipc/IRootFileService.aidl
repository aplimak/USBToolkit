// IRootFileService.aidl
package ir.aeliux.usbtoolkit.shared.ipc;

import ir.aeliux.usbtoolkit.shared.callback.IRootFileCallback;
import ir.aeliux.usbtoolkit.shared.callback.IRootFileExistCallback;
import ir.aeliux.usbtoolkit.shared.dto.FileEntry;

oneway interface IRootFileService {
    // List the contents of a directory
    void listFiles(String path, IRootFileCallback callback);

    // Get metadata for a single path
    void getFileInfo(String path, IRootFileCallback callback);

    // Check if a path exists
    void exists(String path, IRootFileExistCallback callback);
}
