@file:Suppress("PrivatePropertyName", "SpellCheckingInspection", "LocalVariableName",
    "SameParameterValue"
)
@file:OptIn(ExperimentalStdlibApi::class, ExperimentalStdlibApi::class, ExperimentalStdlibApi::class)

package com.crow.kmodbus

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.crow.kmodbus.databinding.ActivityMainBinding
import com.crow.modbus.KModbusAscii
import com.crow.modbus.KModbusRtu
import com.crow.modbus.model.KModbusFunction
import com.crow.modbus.model.KModbusFunction.READ_HOLDING_REGISTERS
import com.crow.modbus.model.KModbusRtuMasterResp
import com.crow.modbus.model.KModbusType
import com.crow.modbus.model.ModbusEndian
import com.crow.modbus.serialport.BaudRate
import com.crow.modbus.serialport.SerialPortParityFunction
import com.crow.modbus.tools.toHexList
import com.crow.modbus.tools.toInt32Data
import com.crow.modbus.tools.toStringGB2312
import com.listen.x3player.kt.modbus.KModbusTcp
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val mBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val mKModbusRtu = KModbusRtu()
    private val mKModbusTcp = KModbusTcp()
    private val mKModbusAscii = KModbusAscii()

    private val mMainScope = MainScope()
    override fun onDestroy() {
        super.onDestroy()
        mMainScope.cancel()
        // Clear all context to prevent any references. You can also continue to use the object after clearing it to continue your tasks later.
        mKModbusRtu.cancelAll()
        mKModbusAscii.cancelAll()
        mKModbusTcp.cancelAll()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

        // You can open different serial ports by constructing multiple KModbusRtu, the same goes for TCP and ASCII
        initRTU(ttySNumber = 0, BaudRate.S_9600)
//        initAscii(ttySNumber = 3, BaudRate.S_9600)
//        initTcp(host = "192.168.1.101", port = 502)
    }

    private fun initRTU(ttySNumber: Int, baudRate: Int) {
        mKModbusRtu.apply {
            // The openSerialPort function has multiple overloads. You can customize the incoming control, serial port, baud rate, check mode, stop bit, and data bit.
            openSerialPort(ttysNumber = ttySNumber, baudRate = baudRate, parity = SerialPortParityFunction.NONE, stopBit = 1, dataBit = 8)

            // Set the listener for data returned from the slave station in master mode
            addOnMasterReceiveListener { arrays ->
                runCatching {
                    // No matter what data is written, as long as the parsed data is empty, it is incorrect!
                    val resp: KModbusRtuMasterResp = resolveMasterResp(arrays, ModbusEndian.ARRAY_BIG_BYTE_BIG) ?: return@addOnMasterReceiveListener

                    // Even if the data is parsed, it's possible that mValues will be null, and that's because the modbus slave will return a successful response by default!
                    val content: Long = resp.mValues.toInt32Data(index = 0, length = 2) ?: return@runCatching
                    mMainScope.launch {
                        mBinding.rtu.text = content.toString()
                    }
                }
                "Rtu : ${resolveMasterResp(arrays, ModbusEndian.ARRAY_BIG_BYTE_BIG)}".info()
            }

            // If you want to poll and write multiple data, you can add the data to the queue in the same way as listOf.
//            setOnDataWriteReadyListener { listOf(buildMasterOutput(READ_HOLDING_REGISTERS, 1, 0, 1)) }

            // Set the reading behavior to: master mode. If it is slave mode, it means that the data sent by the master station will be read.
            startRepeatReceiveDataTask(kModbusBehaviorType = KModbusType.MASTER)
            writeData(mKModbusRtu.buildMasterOutput(
                function = KModbusFunction.READ_HOLDING_REGISTERS,
                slaveAddress = 1,
                startAddress = 1,
                count = 2,
                value = 4
            ))
            // Enable polling tasks for writing data, with built-in timeout mechanism
//            startRepeatWriteDataTask(interval = 50, timeOut = 1000L) { "Rtu Time out!".info() }

            // If you do not enable polling writing, you can also manually control the writing of data yourself.
           /*
            mKModbusRtu.writeData(buildMasterOutput(
                function = KModbusFunction.WRITE_SINGLE_REGISTER,
                slaveAddress = 6,
                startAddress = 0,
                count = 2,
                value = 0
            ))
            */
        }
    }

    private fun initTcp(host: String, port: Int) {
        mKModbusTcp.apply{
            startTcp(host, port, retry = true) { ins, ops ->
                addOnMasterReceiveListener {  arrays ->
                    runCatching {
                        val resp = resolveMasterResp(arrays, ModbusEndian.ARRAY_BIG_BYTE_BIG) ?: return@addOnMasterReceiveListener
                        val content = resp.mValues.toStringGB2312(index = 0, length = 5)
                        mMainScope.launch {
                            mBinding.tcp.text = content
                        }
                    }
                 }
                setOnDataWriteReadyListener { listOf(buildMasterOutput(READ_HOLDING_REGISTERS, 1, 0, 5)) }
                startRepeatReceiveDataTask(ins, KModbusType.MASTER)
                startRepeatWriteDataTask(ops, interval = 1000L, timeOut = 1000L) { "Tcp time out!".info() }
            }
        }
    }

    private fun initAscii(ttySNumber: Int, baudRate: Int) {
        mKModbusAscii.apply {
            openSerialPort(ttySNumber, baudRate)
            addOnMasterReceiveListener { arrays -> "Ascii : ${arrays.toHexList()}".info() }
            setOnDataWriteReadyListener { listOf(buildMasterOutput(READ_HOLDING_REGISTERS, 1, 0, 1)) }
            startRepeatReceiveDataTask(KModbusType.MASTER)
            startRepeatWriteDataTask(interval = 1000L, timeOut = 1000L) { "Ascii time out!".info() }
        }
    }
}