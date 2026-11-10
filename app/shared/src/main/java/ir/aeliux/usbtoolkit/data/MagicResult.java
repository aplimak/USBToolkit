package ir.aeliux.usbtoolkit.data;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Objects;

public final class MagicResult implements Parcelable {
    public final String mimeType;
    public final String error;      // null on success

    public MagicResult(String mimeType, String error) {
        this.mimeType    = mimeType;
        this.error       = error;
    }

    protected MagicResult(Parcel in) {
        this.mimeType    = in.readString();
        this.error       = in.readString();
    }

    public boolean isOk() { return error == null; }

    public static final Creator<MagicResult> CREATOR = new Creator<MagicResult>() {
        @Override
        public MagicResult createFromParcel(Parcel source) {
            return new MagicResult(source);
        }

        @Override
        public MagicResult[] newArray(int size) {
            return new MagicResult[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(mimeType);
        dest.writeString(error);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MagicResult)) return false;
        MagicResult that = (MagicResult) o;
        return Objects.equals(mimeType, that.mimeType) && Objects.equals(error, that.error);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mimeType, error);
    }
}
