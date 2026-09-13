// IUdcListCallback.aidl
package ir.aeliux.usbtoolkit;

oneway interface IUdcListCallback {
    void onResult(in List<String> result);
}