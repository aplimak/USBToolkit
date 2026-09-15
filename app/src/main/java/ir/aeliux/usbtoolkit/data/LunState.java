package ir.aeliux.usbtoolkit.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Represents the state of a single LUN (Logical Unit Number).
 */
public class LunState implements Parcelable {

    public final int lunNumber;
    public final Path file;
    public final boolean cdrom;
    public final boolean readOnly;
    public final boolean removable;

    public LunState(int lunNumber, Path file, boolean cdrom,
                    boolean readOnly, boolean removable) {
        this.lunNumber = lunNumber;
        this.file = file;
        this.cdrom = cdrom;
        this.readOnly = readOnly;
        this.removable = removable;
    }

    protected LunState(Parcel in) {
        lunNumber = in.readInt();
        String fileStr = in.readString();
        file = (fileStr == null) ? null : Paths.get(fileStr);
        cdrom     = in.readByte() != 0;
        readOnly  = in.readByte() != 0;
        removable = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(lunNumber);
        dest.writeString(file == null ? null : file.toString());
        dest.writeByte((byte) (cdrom     ? 1 : 0));
        dest.writeByte((byte) (readOnly  ? 1 : 0));
        dest.writeByte((byte) (removable ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<LunState> CREATOR = new Creator<>() {
        @Override
        public LunState createFromParcel(Parcel in) {
            return new LunState(in);
        }

        @Override
        public LunState[] newArray(int size) {
            return new LunState[size];
        }
    };
}