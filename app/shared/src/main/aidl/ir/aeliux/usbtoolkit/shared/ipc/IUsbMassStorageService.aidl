// IUsbMassStorageService.aidl
package ir.aeliux.usbtoolkit.shared.ipc;

import ir.aeliux.usbtoolkit.shared.callback.IUsbMassStorageCallback;
import ir.aeliux.usbtoolkit.shared.callback.IBooleanCallback;
import ir.aeliux.usbtoolkit.shared.callback.IStringListCallback;
import ir.aeliux.usbtoolkit.shared.callback.IGadgetStateCallback;
import ir.aeliux.usbtoolkit.shared.callback.IGadgetStateListCallback;
import ir.aeliux.usbtoolkit.shared.callback.IMagicResultCallback;
import ir.aeliux.usbtoolkit.shared.dto.LunState;
import ir.aeliux.usbtoolkit.shared.dto.GadgetState;
import ir.aeliux.usbtoolkit.shared.dto.MassStorageConfig;

oneway interface IUsbMassStorageService {
    void start(in MassStorageConfig config, IUsbMassStorageCallback callback);
    void stop(IUsbMassStorageCallback callback);
    void isRunning(IBooleanCallback callback);
    void supportsConfigfs(IBooleanCallback callback);
    void getUdcList(IStringListCallback callback);
    void getGadgetList(IStringListCallback callback);
    void getGadgetState(String name, IGadgetStateCallback callback);
    void getGadgetStateList(IGadgetStateListCallback callback);
    void analyzeFiles(in List<String> paths, IMagicResultCallback callback);
}
