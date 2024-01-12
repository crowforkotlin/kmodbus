@file:Suppress("SpellCheckingInspection", "unused")

package com.crow.modbus

import com.crow.modbus.interfaces.IKModbusReadData
import com.crow.modbus.interfaces.IKModbusWriteData
import com.crow.modbus.interfaces.ISerialPortExt
import com.crow.modbus.model.KModbusType
import com.crow.modbus.model.KModbusFunction.READ_COILS
import com.crow.modbus.model.KModbusFunction.READ_DISCRETE_INPUTS
import com.crow.modbus.model.KModbusFunction.READ_HOLDING_REGISTERS
import com.crow.modbus.model.KModbusFunction.READ_INPUT_REGISTERS
import com.crow.modbus.model.KModbusFunction.WRITE_SINGLE_COIL
import com.crow.modbus.model.KModbusFunction.WRITE_SINGLE_REGISTER
import com.crow.modbus.model.KModbusRtuRespPacket
import com.crow.modbus.model.getFunction
import com.crow.modbus.serialport.SerialPortManager
import com.crow.modbus.tools.error
import com.crow.modbus.tools.info
import com.crow.modbus.tools.readBytes
import com.crow.modbus.tools.toInt16
import com.crow.modbus.tools.toUInt16LittleEndian

/**
 * ● KModbusRtu
 *
 * ● 2023/12/1 10:14
 * @author crowforkotlin
 * @formatter:on
 */
class KModbusRtu : KModbus(), ISerialPortExt {

    /**
     * ● RTU数据类型
     *
     * ● 2023-12-05 18:30:05 周二 下午
     * @author crowforkotlin
     */
    private val mRtu by lazy { KModbusRtuMaster.getInstance() }

    /**
     * ● 读取监听
     *
     * ● 2024-01-10 18:19:38 周三 下午
     * @author crowforkotlin
     */
    private val mReadListener = arrayListOf<IKModbusReadData>()

    /**
     * ● 写入监听
     *
     * ● 2024-01-10 18:29:07 周三 下午
     * @author crowforkotlin
     */
    private var mWriteListener: IKModbusWriteData? = null

    /**
     * ● 自定义读取监听器
     *
     * ● 2024-01-10 19:44:41 周三 下午
     * @author crowforkotlin
     */
    private var mCustomReadListener: (() -> ByteArray?)? = null

    /**
     * ● 串口管理
     *
     * ● 2024-01-10 19:23:42 周三 下午
     * @author crowforkotlin
     */
    private val mSerialPortManager by lazy { SerialPortManager() }

    override fun reOpenSerialPort(ttySNumber: Int, baudRate: Int) {
        closeSerialPort()
        openSerialPort(ttySNumber, baudRate)
    }
    override fun reOpenSerialPort(path: String, baudRate: Int) {
        closeSerialPort()
        openSerialPort(path, baudRate)
    }
    override fun openSerialPort(ttysNumber: Int, baudRate: Int) { mSerialPortManager.openSerialPort(ttysNumber, baudRate) }
    override fun openSerialPort(path: String, baudRate: Int) { mSerialPortManager.openSerialPort(path, baudRate) }
    override fun closeSerialPort(): Boolean { return mSerialPortManager.closeSerialPort() }

    /**
     * ● 作为主站去读取数据
     *
     * ● 2023-12-04 15:02:40 周一 下午
     * @author crowforkotlin
     */
    private fun repeatMasterActionOnRead(onReceive: suspend (ByteArray) -> Unit) {
        val headByteSize = 3
        mSerialPortManager.onReadRepeatEnv {
            it?.let { bis ->
                val headBytes = ByteArray(headByteSize)
                var headReaded = 0
                while (headReaded < headByteSize) {
                    val readed = bis.read(headBytes, headReaded, headByteSize - headReaded)
                    if (readed == - 1) { break }
                    headReaded += readed
                }
                val completeByteSize = headBytes.last().toInt() + 5
                val completeBytes = ByteArray(completeByteSize)
                var completeReaded = 3
                completeBytes[0] = headBytes[0]
                completeBytes[1] = headBytes[1]
                completeBytes[2] = headBytes[2]
                while (completeReaded < completeByteSize) {
                    val readed = bis.read(completeBytes, completeReaded, completeByteSize - completeReaded)
                    if (readed == -1) break
                    completeReaded += readed
                }
                onReceive(completeBytes)
            }
        }
    }

