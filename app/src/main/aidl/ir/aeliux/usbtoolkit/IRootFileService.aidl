// IRootFileService.aidl
package ir.aeliux.usbtoolkit;

import ir.aeliux.usbtoolkit.IRootFileCallback;
import ir.aeliux.usbtoolkit.FileEntry;

interface IRootFileService {
    // List the contents of a directory
    void listFiles(String path, IRootFileCallback callback);

    // Get metadata for a single path
    void getFileInfo(String path, IRootFileCallback callback);

    // Check if a path exists
    boolean exists(String path);

    // Get the filesystem type / mount info
    String getMountInfo(String path);
}