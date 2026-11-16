package ir.aeliux.magic

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Objects

@Parcelize
data class MagicResult(
    val mimeType: String?,
    val error: String?
) : Parcelable {
    val isOk: Boolean
        get() = error == null
}
