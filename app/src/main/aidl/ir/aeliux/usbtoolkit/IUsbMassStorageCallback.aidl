// IUsbMassStorageCallback.aidl
package ir.aeliux.usbtoolkit;

oneway interface IUsbMassStorageCallback {
    void onStepStart(String stepName);
    void onStepComplete(String stepName);
    void onStepFailed(String stepName, String error);
    void onEnd();
}