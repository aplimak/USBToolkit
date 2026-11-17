package ir.aeliux.usbtoolkit.usb.dto

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UsbgConfigStrs(
    var configuration: String?
) : Parcelable
