package com.crow.modbus.model

import com.crow.modbus.tools.CRC16
import com.crow.modbus.tools.fromInt16
import com.crow.modbus.tools.info

data class KModbusRtuRespPacket(
    val mDeviceID: Int,
    val mFunctionCode: KModbusFunction,
    val mAddress: Int,
    val mCount: Int? = null,
    val mValues: ByteArray?,
    val mCrc: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as KModbusRtuRespPacket
        if (mDeviceID != other.mDeviceID) return false
        if (mFunctionCode != other.mFunctionCode) return false
        if (mAddress != other.mAddress) return false
        if (mCount != other.mCount) return false
        if (mValues != null) {
            if (other.mValues == null) return false
            if (!mValues.contentEquals(other.mValues)) return false
        } else if (other.mValues != null) return false
        if (mCrc != other.mCrc) return false
        return true
    }

    override fun hashCode(): Int {
        var result = mDeviceID
        result = 31 * result + mFunctionCode.hashCode()
        result = 31 * result + mAddress
        result = 31 * result + (mCount ?: 0)
        result = 31 * result + (mValues?.contentHashCode() ?: 0)
        result = 31 * result + mCrc
        return result
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun verifyCRC16(): Boolean {
        val address = fromInt16(mAddress)
        val count = mCount?.let { fromInt16(it) }
        val bytes = ByteArray(4 + (count?.size ?: 0) + (mValues?.size ?: 0))
        bytes[0] = mDeviceID.toByte()
        bytes[1] = mFunctionCode.mCode.toByte()
        bytes[2] = address[0]
        bytes[3] = address[1]
        count?.let {
            bytes[4] = it[0]
            bytes[5] = it[1]
        }
        if (mValues != null) {
            System.arraycopy(mValues, 0, bytes, 4 + (count?.size ?: 0), mValues.size)
        }
        bytes.map { it.toHexString() }.info()
        return CRC16.compute(bytes).also { it.info() } == mCrc.also { it.info() }
    }

}