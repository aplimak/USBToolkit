package ir.aeliux.usbtoolkit.usb.dto.function

import android.os.Parcelable
import ir.aeliux.usbtoolkit.usb.UsbFunctions
import kotlinx.parcelize.Parcelize

@Parcelize
data class UsbgMassStorageAttrs(
    var stall: Boolean,
    var luns: List<UsbgMassStorageLun>
) : UsbgFunctionAttrsBase(UsbFunctions.USBG_F_MASS_STORAGE),Parcelable
