// IGadgetStateCallback.aidl
package ir.aeliux.usbtoolkit.callback;

import ir.aeliux.usbtoolkit.data.LunState;
import ir.aeliux.usbtoolkit.data.GadgetState;

oneway interface IGadgetStateCallback {
    void onResult(in GadgetState state);
    void onError(String error);
}