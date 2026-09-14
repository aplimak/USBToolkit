package ir.aeliux.usbtoolkit.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a snapshot of a gadget's state inside configfs.
 */
public class GadgetState implements Parcelable {

    public final String name;
    public final Path gadgetPath;
    public final boolean bound;
    public final String boundUdc;
    public final int vendorId;
    public final int productId;
    public final String manufacturer;
    public final String product;
    public final String serialNumber;
    public final Map<String, LunState> luns;

    public GadgetState(String name, Path gadgetPath, boolean bound, String boundUdc,
                       int vendorId, int productId, String manufacturer,
                       String product, String serialNumber,
                       Map<String, LunState> luns) {
        this.name         = name;
        this.gadgetPath   = gadgetPath;
        this.bound        = bound;
        this.boundUdc     = boundUdc;
        this.vendorId     = vendorId;
        this.productId    = productId;
        this.manufacturer = manufacturer;
        this.product      = product;
        this.serialNumber = serialNumber;
        this.luns         = Collections.unmodifiableMap(new HashMap<>(luns));
    }

    protected GadgetState(Parcel in) {
        name         = in.readString();
        String gp = in.readString();
        gadgetPath   = (gp == null) ? null : Paths.get(gp);
        bound        = in.readByte() != 0;
        boundUdc     = in.readString();
        vendorId     = in.readInt();
        productId    = in.readInt();
        manufacturer = in.readString();
        product      = in.readString();
        serialNumber = in.readString();

        int size = in.readInt();
        Map<String, LunState> map = new HashMap<>(size);
        ClassLoader cl = LunState.class.getClassLoader();
        for (int i = 0; i < size; i++) {
            String key = in.readString();
            LunState value = in.readParcelable(cl);
            map.put(key, value);
        }
        luns = Collections.unmodifiableMap(map);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(gadgetPath == null ? null : gadgetPath.toString());
        dest.writeByte((byte) (bound ? 1 : 0));
        dest.writeString(boundUdc);
        dest.writeInt(vendorId);
        dest.writeInt(productId);
        dest.writeString(manufacturer);
        dest.writeString(product);
        dest.writeString(serialNumber);

        dest.writeInt(luns.size());
        for (Map.Entry<String, LunState> e : luns.entrySet()) {
            dest.writeString(e.getKey());
            dest.writeParcelable(e.getValue(), flags);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<GadgetState> CREATOR = new Creator<GadgetState>() {
        @Override public GadgetState createFromParcel(Parcel in) { return new GadgetState(in); }
        @Override public GadgetState[] newArray(int size)          { return new GadgetState[size]; }
    };
}