@file:Suppress("PrivatePropertyName", "SpellCheckingInspection", "LocalVariableName")
@file:OptIn(ExperimentalStdlibApi::class, ExperimentalStdlibApi::class, ExperimentalStdlibApi::class)

package com.crow.kmodbus

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.crow.modbus.KModbusASCIIMaster
import com.crow.modbus.KModbusRtu
import com.crow.modbus.KModbusRtuMaster
import com.crow.modbus.model.KModbusType
import com.crow.modbus.model.ModbusEndian
import com.crow.modbus.model.ModbusFunction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class MainActivity : AppCompatActivity() {

    private val mKModbusRTU = KModbusRtu()

    private val IO = CoroutineScope(Dispatchers.IO)

    private val kModbusRtuMaster = KModbusRtuMaster.getInstance()

    private val kModbusASCIIMaster = KModbusASCIIMaster.getInstance()

    @OptIn(ExperimentalStdlibApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mKModbusRTU.openSerialPort(0, 9600)
        mKModbusRTU.addOnReceiveDataListener() { "Read Bytes ${it.toHexString()}".info() }
        mKModbusRTU.setOnDataWriteReadyListener { null }
        mKModbusRTU.runRepeatWriteDataTask(1000)
        mKModbusRTU.runRepeatReceiveDataTask    (KModbusType.MASTER)

        /*var revValues = false
        timer(period = 1000) {
            openOutput((if(revValues) intArrayOf(0,0,0,0,0,0,0,0,0) else intArrayOf(1,1,1,1,1,1,1,1,1)).also { revValues = !revValues })
        }*/
    }

    private fun openOutput(values: IntArray) {
        // BIG : 01, 0f, 00, 00, 00, 09, 02, ff, 01, 65, 4c
        // LITTLE_LITTLE : c4, 56, 10, ff, 20, 90, 00, 00, 00, f0, 10
        mKModbusRTU.writeData(kModbusRtuMaster.build(ModbusFunction.WRITE_COILS,1, 0, 9, values = values, endian = ModbusEndian.ARRAY_BIG_BYTE_LITTLE))
//        mSerialPort.writeBytes(kModbusASCIIMaster.build(ModbusFunction.WRITE_COILS,1, 0, 9, value = 1, values = values))
    }
}