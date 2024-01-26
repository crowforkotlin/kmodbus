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

class MainActivity : AppCompatActivity() {

    private val mKModbusRtu = KModbusRtu()
    private val mKModbusTcp = KModbusTcp()
    private val mKModbusAscii = KModbusASCII()

    override fun onDestroy() {
        super.onDestroy()

        // Clear all context to prevent any references. You can also continue to use the object after clearing it to continue your tasks later.
        mKModbusRtu.cleanAllContext()
        mKModbusAscii.cleanAllContext()
        mKModbusTcp.cleanAllContext()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // You can open different serial ports by constructing multiple KModbusRtu, the same goes for TCP and ASCII
        initRTU(ttySNumber = 0, BaudRate.S_9600)
        initAscii(ttySNumber = 3, BaudRate.S_9600)
        initTcp(host = "192.168.1.101", port = 502)
    }

    private fun initRTU(ttySNumber: Int, baudRate: Int) {
        mKModbusRtu.apply {
            openSerialPort(ttySNumber, baudRate)
            addOnMasterReceiveListener { arrays -> "RTU : ${resolveMasterResp(arrays, ModbusEndian.ARRAY_BIG_BYTE_BIG)}".info() }
            setOnDataWriteReadyListener { listOf(buildMasterOutput(READ_HOLDING_REGISTERS, 1, 0, 1)) }
            startRepeatReceiveDataTask(KModbusType.MASTER)
            startRepeatWriteDataTask(1000L, 1000L) { "RTU TIME OUT!".info() }
        }
    }

    private fun initTcp(host: String, port: Int) {
        mKModbusTcp.apply {
            startTcp(host, port, true) { ins, ops ->
                addOnMasterReceiveListener {  arrays -> "TCP : ${resolveMasterResp(arrays, ModbusEndian.ARRAY_BIG_BYTE_BIG)}".info() }
                setOnDataWriteReadyListener { listOf(buildMasterOutput(READ_HOLDING_REGISTERS, 1, 0, 1)) }
                startRepeatReceiveDataTask(ins, KModbusType.MASTER)
                startRepeatWriteDataTask(ops, 1000L, 1000L) { "TCP TIME OUT!".info() }
            }
        }
    }

    private fun initAscii(ttySNumber: Int, baudRate: Int) {
        mKModbusAscii.apply {
            openSerialPort(ttySNumber, baudRate)
            addOnMasterReceiveListener { arrays -> "ASCII : ${arrays.toHexList()}".info() }
            setOnDataWriteReadyListener { listOf(buildMasterOutput(READ_HOLDING_REGISTERS, 1, 0, 1)) }
            startRepeatReceiveDataTask(KModbusType.MASTER)
            startRepeatWriteDataTask(1000L, 1000L) { "ASCII TIME OUT!".info() }
        }
    }
}