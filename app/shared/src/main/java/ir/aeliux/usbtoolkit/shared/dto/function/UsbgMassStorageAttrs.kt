package ir.aeliux.usbtoolkit.shared.dto.function

import android.os.Parcelable
import ir.aeliux.usbtoolkit.shared.UsbFunctions
import ir.aeliux.usbtoolkit.shared.dto.UsbgFunctionAttrsBase
import kotlinx.parcelize.Parcelize

@Parcelize
data class UsbgMassStorageAttrs(
    val stall: Boolean,
    val luns: List<UsbgMassStorageLun>
) : UsbgFunctionAttrsBase(UsbFunctions.USBG_F_MASS_STORAGE),Parcelable
