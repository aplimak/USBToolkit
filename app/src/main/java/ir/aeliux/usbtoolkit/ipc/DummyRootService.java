package ir.aeliux.usbtoolkit.ipc;

import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.Nullable;

import com.topjohnwu.superuser.ipc.RootService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ir.aeliux.usbtoolkit.callback.IRootFileCallback;
import ir.aeliux.usbtoolkit.callback.IRootFileExistCallback;
import ir.aeliux.usbtoolkit.data.FileEntry;

public class DummyRootService extends RootService {

    private final IDummyRootService.Stub binder = new IDummyRootService.Stub(){};

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}