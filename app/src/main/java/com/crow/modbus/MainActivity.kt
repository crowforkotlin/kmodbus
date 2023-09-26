@file:Suppress("PrivatePropertyName", "SpellCheckingInspection", "LocalVariableName")
@file:OptIn(ExperimentalStdlibApi::class, ExperimentalStdlibApi::class)

package com.crow.modbus

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.crow.modbus.serialport.SerialPortManager
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.isClosed
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.writeAvailable
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.concurrent.timer

class MainActivity : AppCompatActivity() {


    private val mSerialPort = SerialPortManager()

    private val IO = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mSerialPort.openSerialPort("/dev/ttyS0", 9600)

        timer(period = 1000L) {
//             mSerialPort.writeBytes(byteArrayOf(0x01, 0x06, 0x00, 0x00, 0x00, 0x01, 0x48, 0x0A))
             mSerialPort.writeBytes(byteArrayOf(0x01, 0x06, 0x00, 0x00, 0x00, 0x01, 0x48, 0x0A))
        }

        mSerialPort.readBytes { bytes -> logger("ReadBytes ${bytes.map { it.toHexString() }}") }

        // IO.launch(CoroutineExceptionHandler { _, _ -> logger("发生异常") }) { onTcpModbusPoll() }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private suspend fun onTcpModbusPoll() {
        val socket: Socket =
            runCatching {
                aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().connect("192.168.1.100", 502)
            }
                .onFailure { logger("连接失败！") }
                .onSuccess { logger("连接成功！") }
                .getOrThrow()
        val input = socket.openReadChannel()
        val output = socket.openWriteChannel()
        IO.launch {
            while (true) {
                val buffer = ByteArray(2048)
                input.readAvailable(buffer)
                if (socket.isClosed) return@launch
                logger(buffer.map { it.toHexString() })
                delay(1000L)
            }
        }
        IO.launch {
            output.writeAvailable(byteArrayOf(0x01, 0x06, 0x00, 0x00, 0x00, 0x01, 0x48, 0x0A))
            logger("发送成功")
        }
    }
}

suspend fun main() {
    val byte = byteArrayOf(0,1)
    val data = ByteArray(1)
    System.arraycopy(byte, 0 , data, 0, 2)
    println(data.toHexString())
    //    onTcpModbusPoll()
    delay(100000)
}

private suspend fun onTcpModbusPoll() {

    fun logger(message: Any?) = println(message)

    val IO = CoroutineScope(Dispatchers.IO)
    val socket: Socket =
        runCatching {
            aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().connect("192.168.1.100", 502)
        }
            .onFailure { logger("连接失败！") }
            .onSuccess { logger("连接成功！") }
            .getOrThrow()
    val input = socket.openReadChannel()
    val output = socket.openWriteChannel(autoFlush = true)
    IO.launch {
        while (true) {
            val buffer = ByteArray(2048)
            input.readAvailable(buffer)
            logger(buffer.map { it.toHexString() })
            delay(1000L)
        }
    }
    IO.launch {
        output.writeFully(byteArrayOf(0x00 ,0x31 ,0x00  ,0x00  ,0x00  ,0x06  ,0x01  ,0x05  ,0x00  ,0x00  ,0xFF.toByte()  ,0x00))
        logger("发送成功")
    }
}