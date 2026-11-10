// IStringListCallback.aidl
package ir.aeliux.usbtoolkit.callback;

oneway interface IStringListCallback {
    void onResult(in List<String> result);
}