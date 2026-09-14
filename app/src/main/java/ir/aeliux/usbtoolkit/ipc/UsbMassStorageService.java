package ir.aeliux.usbtoolkit.ipc;

import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.topjohnwu.superuser.ipc.RootService;

import java.nio.file.Path;

import ir.aeliux.usbtoolkit.IUsbMassStorageService;
import ir.aeliux.usbtoolkit.UsbGadgetException;
import ir.aeliux.usbtoolkit.UsbMassStorageManager;
import ir.aeliux.usbtoolkit.callback.IBooleanCallback;
import ir.aeliux.usbtoolkit.callback.IGadgetStateCallback;
import ir.aeliux.usbtoolkit.callback.IStringListCallback;
import ir.aeliux.usbtoolkit.callback.IUsbMassStorageCallback;
import ir.aeliux.usbtoolkit.data.MassStorageConfig;

public class UsbMassStorageService extends RootService {
    private final String TAG = "UsbMassStorageService";
    private final IUsbMassStorageService.Stub binder = new IUsbMassStorageService.Stub() {
        @Override
        public void start(MassStorageConfig config, IUsbMassStorageCallback callback) {
            Log.d(TAG, "binder.Start");
            safeCall(() -> {
                try {
                    UsbMassStorageManager.setupMassStorage(config, getCallback(callback));
                    Log.d(TAG, "firing onEnd with result: " + true);
                    callback.onEnd(true);
                } catch (UsbGadgetException e) {
                    Log.e(TAG, "Error happened in binder.start: ", e);
                    Log.d(TAG, "firing onEnd with result: " + false);
                    callback.onEnd(false);
                }
            });
        }

        @Override
        public void stop(IUsbMassStorageCallback callback) {
            Log.d(TAG, "binder.stop");
            safeCall(() -> {
                try {
                    Path configfs = UsbMassStorageManager.getConfigfsMountPoint();
                    UsbMassStorageManager.cleanupGadget(configfs, UsbMassStorageManager.GADGET_NAME, getCallback(callback));
                    Log.d(TAG, "firing onEnd with result: " + true);
                    callback.onEnd(true);
                } catch (UsbGadgetException e) {
                    Log.e(TAG, "Error happened in binder.stop: ", e);
                    Log.d(TAG, "firing onEnd with result: " + false);
                    callback.onEnd(false);
                }
            });
        }

        @Override
        public void isRunning(IBooleanCallback callback) {
            Log.d(TAG, "binder.isRunning");
            safeCall(() -> {
                try {
                    var state = UsbMassStorageManager.detectExistingGadget();
                    var result = state != null && state.bound;
                    Log.d(TAG, "firing onResult with result: " + result);
                    callback.onResult(result);
                } catch (UsbGadgetException e) {
                    Log.e(TAG, "Error happened in binder.isRunning: ", e);
                    Log.d(TAG, "firing onResult with result: " + false);
                    callback.onResult(false);
                }
            });
        }

        @Override
        public void supportsConfigfs(IBooleanCallback callback) {
            Log.d(TAG, "binder.supportsConfigfs");
            safeCall(() -> {
                try {
                    UsbMassStorageManager.getConfigfsMountPoint();
                    Log.d(TAG, "firing onResult with result: " + true);
                    callback.onResult(true);
                } catch (UsbGadgetException e) {
                    Log.e(TAG, "Error happened in binder.supportsConfigfs: ", e);
                    Log.d(TAG, "firing onResult with result: " + false);
                    callback.onResult(false);
                }
            });
        }

        @Override
        public void getUdcList(IStringListCallback callback) {
            Log.d(TAG, "binder.getUdcList");
            safeCall(() -> {
                try {
                    var result = UsbMassStorageManager.getUdcList();
                    Log.d(TAG, "firing onResult with result: " + result);
                    callback.onResult(result);
                } catch (UsbGadgetException e) {
                    Log.e(TAG, "Error happened in binder.getUdcList: ", e);
                    Log.d(TAG, "firing onResult with result: " + null);
                    callback.onResult(null);
                }
            });
        }

        @Override
        public void getGadgetList(IStringListCallback callback) {
            Log.d(TAG, "binder.getGadgetList");
            safeCall(() -> {
                try {
                    var configfs = UsbMassStorageManager.getConfigfsMountPoint();
                    var result = UsbMassStorageManager.getGadgetList(configfs);
                    Log.d(TAG, "firing onResult with result: " + result);
                    callback.onResult(result);
                } catch (UsbGadgetException e) {
                    Log.e(TAG, "Error happened in binder.getGadgetList: ", e);
                    Log.d(TAG, "firing onResult with result: " + null);
                    callback.onResult(null);
                }
            });
        }

        @Override
        public void getGadgetState(String name, IGadgetStateCallback callback) {
            Log.d(TAG, "binder.getGadgetState");
            safeCall(() -> {
                try {
                    var configfs = UsbMassStorageManager.getConfigfsMountPoint();
                    var result = UsbMassStorageManager.getGadgetState(configfs, name);
                    Log.d(TAG, "firing onResult with result: " + result);
                    callback.onResult(result);
                } catch (UsbGadgetException e) {
                    Log.e(TAG, "Error happened in binder.getGadgetState: ", e);
                    Log.d(TAG, "firing onError");
                    var cause = e.getCause();
                    callback.onError(e + (cause != null ? "\n Caused by: " + cause : ""));
                }
            });
        }

        @NonNull
        private UsbMassStorageManager.ProgressCallback getCallback(IUsbMassStorageCallback callback) {
            return new UsbMassStorageManager.ProgressCallback() {
                @Override
                public void onStepStart(String stepName) {
                    safeCall(() -> {
                        Log.d(TAG, "firing onStepStart with step: " + stepName);
                        callback.onStepStart(stepName);
                    });
                }

                @Override
                public void onStepComplete(String stepName) {
                    safeCall(() -> {
                        Log.d(TAG, "firing onStepComplete with step: " + stepName);
                        callback.onStepComplete(stepName);
                    });
                }

                @Override
                public void onStepFailed(String stepName, UsbGadgetException error) {
                    safeCall(() -> {
                        Log.e(TAG, "Error happened in step: " + stepName, error);
                        Log.d(TAG, "firing onStepFailed with step: " + stepName);
                        var cause = error.getCause();
                        callback.onStepFailed(stepName, error + (cause != null ? "\n Caused by: " + cause : ""));
                    });
                }
            };
        }
    };

    private interface RemoteAction {
        void run() throws RemoteException;
    }

    private static void safeCall(RemoteAction action) {
        try {
            action.run();
        } catch (RemoteException ignored) {
        }
    }

    @Nullable
    @Override
    public IBinder onBind(@NonNull Intent intent) {
        Log.d(TAG, "onBind");
        return binder;
    }
}
