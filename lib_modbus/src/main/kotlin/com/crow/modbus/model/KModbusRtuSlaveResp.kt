package com.crow.modbus.model

import com.crow.modbus.tools.BytesOutput
import com.crow.modbus.tools.CRC16
import com.crow.modbus.tools.fromInt16

data class KModbusRtuSlaveResp(
    val mSlaveID: Int,
    val mFunction: KModbusFunction,
    val mAddress: Int,
    val mCount: Int? = null,
    val mByteCount: Int? = null,
    val mValues: ByteArray?,
    val mCrc: Int
) {

    /**
     * ⦁  校验CRC 是否正确， true：正确    false：失败
     *
     * ⦁  2024-01-22 14:25:03 周一 下午
     * @author crowforkotlin
     */
    @OptIn(ExperimentalStdlibApi::class)
    fun verifyCRC16(): Boolean {
        val address = fromInt16(mAddress)
        val count = mCount?.let { fromInt16(it) }
        val bytes = ByteArray(4 + (count?.size ?: 0) + (mValues?.size ?: 0))
        bytes[0] = mSlaveID.toByte()
        bytes[1] = mFunction.mCode.toByte()
        bytes[2] = address[0]
        bytes[3] = address[1]
        count?.let {
            bytes[4] = it[0]
            bytes[5] = it[1]
        }
        if (mValues != null) {
            System.arraycopy(mValues, 0, bytes, 4 + (count?.size ?: 0), mValues.size)
        }
        return CRC16.compute(bytes) == mCrc
    }

    fun buildResponse(skipValue: Boolean = false, reComputeCrc16: Boolean = false): ByteArray {
        val output = BytesOutput()
        output.writeInt8(mSlaveID)
        output.writeInt8(mFunction.mCode)
        output.writeInt16(mAddress)
        if (mCount != null) { output.writeInt16(mCount) }
        if (mValues != null && !skipValue) {
            output.write(mValues)
        }
        if (reComputeCrc16) { output.writeInt16Reversal(CRC16.compute(output.toByteArray())) }
        else { output.writeInt16Reversal(mCrc) }
        return output.toByteArray()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KModbusRtuSlaveResp

        if (mSlaveID != other.mSlaveID) return false
        if (mFunction != other.mFunction) return false
        if (mAddress != other.mAddress) return false
        if (mCount != other.mCount) return false
        if (mByteCount != other.mByteCount) return false
        if (mValues != null) {
            if (other.mValues == null) return false
            if (!mValues.contentEquals(other.mValues)) return false
        } else if (other.mValues != null) return false
        if (mCrc != other.mCrc) return false

        return true
    }

    override fun hashCode(): Int {
        var result = mSlaveID
        result = 31 * result + mFunction.hashCode()
        result = 31 * result + mAddress
        result = 31 * result + (mCount ?: 0)
        result = 31 * result + (mByteCount ?: 0)
        result = 31 * result + (mValues?.contentHashCode() ?: 0)
        result = 31 * result + mCrc
        return result
    }
}