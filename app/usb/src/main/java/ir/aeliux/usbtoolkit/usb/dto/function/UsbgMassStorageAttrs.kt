package ir.aeliux.usbtoolkit.usb.dto.function

import android.os.Parcelable
import ir.aeliux.usbtoolkit.usb.UsbFunctions
import ir.aeliux.usbtoolkit.usb.dto.UsbgFunctionAttrsBase
import kotlinx.parcelize.Parcelize

@Parcelize
data class UsbgMassStorageAttrs(
    val stall: Boolean,
    val luns: List<UsbgMassStorageLun>
) : UsbgFunctionAttrsBase(UsbFunctions.USBG_F_MASS_STORAGE),Parcelable
