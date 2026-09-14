// IGadgetStateListCallback.aidl
package ir.aeliux.usbtoolkit.callback;

import ir.aeliux.usbtoolkit.data.LunState;
import ir.aeliux.usbtoolkit.data.GadgetState;

oneway interface IGadgetStateListCallback {
    void onResult(in List<GadgetState> result);
    void onError(String error);
}