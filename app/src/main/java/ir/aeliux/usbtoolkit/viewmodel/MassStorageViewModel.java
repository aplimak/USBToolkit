package ir.aeliux.usbtoolkit.viewmodel;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

import ir.aeliux.usbtoolkit.BuildConfig;
import ir.aeliux.usbtoolkit.data.GadgetState;

public class MassStorageViewModel extends AndroidViewModel {
    private final SharedPreferences sharedPreferences;

    private final String KEY_READONLY = "readonly";
    private final String KEY_CDROM = "cd_rom";
    private final String KEY_REMOVABLE = "removable";
    private final String KEY_UDC = "udc";

    private final MutableLiveData<Boolean> readonly = new MutableLiveData<>();
    private final MutableLiveData<Boolean> cdrom = new MutableLiveData<>();
    private final MutableLiveData<Boolean> removable = new MutableLiveData<>();
    private final MutableLiveData<String> udc = new MutableLiveData<>();
    private final MutableLiveData<List<String>> filePaths = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<GadgetState>> gadgets = new MutableLiveData<>(new ArrayList<>());

    public MassStorageViewModel(@NonNull Application application) {
        super(application);
        sharedPreferences = application.getSharedPreferences("mass_storage", Context.MODE_PRIVATE);

        readonly.setValue(sharedPreferences.getBoolean(KEY_READONLY, false));
        cdrom.setValue(sharedPreferences.getBoolean(KEY_CDROM, false));
        removable.setValue(sharedPreferences.getBoolean(KEY_REMOVABLE, true));
        udc.setValue(sharedPreferences.getString(KEY_UDC, ""));
    }

    @NonNull
    public MutableLiveData<Boolean> getReadonly() {
        return readonly;
    }

    public void setReadonly(boolean value) {
        if (readonly.getValue() != null && readonly.getValue() == value) return;

        readonly.setValue(value);
        sharedPreferences.edit().putBoolean(KEY_READONLY, value).apply();
    }

    @NonNull
    public MutableLiveData<Boolean> getCdrom() {
        return cdrom;
    }

    public void setCdrom(boolean value) {
        if (cdrom.getValue() != null && cdrom.getValue() == value) return;

        cdrom.setValue(value);
        sharedPreferences.edit().putBoolean(KEY_CDROM, value).apply();
    }

    @NonNull
    public MutableLiveData<Boolean> getRemovable() {
        return removable;
    }

    public void setRemovable(boolean value) {
        if (removable.getValue() != null && removable.getValue() == value) return;

        removable.setValue(value);
        sharedPreferences.edit().putBoolean(KEY_REMOVABLE, value).apply();
    }

    @NonNull
    public MutableLiveData<String> getUdc() {
        return udc;
    }

    public void setUdc(String value) {
        if (udc.getValue() != null && value.equals(udc.getValue())) return;

        udc.setValue(value);
        sharedPreferences.edit().putString(KEY_UDC, value).apply();
    }

    public MutableLiveData<List<String>> getFilePaths() {
        return filePaths;
    }

    public void setFilePaths(List<String> value) {
        if (filePaths.getValue() != null && value.equals(filePaths.getValue())) return;

        filePaths.setValue(value);
    }

    public MutableLiveData<List<GadgetState>> getGadgets() {
        return gadgets;
    }

    public void setGadgets(List<GadgetState> value) {
        if (gadgets.getValue() != null && value.equals(gadgets.getValue())) return;

        gadgets.setValue(value);
    }
}
