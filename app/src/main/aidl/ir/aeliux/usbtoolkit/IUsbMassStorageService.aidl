// IUsbMassStorageService.aidl
package ir.aeliux.usbtoolkit;

import ir.aeliux.usbtoolkit.IUsbMassStorageCallback;
import ir.aeliux.usbtoolkit.IBooleanCallback;
import ir.aeliux.usbtoolkit.IUdcListCallback;
import ir.aeliux.usbtoolkit.data.LunState;
import ir.aeliux.usbtoolkit.data.GadgetState;
import ir.aeliux.usbtoolkit.data.MassStorageConfig;

oneway interface IUsbMassStorageService {
    void start(in MassStorageConfig config, IUsbMassStorageCallback callback);
    void stop(IUsbMassStorageCallback callback);
    void isRunning(IBooleanCallback callback);
    void supportsConfigfs(IBooleanCallback callback);
    void getUdcList(IUdcListCallback callback);
}