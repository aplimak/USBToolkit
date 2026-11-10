package ir.aeliux.usbtoolkit.shared.dto

data class UsbgGadgetAttrs(
    var bcdUSB: Short,
    var bDeviceClass: Byte,
    var bDeviceSubClass: Byte,
    var bDeviceProtocol: Byte,
    var bMaxPacketSize0: Byte,
    var idVendor: Short,
    var idProduct: Short,
    var bcdDevice: Short
)
