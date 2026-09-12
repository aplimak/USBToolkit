package ir.aeliux.usbtoolkit;

import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.topjohnwu.superuser.ipc.RootService;

import java.nio.file.Path;
import java.util.List;

public class UsbMassStorageService extends RootService {
    private final IUsbMassStorageService.Stub binder = new IUsbMassStorageService.Stub() {
        @Override
        public void Start(List<String> files,
                          boolean readOnly,
                          boolean cdrom,
                          boolean removable,
                          IUsbMassStorageCallback callback) {
            UsbMassStorageManager.MassStorageConfig.Builder builder = new UsbMassStorageManager.MassStorageConfig.Builder();
            for (String path : files) {
                builder.addImage(path);
            }
            UsbMassStorageManager.MassStorageConfig config = builder.setCdrom(cdrom)
                                                                    .setReadOnly(readOnly)
                                                                    .setRemovable(removable)
                                                                    .build();

            try {
                UsbMassStorageManager.setupMassStorage(config, new UsbMassStorageManager.ProgressCallback() {
                    @Override
                    public void onStepStart(String stepName) {
                        try {
                            callback.onStepStart(stepName);
                        } catch (RemoteException ignored) {

                        }
                    }

                    @Override
                    public void onStepComplete(String stepName) {
                        try {
                            callback.onStepComplete(stepName);
                        } catch (RemoteException ignored) {

                        }
                    }

                    @Override
                    public void onStepFailed(String stepName, UsbGadgetException error) {
                        try {
                            callback.onStepFailed(stepName, error.toString());
                        } catch (RemoteException ignored) {

                        }
                    }
                });
                callback.onEnd();
            } catch (UsbGadgetException e) {
                try {
                    callback.onStepFailed("[overall]", e.toString());
                } catch (RemoteException ignored) {

                }
            } catch (RemoteException ignored) {

            }
        }

        @Override
        public void Stop(IUsbMassStorageCallback callback) {
            try {
                Path configfs = UsbMassStorageManager.getConfigfsMountPoint();
                UsbMassStorageManager.cleanupGadget(configfs, UsbMassStorageManager.GADGET_NAME);
            } catch (UsbGadgetException e) {
                try {
                    callback.onStepFailed("Get ConfigFS mountpoint", e.toString());
                } catch (RemoteException ignored) {

                }
            }
        }

        @Override
        public void isRunning(IBooleanCallback callback) {
            try {
                var state = UsbMassStorageManager.detectExistingGadget();
                callback.onResult(state != null && state.bound);
            } catch (UsbGadgetException e) {
                try {
                    callback.onResult(false);
                } catch (RemoteException ignored) {

                }
            } catch (RemoteException ignored) {

            }
        }
    };

    @Nullable
    @Override
    public IBinder onBind(@NonNull Intent intent) {
        return binder;
    }
}
