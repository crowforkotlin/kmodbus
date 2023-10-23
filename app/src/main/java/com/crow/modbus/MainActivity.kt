@file:Suppress("PrivatePropertyName", "SpellCheckingInspection", "LocalVariableName")
@file:OptIn(ExperimentalStdlibApi::class, ExperimentalStdlibApi::class, ExperimentalStdlibApi::class)

package com.crow.modbus

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.crow.modbus.comm.KModbusASCIIMaster
import com.crow.modbus.comm.KModbusRtuMaster
import com.crow.modbus.comm.KModbusTCPMaster
import com.crow.modbus.comm.model.ModbusEndian
import com.crow.modbus.comm.model.ModbusFunction
import com.crow.modbus.ext.logger
import com.crow.modbus.serialport.SerialPortManager
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.concurrent.timer
import kotlin.system.measureTimeMillis

class MainActivity : AppCompatActivity() {

    private val mSerialPort = SerialPortManager()

    private val IO = CoroutineScope(Dispatchers.IO)

    private val kModbusRtuMaster = KModbusRtuMaster.getInstance()

    private val kModbusASCIIMaster = KModbusASCIIMaster.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mSerialPort.openSerialPort("/dev/ttyS0", 9600)

        mSerialPort.readBytes { logger(it.map { byte -> byte.toHexString() }) }

        var revValues = false
        timer(period = 1000) {
            openOutput((if(revValues) intArrayOf(0,0,0,0,0,0,0,0,0) else intArrayOf(1,1,1,1,1,1,1,1,1)).also { revValues = !revValues })
        }
    }

    private fun openOutput(values: IntArray) {
        // BIG : 01, 0f, 00, 00, 00, 09, 02, ff, 01, 65, 4c
        // LITTLE_LITTLE : c4, 56, 10, ff, 20, 90, 00, 00, 00, f0, 10
        mSerialPort.writeBytes(kModbusRtuMaster.build(ModbusFunction.READ_COILS,1, 0, 1, values = values, endian = ModbusEndian.ARRAY_BIG_BYTE_BIG))
//        mSerialPort.writeBytes(kModbusRtuMaster.build(ModbusFunction.WRITE_SINGLE_COIL,1, 0, 1, value = 1, endian = ModbusEndian.ARRAY_BIG_BYTE_BIG))
//        mSerialPort.writeBytes(kModbusRtuMaster.build(ModbusFunction.WRITE_COILS,1, 0, 9, values = values, endian = ModbusEndian.ARRAY_BIG_BYTE_BIG))
//        mSerialPort.writeBytes(kModbusASCIIMaster.build(ModbusFunction.WRITE_COILS,1, 0, 9, value = 1, values = values))
    }
}

suspend fun main() {
    val packet = KModbusRtuMaster.getInstance().build(ModbusFunction.READ_HOLDING_REGISTERS,1, 0, 1, endian = ModbusEndian.ARRAY_BIG_BYTE_LITTLE)
    println(packet.map { it.toHexString() })
//    val data = byteArrayOf(0x01, 0x83.toByte(), 0x02, 0xC0.toByte(), 0xF1.toByte())
    val data = byteArrayOf(
        0x01, 0x03, 0x12, 0x00, 0x04, 0x00, 0x2C, 0x00,
        0x42, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x65, 0xDC.toByte()
    )
    println(KModbusRtuMaster.getInstance().resolve(data, ModbusEndian.ARRAY_BIG_BYTE_BIG).toIntData(2))
    return
    onTcpModbusPoll().join()
}

private suspend fun onTcpModbusPoll(): Job {
    fun logger(message: Any?) = println(message)
    val IO = CoroutineScope(Dispatchers.IO)
    val values = intArrayOf(1, 1, 1, 1, 1, 1, 1, 1)
//    val values = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
    val data = KModbusTCPMaster.getInstance().build(ModbusFunction.WRITE_COILS, 1,1, 8, value = 1, values = values)
    logger(data.map { it.toHexString() })
    val socket: Socket = runCatching { aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().connect("192.168.1.100", 502) }
            .onFailure { logger("连接失败！") }
            .onSuccess { logger("连接成功！") }
            .getOrThrow()
    val input = socket.openReadChannel()
    val output = socket.openWriteChannel(autoFlush = true)
    val job = IO.launch {
        while (true) {
            val buffer = ByteArray(2048)
            val size = input.readAvailable(buffer)
            logger(buffer.take(size).map { it.toHexString() })
            delay(1000L)
        }
    }
    IO.launch {
        output.writeFully(data)
        logger("发送成功")
    }
    return job
}