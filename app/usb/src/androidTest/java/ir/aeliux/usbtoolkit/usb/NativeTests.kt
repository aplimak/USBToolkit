package ir.aeliux.usbtoolkit.usb

import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.aeliux.usbtoolkit.usb.dto.UsbgConfigAttrs
import ir.aeliux.usbtoolkit.usb.dto.UsbgConfigStrs
import ir.aeliux.usbtoolkit.usb.dto.UsbgGadgetAttrs
import ir.aeliux.usbtoolkit.usb.dto.UsbgGadgetStrs
import ir.aeliux.usbtoolkit.usb.dto.function.UsbgMassStorageAttrs
import ir.aeliux.usbtoolkit.usb.dto.function.UsbgMassStorageLun
import org.junit.Assert
import org.junit.Test
import org.junit.function.ThrowingRunnable
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeTests {
    @Test
    fun dtoUsbgGadgetAttrsTest() {
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

        val processedGadgetAttrs = processUsbgGadgetAttrs(sampleGadgetAttrs)

        Assert.assertThrows(IllegalArgumentException::class.java) {
            processUsbgGadgetAttrs(null)
        }
        Assert.assertNotSame(sampleGadgetAttrs, processedGadgetAttrs)
        Assert.assertEquals(sampleGadgetAttrs, processedGadgetAttrs)
    }

    @Test
    fun dtoUsbgGadgetStrsTest() {
        val sampleGadgetStrs = UsbgGadgetStrs(
            manufacturer = "test manufacturer",
            product = "test product",
            serial = "test serial"
        )
        val processedGadgetStrs = processUsbgGadgetStrs(sampleGadgetStrs)

        Assert.assertThrows(IllegalArgumentException::class.java) {
            processUsbgGadgetStrs(null)
        }
        Assert.assertNotSame(sampleGadgetStrs, processedGadgetStrs)
        Assert.assertEquals(sampleGadgetStrs, processedGadgetStrs)
    }

    @Test
    fun dtoUsbgConfigAttrsTest() {
        val sampleConfigAttrs = UsbgConfigAttrs(
            bmAttributes = 0b11111,
            bMaxPower = 0b1001
        )
        val processedConfigAttrs = processUsbgConfigAttrs(sampleConfigAttrs);

        Assert.assertThrows(IllegalArgumentException::class.java) {
            processUsbgConfigAttrs(null)
        }
        Assert.assertNotSame(sampleConfigAttrs, processedConfigAttrs)
        Assert.assertEquals(sampleConfigAttrs, processedConfigAttrs)
    }

    @Test
    fun dtoUsbgConfigStrsTest() {
        val sampleConfigStrs = UsbgConfigStrs(
            configuration = "test configuration"
        )
        val processedConfigStrs = processUsbgConfigStrs(sampleConfigStrs);

        Assert.assertThrows(IllegalArgumentException::class.java) {
            processUsbgConfigStrs(null)
        }
        Assert.assertNotSame(sampleConfigStrs, processedConfigStrs)
        Assert.assertEquals(sampleConfigStrs, processedConfigStrs)
    }

    @Test
    fun dtoUsbgMassStorageAttrsTest() {
        val sampleMassStorageAttrs = UsbgMassStorageAttrs(
            stall = true,
            luns = listOf(
                UsbgMassStorageLun(
                    id = 7,
                    cdrom = true,
                    ro = true,
                    nofua = false,
                    removable = true,
                    file = "test file",
                    inquiryString = "test"
                ),
                UsbgMassStorageLun(
                    id = 2,
                    cdrom = false,
                    ro = false,
                    nofua = true,
                    removable = false,
                    file = "test file 2",
                    inquiryString = "test2"
                )
            )
        )
        val processedMassStorageAttrs = processUsbgMassStorageAttrs(sampleMassStorageAttrs);

        Assert.assertThrows(IllegalArgumentException::class.java) {
            processUsbgMassStorageAttrs(null)
        }
        Assert.assertNotSame(sampleMassStorageAttrs, processedMassStorageAttrs)
        Assert.assertEquals(sampleMassStorageAttrs, processedMassStorageAttrs)
    }

    companion object {
        init {
            System.loadLibrary("usbtktests")
        }

        @JvmStatic
        private external fun processUsbgGadgetAttrs(gadgetAttrs: UsbgGadgetAttrs?): UsbgGadgetAttrs?

        @JvmStatic
        private external fun processUsbgGadgetStrs(gadgetStrs: UsbgGadgetStrs?): UsbgGadgetStrs?

        @JvmStatic
        private external fun processUsbgConfigAttrs(configAttrs: UsbgConfigAttrs?): UsbgConfigAttrs?

        @JvmStatic
        private external fun processUsbgConfigStrs(configStrs: UsbgConfigStrs?): UsbgConfigStrs?

        @JvmStatic
        private external fun processUsbgMassStorageAttrs(massStorageAttrs: UsbgMassStorageAttrs?): UsbgMassStorageAttrs?
    }
}
