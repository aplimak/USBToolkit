// IUsbMassStorageCallback.aidl
package ir.aeliux.usbtoolkit.shared.callback;

oneway interface IUsbMassStorageCallback {
    void onStepStart(String stepName);
    void onStepComplete(String stepName);
    void onStepFailed(String stepName, String error);
    void onEnd(boolean result);
}
