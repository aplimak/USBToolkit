// IRootFileExistCallback.aidl
package ir.aeliux.usbtoolkit;

oneway interface IRootFileExistCallback {
    void OnResult(boolean exists);
}