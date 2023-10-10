@file:Suppress("PrivatePropertyName", "SpellCheckingInspection", "LocalVariableName")
@file:OptIn(ExperimentalStdlibApi::class, ExperimentalStdlibApi::class,
    ExperimentalStdlibApi::class
)

package com.crow.modbus

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.crow.modbus.serialport.KModbusASCIIMaster
import com.crow.modbus.serialport.KModbusRtuMaster
import com.crow.modbus.serialport.KModbusTCPMaster
import com.crow.modbus.serialport.ModbusFunction
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

class MainActivity : AppCompatActivity() {

    private val mSerialPort = SerialPortManager()

    private val IO = CoroutineScope(Dispatchers.IO)

    private val kModbusRtuMaster = KModbusRtuMaster.getInstance()

    private val kModbusASCIIMaster = KModbusASCIIMaster.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mSerialPort.openSerialPort("/dev/ttyS0", 9600)

        mSerialPort.readBytes { bytes -> logger("ReadBytes ${bytes.map { it.toHexString() }}") }


        var revValues = false
        timer(period = 1000) {
            openOutput((if(revValues) intArrayOf(0,0,0,0,0,0,0,0,0) else intArrayOf(1,1,1,1,1,1,1,1,1)).also { revValues = !revValues })
        }
    }

    private fun openOutput(values: IntArray) {
//        mSerialPort.writeBytes(kModbusRtuMaster.build(ModbusFunction.WRITE_COILS,1, 0, 9, values = values))
        mSerialPort.writeBytes(kModbusASCIIMaster.build(ModbusFunction.WRITE_COILS,1, 0, 9, value = 1, values = values))
    }
}

suspend fun main() { onTcpModbusPoll().join() }

private suspend fun onTcpModbusPoll(): Job {
    fun logger(message: Any?) = println(message)
    val IO = CoroutineScope(Dispatchers.IO)
    val data = KModbusASCIIMaster.getInstance().build(ModbusFunction.WRITE_SINGLE_COIL, 1,1, 1, value = 1)
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