// IUsbMassStorageService.aidl
package ir.aeliux.usbtoolkit;

import ir.aeliux.usbtoolkit.IUsbMassStorageCallback;
import ir.aeliux.usbtoolkit.IBooleanCallback;

oneway interface IUsbMassStorageService {
    void start(in List<String> files,
               boolean readOnly,
               boolean cdrom,
               boolean removable,
               IUsbMassStorageCallback callback);

    void stop(IUsbMassStorageCallback callback);

    void isRunning(IBooleanCallback callback);
}