    /**
     * ● 作为从站去读取数据
     *
     * ● 2024-01-10 19:48:17 周三 下午
     * @author crowforkotlin
     */
    private fun repeatSlaveActionOnRead(onReceive: suspend (ByteArray) -> Unit) {
        mSerialPortManager.onReadRepeatEnv {
            it?.let { bis ->
                runCatching {
                    val slave = bis.read()
                    when(val function = getFunction(bis.read())) {
                        WRITE_SINGLE_REGISTER, WRITE_SINGLE_COIL -> {
                            val bytes = bis.readBytes(6)
                            val address = toInt16(bytes)
                            val values = byteArrayOf(bytes[2], bytes[3])
                            val crc = toUInt16LittleEndian(bytes, 4)
                            KModbusRtuRespPacket(slave, function, address, null, values, crc).verifyCRC16()
                        }
                        READ_COILS, READ_DISCRETE_INPUTS, READ_INPUT_REGISTERS, READ_HOLDING_REGISTERS -> {
                            val bytes = bis.readBytes(6)
                            val address = toInt16(bytes)
                            val count = toInt16(bytes, 2)
                            val crc = toUInt16LittleEndian(bytes, 4)
                            KModbusRtuRespPacket(slave, function, address, count, null, crc).verifyCRC16()
                        }
                        else -> {

                        }
                    }
                }
                    .onFailure { catch -> catch.stackTraceToString().error() }
            }
        }
    }

    /**
     * ● 自定义读取规则 手动读取解析数据
     *
     * ● 2024-01-10 19:48:26 周三 下午
     * @author crowforkotlin
     */
    private fun repeatCustomActionOnRead(onReceive: suspend (ByteArray) -> Unit) {
        mSerialPortManager.onReadRepeatEnv {
            onReceive(mCustomReadListener?.invoke() ?: return@onReadRepeatEnv)
        }
    }

    /**
     * ● 写入数据
     *
     * ● 2024-01-10 20:07:29 周三 下午
     * @author crowforkotlin
     */
    fun writeData(array: ByteArray) { mSerialPortManager.writeBytes(array) }

    /**
     * ● 启用接受数据的任务
     *
     * ● 2024-01-10 18:23:52 周三 下午
     * @author crowforkotlin
     */
    fun runRepeatReceiveDataTask(kModbusType: KModbusType) {
        if (mSerialPortManager.mFileOutputStream == null) {
            "The read stream has not been opened yet. Maybe the serial port is not open?".error()
            return
        }
        when(kModbusType) {
            KModbusType.MASTER -> {
                repeatMasterActionOnRead { data ->
                    mReadListener.forEach { it.onRead(data) }
                }
            }
            KModbusType.SLAVE -> {
                repeatSlaveActionOnRead { data ->
                    mReadListener.forEach { it.onRead(data) }
                }
            }
            KModbusType.CUSTOM -> {
                repeatCustomActionOnRead { data ->
                    mReadListener.forEach { it.onRead(data) }
                }
            }
        }
    }

    /**
     * ● 循环写入数据任务
     *
     * ● 2024-01-10 18:33:35 周三 下午
     * @author crowforkotlin
     */
    fun runRepeatWriteDataTask(interval: Long)  {
        if (mSerialPortManager.mFileOutputStream == null) {
            "The write stream has not been opened yet. Maybe the serial port is not open?".error()
            return
        }
        mSerialPortManager.writeRepeat(interval) { mWriteListener?.onWrite() }
    }

    /**
     * ● 监听器
     * 
     * ● 2024-01-10 20:03:16 周三 下午
     * @author crowforkotlin
     */
    fun addOnReceiveDataListener(listener: IKModbusReadData) { mReadListener.add(listener) }
    fun setOnDataWriteReadyListener (listener: IKModbusWriteData) { mWriteListener = listener }
    fun setOnCustomReadRulesListener(listener: () -> ByteArray?) { mCustomReadListener = listener }
    fun removeOnDataListener(listener: IKModbusReadData) { mReadListener.remove(listener) }
    fun removeAllOnReceiveDataListener() { mReadListener.clear() }
    fun removeOnWriteDataReadyListener() { mWriteListener = null }
}