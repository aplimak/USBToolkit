package ir.aeliux.usbtoolkit.rootservice;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public final class RootServiceState {
    private final String KEY_UDC = "udc";
    private final SharedPreferences prefs;
    private final Object exitLocksLock = new Object();
    private final Set<String> exitLocks = new HashSet<>();
    private final Set<String> udcList = new LinkedHashSet<>();
    private String udc;

    public RootServiceState(Context ctx) {
        prefs = ctx.getSharedPreferences("root_service", Context.MODE_PRIVATE);

        udc = prefs.getString(KEY_UDC, "");
    }

    public void setExitLock(String lockId) {
        synchronized (exitLocksLock) {
            exitLocks.add(lockId);
        }
    }

    public void unsetExitLock(String lockId) {
        synchronized (exitLocksLock) {
            exitLocks.remove(lockId);
        }
    }

    public boolean canExit() {
        synchronized (exitLocksLock) {
            return exitLocks.isEmpty();
        }
    }

    public String getUdc() {
        if (!TextUtils.isEmpty(udc) && udcList.contains(udc)) {
            return udc;
        }
        if (!udcList.isEmpty()) {
            String firstUdc = udcList.iterator().next();
            udc = firstUdc;
            return firstUdc;
        }
        return "";
    }

    public void setUdc(String udc) {
        if (!udcList.contains(udc)) {
            throw new IllegalArgumentException("UDC is not exists: " + udc);
        }
        this.udc = udc;
        prefs.edit().putString(KEY_UDC, udc).apply();
    }

    public Set<String> getUdcList() {
        return udcList;
    }
}
