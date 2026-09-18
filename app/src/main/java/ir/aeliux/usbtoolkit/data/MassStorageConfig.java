package ir.aeliux.usbtoolkit.data;

import java.io.File;
import java.util.Objects;

import android.os.Parcel;
import android.os.Parcelable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable configuration for a mass storage gadget setup.
 * Use the {@link Builder} to create an instance.
 */
public class MassStorageConfig implements Parcelable {

    public final List<Path> imagePaths;
    public final boolean cdrom;
    public final boolean readOnly;
    public final boolean removable;
    public final String udc;

    private MassStorageConfig(Builder builder) {
        this.imagePaths = List.copyOf(builder.imagePaths);
        this.cdrom       = builder.cdrom;
        this.readOnly    = builder.readOnly;
        this.removable   = builder.removable;
        this.udc         = builder.udc;
    }

    protected MassStorageConfig(Parcel in) {
        int n = in.readInt();
        List<Path> paths = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            String s = in.readString();
            paths.add(s == null ? null : Paths.get(s));
        }
        this.imagePaths = Collections.unmodifiableList(paths);
        this.cdrom     = in.readByte() != 0;
        this.readOnly  = in.readByte() != 0;
        this.removable = in.readByte() != 0;
        this.udc       = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(imagePaths.size());
        for (Path p : imagePaths) {
            dest.writeString(p == null ? null : p.toString());
        }
        dest.writeByte((byte) (cdrom     ? 1 : 0));
        dest.writeByte((byte) (readOnly  ? 1 : 0));
        dest.writeByte((byte) (removable ? 1 : 0));
        dest.writeString(udc);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<MassStorageConfig> CREATOR = new Creator<>() {
        @Override
        public MassStorageConfig createFromParcel(Parcel in) {
            return new MassStorageConfig(in);
        }

        @Override
        public MassStorageConfig[] newArray(int size) {
            return new MassStorageConfig[size];
        }
    };

    public static class Builder {
        private final List<Path> imagePaths = new ArrayList<>();
        private boolean cdrom = false;
        private boolean readOnly = false;
        private boolean removable = true;
        private String udc;

        /**
         * Adds an image file (will be resolved to an absolute path during setup).
         */
        public Builder addImage(String image) {
            return addImage(new File(image).toPath());
        }

        /**
         * Adds an image file (will be resolved to an absolute path during setup).
         */
        public Builder addImage(Path image) {
            imagePaths.add(Objects.requireNonNull(image, "image path cannot be null"));
            return this;
        }

        public Builder setCdrom(boolean cdrom) {
            this.cdrom = cdrom;
            return this;
        }

        public Builder setReadOnly(boolean readOnly) {
            this.readOnly = readOnly;
            return this;
        }

        public Builder setRemovable(boolean removable) {
            this.removable = removable;
            return this;
        }

        public Builder setUdc(String udc) {
            this.udc = udc;
            return this;
        }

        public MassStorageConfig build() {
            if (imagePaths.isEmpty()) {
                throw new IllegalArgumentException("At least one image path is required");
            }
            if (udc == null || udc.isEmpty()) {
                throw new IllegalArgumentException("UDC is not set");
            }
            return new MassStorageConfig(this);
        }
    }
}
