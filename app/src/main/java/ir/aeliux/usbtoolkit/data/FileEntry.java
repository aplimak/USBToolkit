package ir.aeliux.usbtoolkit.data;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Objects;

public class FileEntry implements Parcelable {

    private final String name;
    private final String absolutePath;
    private final boolean isDirectory;
    private final long size;
    private final long lastModified;

    public FileEntry(String name, String absolutePath, boolean isDirectory,
                     long size, long lastModified) {
        this.name = name;
        this.absolutePath = absolutePath;
        this.isDirectory = isDirectory;
        this.size = size;
        this.lastModified = lastModified;
    }

    protected FileEntry(Parcel in) {
        name = in.readString();
        absolutePath = in.readString();
        isDirectory = in.readInt() == 1;
        size = in.readLong();
        lastModified = in.readLong();
    }

    public static final Creator<FileEntry> CREATOR = new Creator<FileEntry>() {
        @Override
        public FileEntry createFromParcel(Parcel in) {
            return new FileEntry(in);
        }

        @Override
        public FileEntry[] newArray(int size) {
            return new FileEntry[size];
        }
    };

    public String getName() { return name; }
    public String getAbsolutePath() { return absolutePath; }
    public boolean isDirectory() { return isDirectory; }
    public long getSize() { return size; }
    public long getLastModified() { return lastModified; }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(absolutePath);
        dest.writeInt(isDirectory ? 1 : 0);
        dest.writeLong(size);
        dest.writeLong(lastModified);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileEntry)) return false;
        FileEntry other = (FileEntry) o;
        return isDirectory == other.isDirectory
                && size == other.size
                && lastModified == other.lastModified
                && Objects.equals(name, other.name)
                && Objects.equals(absolutePath, other.absolutePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, absolutePath, isDirectory, size, lastModified);
    }
}
