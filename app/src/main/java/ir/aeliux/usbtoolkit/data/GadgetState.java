package ir.aeliux.usbtoolkit.data;

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import ir.aeliux.usbtoolkit.util.DataConversion;

/**
 * Represents a snapshot of a gadget's state inside configfs.
 */
public class GadgetState implements Parcelable {

    public static final int RESERVED_BIT_7 = 0x80;
    public static final int SELF_POWERED   = 0x40;
    public static final int REMOTE_WAKEUP  = 0x20;
    public static final int RESERVED_LOW   = 0x1F;

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
    public final int bcdUsb;
    public final int bcdDevice;
    public final int bmAttributes;

    public GadgetState(String name, Path gadgetPath, boolean bound, String boundUdc,
                       int vendorId, int productId, String manufacturer,
                       String product, String serialNumber,
                       Map<String, LunState> luns, int bcdUsb, int bcdDevice,
                       int bmAttributes) {
        this.name         = name;
        this.gadgetPath   = gadgetPath;
        this.bound        = bound;
        this.boundUdc     = boundUdc;
        this.vendorId     = vendorId;
        this.productId    = productId;
        this.manufacturer = manufacturer;
        this.product      = product;
        this.serialNumber = serialNumber;
        this.luns         = Map.copyOf(luns);
        this.bcdUsb       = bcdUsb;
        this.bcdDevice    = bcdDevice;
        this.bmAttributes = bmAttributes & 0xFF;
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
            LunState value;
            if (Build.VERSION.SDK_INT >= 33) {
                value = in.readParcelable(cl, LunState.class);
            } else {
                value = in.readParcelable(cl);
            }
            map.put(key, value);
        }
        luns = Collections.unmodifiableMap(map);
        bcdUsb = in.readInt();
        bcdDevice = in.readInt();
        bmAttributes = in.readInt();
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
        dest.writeInt(bcdUsb);
        dest.writeInt(bcdDevice);
        dest.writeInt(bmAttributes);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<GadgetState> CREATOR = new Creator<>() {
        @Override
        public GadgetState createFromParcel(Parcel in) {
            return new GadgetState(in);
        }

        @Override
        public GadgetState[] newArray(int size) {
            return new GadgetState[size];
        }
    };

    public boolean isReservedBit7Set() {
        return (bmAttributes & RESERVED_BIT_7) != 0;
    }

    public boolean isSelfPowered() {
        return (bmAttributes & SELF_POWERED) != 0;
    }

    public boolean isRemoteWakeup() {
        return (bmAttributes & REMOTE_WAKEUP) != 0;
    }

    public int reservedLowBits() {
        return bmAttributes & RESERVED_LOW;
    }

    public String formatBcdUsb() {
        return DataConversion.formatBcd(bcdUsb);
    }

    public String formatBcdDevice() {
        return DataConversion.formatBcd(bcdDevice);
    }

    public String formatBmAttributes() {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("0x%02X (", bmAttributes));

        sb.append(isSelfPowered() ? "Self-Powered" : "Bus-Powered");
        sb.append(", Remote Wakeup: ")
                .append(isRemoteWakeup() ? "Enabled" : "Disabled");

        if (!isReservedBit7Set()) {
            sb.append(", WARNING: bit 7 should be 1");
        }

        if (reservedLowBits() != 0) {
            sb.append(String.format(
                    ", WARNING: reserved low bits set: 0x%02X",
                    reservedLowBits()
            ));
        }

        sb.append(")");
        return sb.toString();
    }
}