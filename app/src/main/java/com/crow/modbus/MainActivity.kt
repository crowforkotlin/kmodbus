@file:Suppress("PrivatePropertyName", "SpellCheckingInspection", "LocalVariableName")
@file:OptIn(ExperimentalStdlibApi::class, ExperimentalStdlibApi::class, ExperimentalStdlibApi::class)

package com.crow.modbus

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.crow.modbus.comm.KModbusASCIIMaster
import com.crow.modbus.comm.model.ModbusEndian
import com.crow.modbus.comm.model.ModbusFunction
import com.crow.modbus.ext.logger
import com.crow.modbus.serialport.SerialPortManager
import com.listen.x3player.kt.modbus.comm.KModbusRtuMaster
import com.listen.x3player.kt.modbus.comm.KModbusTCPMaster
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
        setContentView(com.crow.modbus.R.layout.activity_main)

        mSerialPort.openSerialPort("/dev/ttyS0", 9600)
        mSerialPort.readBytes { _, bytes -> logger("ReadBytes ${bytes.map { it.toHexString() }}") }


        var revValues = false
        timer(period = 1000) {
            openOutput((if(revValues) intArrayOf(0,0,0,0,0,0,0,0,0) else intArrayOf(1,1,1,1,1,1,1,1,1)).also { revValues = !revValues })
        }
    }

    private fun openOutput(values: IntArray) {
        // BIG : 01, 0f, 00, 00, 00, 09, 02, ff, 01, 65, 4c
        // LITTLE_LITTLE : c4, 56, 10, ff, 20, 90, 00, 00, 00, f0, 10
        mSerialPort.writeBytes(kModbusRtuMaster.build(ModbusFunction.WRITE_COILS,1, 0, 9, values = values, endian = ModbusEndian.ARRAY_BIG_BYTE_LITTLE))
//        mSerialPort.writeBytes(kModbusASCIIMaster.build(ModbusFunction.WRITE_COILS,1, 0, 9, value = 1, values = values))
    }
}