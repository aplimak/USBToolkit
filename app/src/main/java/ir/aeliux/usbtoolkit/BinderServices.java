package ir.aeliux.usbtoolkit;

import android.content.Context;

public class BinderServices {
    public static final Class<?> UMS_SERVICE;
    public static final Class<?> RF_SERVICE;
    public static final Class<?> KAR_SERVICE;

    static {
        try {
            UMS_SERVICE = Class.forName("ir.aeliux.usbtoolkit.rootservice.UsbMassStorageService");
            RF_SERVICE = Class.forName("ir.aeliux.usbtoolkit.rootservice.RootFileService");
            KAR_SERVICE = Class.forName("ir.aeliux.usbtoolkit.rootservice.KeepAliveRootService");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
