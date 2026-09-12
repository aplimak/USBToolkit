package ir.aeliux.usbtoolkit;

import android.app.Application;

public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        App.init(this);
        LoadingDialog.init(this);
    }
}
