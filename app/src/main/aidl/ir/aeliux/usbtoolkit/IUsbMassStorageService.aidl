// IUsbMassStorageService.aidl
package ir.aeliux.usbtoolkit;

import ir.aeliux.usbtoolkit.IUsbMassStorageCallback;
import ir.aeliux.usbtoolkit.IBooleanCallback;
import ir.aeliux.usbtoolkit.IUdcListCallback;
import ir.aeliux.usbtoolkit.LunState;
import ir.aeliux.usbtoolkit.GadgetState;
import ir.aeliux.usbtoolkit.MassStorageConfig;

oneway interface IUsbMassStorageService {
    void start(in List<String> files,
               boolean readOnly,
               boolean cdrom,
               boolean removable,
               String udc,
               IUsbMassStorageCallback callback);
    void stop(IUsbMassStorageCallback callback);
    void isRunning(IBooleanCallback callback);
    void supportsConfigfs(IBooleanCallback callback);
    void getUdcList(IUdcListCallback callback);
}