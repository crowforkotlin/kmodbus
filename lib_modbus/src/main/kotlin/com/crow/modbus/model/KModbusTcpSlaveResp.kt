@file:Suppress("ArrayInDataClass")

package com.crow.modbus.model

import com.crow.modbus.tools.BytesOutput
import com.crow.modbus.tools.CRC16
import com.crow.modbus.tools.fromInt16

data class KModbusTcpSlaveResp(
    val mMbap: ByteArray,
    val mSlaveID: Int,
    val mFunction: KModbusFunction,
    val mAddress: Int? = null,
    val mCount: Int? = null,
    val mByteCount: Int? = null,
    val mValues: ByteArray?
) {

    fun buildResponse(skipValue: Boolean = false): ByteArray {
        val output = BytesOutput()
        output.writeBytes(mMbap, mMbap.size)
        output.writeInt8(mSlaveID)
        output.writeInt8(mFunction.mCode)
        if (mAddress != null) { output.writeInt16(mAddress) }
        if (mCount != null) { output.writeInt16(mCount) }
        if (mValues != null && !skipValue) { output.write(mValues) }
        return output.toByteArray()
    }
}