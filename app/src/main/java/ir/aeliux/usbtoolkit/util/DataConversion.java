package ir.aeliux.usbtoolkit.util;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.Locale;

public final class DataConversion {
    public static int stringToInt(String value, int radix) {
        if (radix == 16 && (value.startsWith("0x") || value.startsWith("0X"))) {
            value = value.substring(2);
        }

        return Integer.parseInt(value, radix);
    }

    public static String intToString(int value, int radix) {
        return  (radix == 16 ? "0x" : "") + Integer.toString(value, radix);
    }

    /**
     * Formats a 16-bit BCD value (e.g., bcdUSB, bcdDevice) into a human-readable
     * version string like "2.00" or "1.10".
     * <p>
     * The BCD encoding packs two decimal digits per byte:
     *   high byte = major version (integer part)
     *   low byte  = minor version (fractional part, two decimal digits)
     * <p>
     * Example:
     *   0x0200 -> "2.00"
     *   0x0110 -> "1.10"
     *   0x0100 -> "1.00"
     *
     * @param raw the raw 16-bit value (e.g., 0x0200)
     * @return formatted string like "2.00"
     */
    public static String formatBcd(int raw) {
        int highByte = (raw >> 8) & 0xFF;
        int lowByte  = raw & 0xFF;

        int major = bcdToInt(highByte);
        int minor = bcdToInt(lowByte);

        return String.format(Locale.ENGLISH, "%d.%02d", major, minor);
    }

    /**
     * Converts a single byte in BCD format to its decimal integer value.
     * Each nibble represents a decimal digit.
     * <p>
     * Example:
     *   0x02 -> 2
     *   0x10 -> 10
     *   0x99 -> 99
     *
     * @param bcd the byte value in BCD (0x00 - 0x99)
     * @return decimal integer
     */
    private static int bcdToInt(int bcd) {
        int tens = (bcd >> 4) & 0x0F;
        int ones = bcd & 0x0F;
        return tens * 10 + ones;
    }

    /**
     * Converts a Parcelable object to a byte array.
     * @param parcelable The object to extract data.
     * @return Byte array containing parcelable's data.
     */
    public static byte[] marshall(Parcelable parcelable) {
        Parcel parcel = Parcel.obtain();
        parcelable.writeToParcel(parcel, 0);
        byte[] bytes = parcel.marshall();
        parcel.recycle(); // Always recycle the Parcel to return it to the pool
        return bytes;
    }

    public static String safeMarshall(Parcelable parcelable) {
        byte[] bytes = marshall(parcelable);

        // Using NO_WRAP | NO_PADDING is a common choice for cleaner string storage
        return Base64.encodeToString(bytes, Base64.NO_WRAP | Base64.NO_PADDING);
    }

    /**
     * Reconstructs a Parcelable object from a byte array.
     * @param bytes Data to initialize the new object.
     * @param creator A Parcelable creator.
     * @return The new object.
     * @param <T> Class to create.
     */
    public static <T> T unmarshall(byte[] bytes, Parcelable.Creator<T> creator) {
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0); // CRITICAL: Rewind the Parcel to the start
        T result = creator.createFromParcel(parcel);
        parcel.recycle();
        return result;
    }

    @Nullable
    public static <T> T safeUnmarshall(String base64String, Parcelable.Creator<T> creator) {
        try {
            byte[] bytes = Base64.decode(base64String, Base64.NO_WRAP | Base64.NO_PADDING);
            return unmarshall(bytes, creator);
        } catch (Exception e) {
            // This can happen if the Parcel format is incompatible (e.g., after an OS update)
            Log.w("safeUnmarshall", "Invalid data supplied: " + base64String, e);
            return null;
        }
    }
}
