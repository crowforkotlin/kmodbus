@file:Suppress("PrivatePropertyName", "SpellCheckingInspection", "LocalVariableName",
    "SameParameterValue"
)
@file:OptIn(ExperimentalStdlibApi::class, ExperimentalStdlibApi::class, ExperimentalStdlibApi::class)

package com.crow.kmodbus

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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
import com.crow.modbus.tools.toStringGB2312
import com.crow.modbus.KModbusTcpClient
import com.crow.modbus.KModbusTcpServer
import com.crow.modbus.model.KModbusFunction.WRITE_MULTIPLE_COILS
import com.crow.modbus.model.KModbusFunction.WRITE_SINGLE_COIL
import com.crow.modbus.tools.splitInt8ToBits
import com.crow.modbus.tools.toInt32Data
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val mBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val mKModbusRtu = KModbusRtu()
    private val mKModbusTcpClient = KModbusTcpClient()
    private val mKModbusTcpServer = KModbusTcpServer()
    private val mKModbusAscii = KModbusAscii()

    override fun onDestroy() {
        super.onDestroy()
        // Clear all context to prevent any references. You can also continue to use the object after clearing it to continue your tasks later.
        mKModbusRtu.cancelAll()
        mKModbusAscii.cancelAll()

        // When you clear all tasks, kmodbus TCP will be forcibly terminated even if the reconnection is configured
        mKModbusTcpClient.cancelAll()
        mKModbusTcpServer.cancelAll()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

        // You can open different serial ports by constructing multiple KModbusRtu, the same goes for TCP and ASCII
//        initRtuSlave(ttySNumber = 0, BaudRate.S_9600)
//        initTcpSlave(port = 8080)
        initRtuMaster(ttySNumber = 3, BaudRate.S_9600)
//        initAsciiMaster(ttySNumber = 3, BaudRate.S_9600)


        // kmodbus tcp msater support multiple client connection
//        initTcpMaster(host = "192.168.1.102", port = 8080)
//        initTcpMaster(host = "192.168.1.102", port = 8080)
//        initTcpMaster(host = "192.168.1.102", port = 8080)
//        val clinetJob = initTcpMaster(host = "192.168.1.102", port = 8080)
//        lifecycleScope.launch {
//            delay(3000L)
//
//            // If you cancel the kmodbus TCP job, regardless of whether you have the reconnection mechanism enabled or not, this will be invalid and the TCP connection will be closed
//            clinetJob.cancel()
//        }
    }

    private fun initRtuSlave(ttySNumber: Int, baudRate: Int) {
        mKModbusRtu.apply {
            openSerialPort(ttysNumber = ttySNumber, baudRate = baudRate, parity = SerialPortParityFunction.NONE, stopBit = 1, dataBit = 8)

            addOnSlaveReceiveListener { slaveResp ->
                runCatching {
                    slaveResp.mValues?.let { value ->
                        when(slaveResp.mFunction) {
                            KModbusFunction.WRITE_SINGLE_COIL -> {
                                val coilVal = value.toInt32Data(index = 0, length = 2) ?: 0
                                val coilBoolVal = coilVal < 0
                                "coilsVal is ${coilBoolVal}".info()
                            }
                            KModbusFunction.WRITE_MULTIPLE_COILS -> {
                                val coilsVal = value.splitInt8ToBits(reverse = true)
                                "coilsVal is ${coilsVal.toList()}".info()
                            }
                            KModbusFunction.WRITE_MULTIPLE_REGISTERS -> {
                                val registersVal = value.toInt32Data(index = 0, length = 2) ?: 0
                                "registersVal is $registersVal".info()
                            }
                            else -> {
                                // TODO Maybe is read...
                            }
                        }
                    }
                }
            }

            startRepeatReceiveDataTask(KModbusType.SLAVE)
        }
    }

    private fun initTcpSlave(port: Int) {
        mKModbusTcpServer.apply{
            tcpServer(port) { ins, ops ->
                continuouslyReadData(ins, ops, KModbusType.SLAVE) { slaveResp ->
                    slaveResp.mValues?.let { value ->
                        when(slaveResp.mFunction) {
                            WRITE_MULTIPLE_COILS -> {
                                "tcp slave resp value : ${value.splitInt8ToBits(reverse = true).toList().info()}".info()
                            }
                            WRITE_SINGLE_COIL -> {
                                "tcp slave resp value : ${value.splitInt8ToBits(reverse = true).toList().info()}".info()
                            }
                            else -> { }
                        }
                        "tcp slave resp : $slaveResp".info()
                        "--------- tcp slave receive end ---------".info()
                    }
                }
            }
        }
    }

    private fun initRtuMaster(ttySNumber: Int, baudRate: Int) {
        mKModbusRtu.apply {
            // The openSerialPort function has multiple overloads. You can customize the incoming control, serial port, baud rate, check mode, stop bit, and data bit.
            openSerialPort(ttysNumber = ttySNumber, baudRate = baudRate, parity = SerialPortParityFunction.NONE, stopBit = 1, dataBit = 8)

            // Set the listener for data returned from the slave station in master mode
            addOnMasterReceiveListener { arrays ->
                runCatching {
                    // No matter what data is written, as long as the parsed data is empty, it is incorrect!
                    arrays.info()
                    val resp: KModbusRtuMasterResp = resolveMasterResp(arrays, ModbusEndian.ARRAY_BIG_BYTE_BIG) ?: return@addOnMasterReceiveListener

                    // Even if the data is parsed, it's possible that mValues will be null, and that's because the modbus slave will return a successful response by default!
                    val content: Long = resp.mValues.toInt32Data(index = 0, length = 2) ?: return@runCatching
                    lifecycleScope.launch {
                        mBinding.rtu.text = content.toString()
                    }
                }
                "Rtu : ${resolveMasterResp(arrays, ModbusEndian.ARRAY_BIG_BYTE_BIG)}".info()
            }

            // If you want to poll and write multiple data, you can add the data to the queue in the same way as listOf.
             setOnDataWriteReadyListener { listOf(buildMasterOutput(READ_HOLDING_REGISTERS, 1, 0, 1)) }

            // Set the reading behavior to: master mode. If it is slave mode, it means that the data sent by the master station will be read.
            startRepeatReceiveDataTask(kModbusBehaviorType = KModbusType.MASTER)

            /*writeData(mKModbusRtu.buildMasterOutput(
                function = KModbusFunction.READ_HOLDING_REGISTERS,
                slaveAddress = 1,
                startAddress = 1,
                count = 2,
                value = 4
            ))*/
            // Enable polling tasks for writing data, with built-in timeout mechanism
             startRepeatWriteDataTask(interval = 1000L, timeOut = 1000L) { "Rtu Time out!".info() }

            // If you do not enable polling writing, you can also manually control the writing of data yourself.
           /* mKModbusRtu.writeData(buildMasterOutput(
                function = KModbusFunction.WRITE_SINGLE_REGISTER,
                slaveAddress = 6,
                startAddress = 0,
                count = 2,
                value = 0
            )) */
        }
    }

    private fun initTcpMaster(host: String, port: Int): Job {
        mKModbusTcpClient.apply{
            return tcpClient(KModbusTcpClient.ClientInfo(host, port) { ins, ops, clientInfo, clientJob ->
                val receiveJob = continuouslyReadData(clientInfo, clientJob, ins, KModbusType.MASTER) { slaveResp ->
                    runCatching {
                        val content = slaveResp.mValues.toStringGB2312(index = 0, length = 5)
                        lifecycleScope.launch {
                            mBinding.tcp.text = content
                        }
                    }
                }
                val writeJob = continuouslyWriteData(clientInfo, clientJob, ops, interval = 1000L, timeOut = 1000L) {
                    listOf(buildMasterOutput(READ_HOLDING_REGISTERS, 1, 0, 5))
                }
                listOf(receiveJob, writeJob)
            })
        }
    }

    private fun initAsciiMaster(ttySNumber: Int, baudRate: Int) {
        mKModbusAscii.apply {
            openSerialPort(ttySNumber, baudRate)
            addOnMasterReceiveListener { arrays -> "Ascii : ${arrays.toHexList()}".info() }
            setOnDataWriteReadyListener { listOf(buildMasterOutput(READ_HOLDING_REGISTERS, 1, 0, 1)) }
            startRepeatReceiveDataTask(KModbusType.MASTER)
            startRepeatWriteDataTask(interval = 1000L, timeOut = 1000L) { "Ascii time out!".info() }
        }
    }
}