// IUsbMassStorageService.aidl
package ir.aeliux.usbtoolkit.ipc;

import ir.aeliux.usbtoolkit.callback.IUsbMassStorageCallback;
import ir.aeliux.usbtoolkit.callback.IBooleanCallback;
import ir.aeliux.usbtoolkit.callback.IStringListCallback;
import ir.aeliux.usbtoolkit.callback.IGadgetStateCallback;
import ir.aeliux.usbtoolkit.callback.IGadgetStateListCallback;
import ir.aeliux.usbtoolkit.data.LunState;
import ir.aeliux.usbtoolkit.data.GadgetState;
import ir.aeliux.usbtoolkit.data.MassStorageConfig;

oneway interface IUsbMassStorageService {
    void start(in MassStorageConfig config, IUsbMassStorageCallback callback);
    void stop(IUsbMassStorageCallback callback);
    void isRunning(IBooleanCallback callback);
    void supportsConfigfs(IBooleanCallback callback);
    void getUdcList(IStringListCallback callback);
    void getGadgetList(IStringListCallback callback);
    void getGadgetState(String name, IGadgetStateCallback callback);
    void getGadgetStateList(IGadgetStateListCallback callback);
}