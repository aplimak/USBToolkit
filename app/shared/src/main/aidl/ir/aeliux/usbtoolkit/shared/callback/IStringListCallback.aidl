// IStringListCallback.aidl
package ir.aeliux.usbtoolkit.shared.callback;

oneway interface IStringListCallback {
    void onResult(in List<String> result);
}
