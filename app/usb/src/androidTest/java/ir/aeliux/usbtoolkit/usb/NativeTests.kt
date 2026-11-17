package ir.aeliux.usbtoolkit.usb

import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.aeliux.usbtoolkit.usb.dto.UsbgGadgetAttrs
import org.junit.Assert
import org.junit.Test
import org.junit.function.ThrowingRunnable
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeTests {
    @Test
    fun dtoTest() {
        val sampleGadgetAttrs = UsbgGadgetAttrs(
            bcdUSB = 0x2000.toShort(),
            bDeviceClass = 2.toByte(),
            bDeviceSubClass = 5.toByte(),
            bDeviceProtocol = 86.toByte(),
            bMaxPacketSize0 = 1.toByte(),
            idVendor = 74.toShort(),
            idProduct = 9.toShort(),
            bcdDevice = 7.toShort()
        )

        val processedGadgetAttrs = processUsbgGadgetAttrs(sampleGadgetAttrs);

        Assert.assertThrows(IllegalArgumentException::class.java) {
            processUsbgGadgetAttrs(null)
        }

        Assert.assertNotSame(sampleGadgetAttrs, processedGadgetAttrs);
        Assert.assertEquals(sampleGadgetAttrs, processedGadgetAttrs);
    }

    companion object {
        init {
            System.loadLibrary("usbtk")
        }

        @JvmStatic
        private external fun processUsbgGadgetAttrs(gadgetAttrs: UsbgGadgetAttrs?): UsbgGadgetAttrs?
    }
}
