package com.crow.modbus.model

data class KModbusTcpMasterResp(
    val mSlaveID: Int,
    val mFunction: Int,
    val mByteCount: Int,
    val mValues: ByteArray,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as KModbusTcpMasterResp
        if (mSlaveID != other.mSlaveID) return false
        if (mFunction != other.mFunction) return false
        if (mByteCount != other.mByteCount) return false
        if (!mValues.contentEquals(other.mValues)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = mSlaveID
        result = 31 * result + mFunction
        result = 31 * result + mByteCount
        result = 31 * result + mValues.contentHashCode()
        return result
    }
}