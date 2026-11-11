package ir.aeliux.usbtoolkit.usb.dto

import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Parcelize
data class UsbgConfigAttrs(
    var bmAttributes: Byte,
    var bMaxPower: Byte
) : Parcelable
