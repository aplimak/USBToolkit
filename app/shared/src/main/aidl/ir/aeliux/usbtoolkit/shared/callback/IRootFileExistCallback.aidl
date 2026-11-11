// IRootFileExistCallback.aidl
package ir.aeliux.usbtoolkit.shared.callback;

oneway interface IRootFileExistCallback {
    void OnResult(boolean exists);
}
