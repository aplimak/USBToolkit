package ir.aeliux.usbtoolkit.usb.dto.function

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UsbgMassStorageLun(
    val id: Int,
    val cdrom: Boolean,
    val ro: Boolean,
    val nofua: Boolean,
    val removable: Boolean,
    val file: String,
    val inquiryString: String
) : Parcelable
