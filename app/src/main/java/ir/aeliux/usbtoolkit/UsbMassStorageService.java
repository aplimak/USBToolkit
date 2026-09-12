package ir.aeliux.usbtoolkit;

import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.topjohnwu.superuser.ipc.RootService;

import java.nio.file.Path;
import java.util.List;

public class UsbMassStorageService extends RootService {
    private final String TAG = "UsbMassStorageService";
    private final IUsbMassStorageService.Stub binder = new IUsbMassStorageService.Stub() {
        @Override
        public void start(List<String> files,
                          boolean readOnly,
                          boolean cdrom,
                          boolean removable,
                          IUsbMassStorageCallback callback) {
            Log.d(TAG, "binder.Start");
            UsbMassStorageManager.MassStorageConfig.Builder builder = new UsbMassStorageManager.MassStorageConfig.Builder();
            for (String path : files) {
                builder.addImage(path);
            }
            UsbMassStorageManager.MassStorageConfig config = builder.setCdrom(cdrom)
                                                                    .setReadOnly(readOnly)
                                                                    .setRemovable(removable)
                                                                    .build();

            try {
                UsbMassStorageManager.setupMassStorage(config, getCallback(callback));
                Log.d(TAG, "firing onEnd with result: " + true);
                callback.onEnd(true);
            } catch (UsbGadgetException e) {
                try {
                    Log.e(TAG, "Error happened in binder.start: ", e);
                    Log.d(TAG, "firing onEnd with result: " + false);
                    callback.onEnd(false);
                } catch (RemoteException ignored1) {}
            } catch (RemoteException ignored) {}
        }

        @Override
        public void stop(IUsbMassStorageCallback callback) {
            Log.d(TAG, "binder.stop");
            try {
                Path configfs;
                try {
                    configfs = UsbMassStorageManager.getConfigfsMountPoint();
                } catch (UsbGadgetException e) {
                    callback.onStepFailed("Get ConfigFS mountpoint", e.toString());
                    throw e;
                }
                UsbMassStorageManager.cleanupGadget(configfs, UsbMassStorageManager.GADGET_NAME, getCallback(callback));
                Log.d(TAG, "firing onEnd with result: " + true);
                callback.onEnd(true);
            } catch (UsbGadgetException e) {
                try {
                    Log.e(TAG, "Error happened in binder.stop: ", e);
                    Log.d(TAG, "firing onEnd with result: " + false);
                    callback.onEnd(false);
                } catch (RemoteException ignored) {}
            } catch (RemoteException ignored) {}
        }

        @Override
        public void isRunning(IBooleanCallback callback) {
            Log.d(TAG, "binder.isRunning");
            try {
                var state = UsbMassStorageManager.detectExistingGadget();
                var result = state != null && state.bound;
                Log.d(TAG, "firing onResult with result: " + result);
                callback.onResult(result);
            } catch (UsbGadgetException e) {
                try {
                    Log.e(TAG, "Error happened in binder.isRunning: ", e);
                    Log.d(TAG, "firing onResult with result: " + false);
                    callback.onResult(false);
                } catch (RemoteException ignored) {}
            } catch (RemoteException ignored) {}
        }

        @NonNull
        private UsbMassStorageManager.ProgressCallback getCallback(IUsbMassStorageCallback callback) {
            return new UsbMassStorageManager.ProgressCallback() {
                @Override
                public void onStepStart(String stepName) {
                    try {
                        Log.d(TAG, "firing onStepStart with step: " + stepName);
                        callback.onStepStart(stepName);
                    } catch (RemoteException ignored) {
                    }
                }

                @Override
                public void onStepComplete(String stepName) {
                    try {
                        Log.d(TAG, "firing onStepComplete with step: " + stepName);
                        callback.onStepComplete(stepName);
                    } catch (RemoteException ignored) {
                    }
                }

                @Override
                public void onStepFailed(String stepName, UsbGadgetException error) {
                    try {
                        Log.e(TAG, "Error happened in step: " + stepName, error);
                        Log.d(TAG, "firing onStepFailed with step: " + stepName);
                        callback.onStepFailed(stepName, error.toString());
                    } catch (RemoteException ignored) {
                    }
                }
            };
        }
    };

    @Nullable
    @Override
    public IBinder onBind(@NonNull Intent intent) {
        Log.d(TAG, "onBind");
        return binder;
    }
}
