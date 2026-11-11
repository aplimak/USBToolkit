// IGadgetStateCallback.aidl
package ir.aeliux.usbtoolkit.shared.callback;

import ir.aeliux.usbtoolkit.shared.dto.LunState;
import ir.aeliux.usbtoolkit.shared.dto.GadgetState;

oneway interface IGadgetStateCallback {
    void onResult(in GadgetState state);
    void onError(String error);
}
