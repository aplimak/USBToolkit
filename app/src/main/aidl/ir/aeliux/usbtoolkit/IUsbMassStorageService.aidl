// IUsbMassStorageService.aidl
package ir.aeliux.usbtoolkit;

import ir.aeliux.usbtoolkit.IUsbMassStorageCallback;
import ir.aeliux.usbtoolkit.IBooleanCallback;

oneway interface IUsbMassStorageService {
    void Start(in List<String> files,
               boolean readOnly,
               boolean cdrom,
               boolean removable,
               IUsbMassStorageCallback callback);

    void Stop(IUsbMassStorageCallback callback);

    void isRunning(IBooleanCallback callback);
}