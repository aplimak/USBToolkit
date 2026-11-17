package ir.aeliux.usbtoolkit.usb.dto

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UsbgGadgetStrs(
    var manufacturer: String?,
    var product: String?,
    var serial: String?
) : Parcelable
