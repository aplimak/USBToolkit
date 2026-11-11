// IGadgetStateListCallback.aidl
package ir.aeliux.usbtoolkit.shared.callback;

import ir.aeliux.usbtoolkit.shared.dto.LunState;
import ir.aeliux.usbtoolkit.shared.dto.GadgetState;

oneway interface IGadgetStateListCallback {
    void onResult(in List<GadgetState> result);
    void onError(String error);
}
