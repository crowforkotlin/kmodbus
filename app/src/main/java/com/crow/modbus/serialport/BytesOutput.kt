package com.crow.modbus.serialport

import com.crow.modbus.ext.Bytes
import com.crow.modbus.ext.fromInt16
import com.crow.modbus.ext.fromInt16LittleEndian
import com.crow.modbus.ext.fromInt32
import com.crow.modbus.ext.fromInt8
import java.io.ByteArrayOutputStream

class BytesOutput : ByteArrayOutputStream() {

    fun writeInt8(byte: Byte) {
        this.write(byte.toInt())
    }

    fun writeInt8(int32: Int) {
        this.write(fromInt8(int32))
    }

    fun writeInt16(int32: Int) {
        val bytes: Bytes = fromInt16(int32)
        this.write(bytes, 0, bytes.size)
    }

    fun writeInt16Reversal(int32: Int) {
        val bytes: Bytes = fromInt16LittleEndian(int32)
        this.write(bytes, 0, bytes.size)
    }

    fun writeInt32(int32: Int) {
        val bytes: ByteArray = fromInt32(int32)
        this.write(bytes, 0, bytes.size)
    }

    fun writeBytes(bytes: Bytes, len: Int) {
        this.write(bytes, 0, len)
    }
}
