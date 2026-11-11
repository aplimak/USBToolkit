// IRootFileCallback.aidl
package ir.aeliux.usbtoolkit.shared.callback;

import ir.aeliux.magic.MagicResult;

oneway interface IMagicResultCallback {
    void onResult(in Map<String, MagicResult> result);
}
