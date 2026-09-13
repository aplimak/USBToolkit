// IRootFileExistCallback.aidl
package ir.aeliux.usbtoolkit.callback;

oneway interface IRootFileExistCallback {
    void OnResult(boolean exists);
}