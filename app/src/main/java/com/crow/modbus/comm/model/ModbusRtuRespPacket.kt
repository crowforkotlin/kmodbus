package com.crow.modbus.comm.model


data class ModbusRtuRespPacket(
    val mSlave: Int,
    val mFunctionCode: Int,
    val mBytesCount: Int,
    val mValues: List<Int>,
) {

    fun toFloatData() {

    }

    fun toIntData(intBitLength: Int = 2): List<Int> {
        return runCatching { when (intBitLength) {
                1 -> mValues
                2 -> {
                    println(mValues)
                    val chunkedValues = mValues.chunked(2)
                    val data = ArrayList<Int>(chunkedValues.size)
                    for (ints in chunkedValues) { data.add(((ints[0] and 0xFF) shl 8) or (ints[1] and 0xFF)) }
                    data
                }
                4 -> {
                    val chunkedValues = mValues.chunked(4)
                    println(chunkedValues)
                    val data = ArrayList<Int>(chunkedValues.size)
                    for (ints in chunkedValues) {
                        data.add(((ints[0] and 0xFF) shl 24) or
                                ((ints[1] and 0xFF) shl 16) or
                                ((ints[2] and 0xFF) shl 8) or
                                (ints[3] and 0xFF))
                    }
                    data
                }
                else -> error("不合法的整形长度参数！")
            } }
            .getOrElse { listOf() }
    }
}