package ir.aeliux.usbtoolkit.rootservice;

import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.topjohnwu.superuser.ipc.RootService;

import ir.aeliux.usbtoolkit.shared.ipc.IKeepAliveRootService;

public class KeepAliveRootService extends RootService {
    private final String TAG = "KeepAliveRootService";

    private final IKeepAliveRootService.Stub binder = new IKeepAliveRootService.Stub() {
    };

    @Nullable
    @Override
    public IBinder onBind(@NonNull Intent intent) {
        Log.d(TAG, "onBind");
        return binder;
    }

    @Override
    public boolean onUnbind(@NonNull Intent intent) {
        Log.d(TAG, "onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(@NonNull Intent intent) {
        Log.d(TAG, "onRebind");
    }
}
