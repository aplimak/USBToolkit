package ir.aeliux.usbtoolkit.dto

import android.os.Parcelable
import ir.aeliux.usbtoolkit.data.UsbFunctions
import kotlinx.parcelize.Parcelize

abstract class UsbgFunctionAttrsBase(
    val type: UsbFunctions
)
