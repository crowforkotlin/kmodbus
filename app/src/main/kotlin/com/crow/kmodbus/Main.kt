package com.crow.kmodbus
import com.crow.modbus.KModbusRtu
import com.crow.modbus.KModbusTcpClient
import com.crow.modbus.model.KModbusFunction
import com.crow.modbus.model.KModbusType
import com.crow.modbus.tools.BytesOutput
import com.crow.modbus.tools.asciiHexToByte
import com.crow.modbus.tools.splitInt8ToBits
import com.crow.modbus.tools.toAsciiHexByte
import com.crow.modbus.tools.toAsciiHexBytes
import com.crow.modbus.tools.toHexList
import com.crow.modbus.tools.toInt16
import com.crow.modbus.tools.toIntArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.experimental.and
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

private fun monitorTcp(slaveAddr: Int, startAddr: Int, regCount: Int, valueSize: Int, count: Short, isSingle: Boolean =false    ) {
    KModbusTcpClient().apply {
        var _count: Short = count
        val array = IntArray(valueSize)
        tcpClient(KModbusTcpClient.ClientInfo("192.168.1.100", 8080) { ins, ops, clientInfo, job ->
            val readJob = continuouslyReadData(clientInfo, job, ins, KModbusType.MASTER) {
                it.mValues.toHexList().info()
            }
            val writeJob = continuouslyWriteData(clientInfo, job, ops, 200L, 1000L) {
                val countInt = _count.toInt()
                repeat(valueSize) { array[it] = countInt }
                _count++
                if (isSingle) {
                    listOf(buildMasterOutput(KModbusFunction.WRITE_MULTIPLE_REGISTERS, slaveAddr  , startAddr, regCount, values = array))
                } else {
                    listOf(buildMasterOutput(KModbusFunction.WRITE_MULTIPLE_REGISTERS, slaveAddr  , startAddr, regCount, values = array))
                }
            }
            listOf(readJob, writeJob)
        })
    }
}

suspend fun main() = runBlocking {
    monitorTcp(1, 0, 4, 4, 0)
    monitorTcp(2, 0, 3, 3, 400)
    coroutineContext.job.join()
    println(toInt16(byteArrayOf(2, 37), 0).toLong())
    println((0xff and 0xFF).toInt())
    println(byteArrayOf(0xff.toByte()).first().toLong())
    println(
        KModbusRtu().buildMasterOutput(
            function = KModbusFunction.WRITE_SINGLE_REGISTER,
            slaveAddress = 6,
            startAddress = 0,
            count = 2,
            value = 0
        ).toHexList()
    )
    println(toAsciiHexBytes(byteArrayOf(0x01, 0x03, 0x00, 0x00, 0x00, 0x01)).toHexList())
    val byteValue= byteArrayOf(0x01.toByte())
    println(toAsciiHexBytes(byteValue).toHexList())
    val function = asciiHexToByte(0x38, 0x33)
    println((function.toInt() and 0xFF))
    println((function.toInt() and 0xF) + 0x80)
    println((function.toInt() and 0xF) + 0x80 == function.toInt())
}

