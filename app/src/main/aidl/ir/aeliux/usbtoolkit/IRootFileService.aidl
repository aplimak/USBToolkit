// IRootFileService.aidl
package ir.aeliux.usbtoolkit;

import ir.aeliux.usbtoolkit.IRootFileCallback;
import ir.aeliux.usbtoolkit.IRootFileExistCallback;
import ir.aeliux.usbtoolkit.FileEntry;

oneway interface IRootFileService {
    // List the contents of a directory
    void listFiles(String path, IRootFileCallback callback);

    // Get metadata for a single path
    void getFileInfo(String path, IRootFileCallback callback);

    // Check if a path exists
    void exists(String path, IRootFileExistCallback callback);
}