package ir.aeliux.usbtoolkit.util;

public class DataConversion {
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
     *
     * The BCD encoding packs two decimal digits per byte:
     *   high byte = major version (integer part)
     *   low byte  = minor version (fractional part, two decimal digits)
     *
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

        return String.format("%d.%02d", major, minor);
    }

    /**
     * Converts a single byte in BCD format to its decimal integer value.
     * Each nibble represents a decimal digit.
     *
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
}
