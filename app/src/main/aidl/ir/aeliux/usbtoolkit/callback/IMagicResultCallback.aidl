// IRootFileCallback.aidl
package ir.aeliux.usbtoolkit.callback;

import ir.aeliux.usbtoolkit.data.MagicResult;

oneway interface IMagicResultCallback {
    void onResult(in Map<String, MagicResult> result);
}