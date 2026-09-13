// IUdcListCallback.aidl
package ir.aeliux.usbtoolkit.callback;

oneway interface IUdcListCallback {
    void onResult(in List<String> result);
}