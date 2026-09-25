package ir.aeliux.usbtoolkit.dto

import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Parcelize
data class UsbgConfigAttrs(
    var bmAttributes: Byte,
    var bMaxPower: Byte
) : Parcelable
