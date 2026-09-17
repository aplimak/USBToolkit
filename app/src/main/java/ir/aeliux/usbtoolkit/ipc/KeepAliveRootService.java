package ir.aeliux.usbtoolkit.ipc;

import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.topjohnwu.superuser.ipc.RootService;

public class KeepAliveRootService extends RootService {
    private final String TAG = "KeepAliveRootService";

    private final IKeepAliveRootService.Stub binder = new IKeepAliveRootService.Stub(){};

    @Nullable
    @Override
    public IBinder onBind(@NonNull Intent intent) {
        Log.d(TAG, "onBind");
        return binder;
    }

    @Override
    public void onRebind(@NonNull Intent intent) {
        Log.d(TAG, "onRebind");
    }

    @Override
    public boolean onUnbind(@NonNull Intent intent) {
        Log.d(TAG, "onUnbind");
        return super.onUnbind(intent);
    }
}