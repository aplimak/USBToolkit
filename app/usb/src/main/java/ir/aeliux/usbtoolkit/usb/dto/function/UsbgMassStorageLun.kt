package ir.aeliux.usbtoolkit.usb.dto.function

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UsbgMassStorageLun(
    var id: Int,
    var cdrom: Boolean,
    var ro: Boolean,
    var nofua: Boolean,
    var removable: Boolean,
    var file: String,
    var inquiryString: String
) : Parcelable
