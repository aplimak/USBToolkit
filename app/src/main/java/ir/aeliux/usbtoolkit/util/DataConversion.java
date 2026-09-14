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
}
