@file:Suppress("PrivatePropertyName", "SpellCheckingInspection", "LocalVariableName",
    "SameParameterValue"
)
@file:OptIn(ExperimentalStdlibApi::class, ExperimentalStdlibApi::class, ExperimentalStdlibApi::class)

package com.crow.kmodbus

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.crow.modbus.KModbusASCII
import com.crow.modbus.KModbusRtu
import com.crow.modbus.KModbusTcp
import com.crow.modbus.model.KModbusFunction.READ_HOLDING_REGISTERS
import com.crow.modbus.model.KModbusType
import com.crow.modbus.model.ModbusEndian
import com.crow.modbus.serialport.BaudRate
import com.crow.modbus.tools.toHexList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class MainActivity : AppCompatActivity() {

    private val mKModbusRTU = KModbusRtu()
    private val mKModbusTcp = KModbusTcp()
    private val mKModbusAscii = KModbusASCII()

    private val IO = CoroutineScope(Dispatchers.IO)

    override fun onDestroy() {
        super.onDestroy()
        "onDestroy".info()
        mKModbusRTU.cleanAll()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        initRTU(0, BaudRate.S_9600)
        initAscii(0, BaudRate.S_9600)
        initTcp("192.168.1.101", 502)
    }

    private fun initRTU(ttySNumber: Int, baudRate: Int) {
        mKModbusRTU.apply {
            openSerialPort(ttySNumber, baudRate)
            addOnMasterReceiveListener {
                val resp = mKModbusRTU.resolveMasterResp(it, ModbusEndian.ARRAY_BIG_BYTE_BIG) ?: return@addOnMasterReceiveListener
                resp.info()
            }
            setOnDataWriteReadyListener { buildMasterOutput(READ_HOLDING_REGISTERS, 1, 0, 1) }
            startRepeatReceiveDataTask(KModbusType.MASTER)
            startRepeatWriteDataTask(1000L, 1000L) { "RTU TIME OUT!".info() }
        }
    }

    private fun initTcp(host: String, port: Int) {
        mKModbusTcp.apply {
            startTcp(host, port, true) { ins, ops ->
                addOnMasterReceiveListener {  arrays -> resolveMasterResp(arrays, ModbusEndian.ARRAY_BIG_BYTE_BIG).info() }
                setOnDataWriteReadyListener { buildMasterOutput(READ_HOLDING_REGISTERS, 1, 0, 1) }
                startRepeatReceiveDataTask(ins, KModbusType.MASTER)
                startRepeatWriteDataTask(ops, 1000L, 1000L) { "TCP TIME OUT!".info() }
            }
        }
    }

    private fun initAscii(ttySNumber: Int, baudRate: Int) {
        mKModbusAscii.apply {
            mSkipAwait = true
            openSerialPort(ttySNumber, baudRate)
            addOnMasterReceiveListener {
                "ascii : ${it.toHexList()}".info()
            }
            setOnDataWriteReadyListener { buildMasterOutput(READ_HOLDING_REGISTERS, 1, 0, 1) }
            startRepeatReceiveDataTask(KModbusType.MASTER)
            startRepeatWriteDataTask(1000L, 1000L) { "ASCII TIME OUT!".info() }
        }
    }
}