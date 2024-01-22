@file:Suppress("SpellCheckingInspection", "unused")

package com.crow.modbus

import com.crow.modbus.interfaces.IKModbusWriteData
import com.crow.modbus.interfaces.ISerialPortExt
import com.crow.modbus.model.KModbusFunction
import com.crow.modbus.model.KModbusFunction.READ_COILS
import com.crow.modbus.model.KModbusFunction.READ_DISCRETE_INPUTS
import com.crow.modbus.model.KModbusFunction.READ_HOLDING_REGISTERS
import com.crow.modbus.model.KModbusFunction.READ_INPUT_REGISTERS
import com.crow.modbus.model.KModbusFunction.WRITE_COILS
import com.crow.modbus.model.KModbusFunction.WRITE_HOLDING_REGISTERS
import com.crow.modbus.model.KModbusFunction.WRITE_SINGLE_COIL
import com.crow.modbus.model.KModbusFunction.WRITE_SINGLE_REGISTER
import com.crow.modbus.model.KModbusRtuMasterResp
import com.crow.modbus.model.KModbusRtuSlaveResp
import com.crow.modbus.model.KModbusType
import com.crow.modbus.model.ModbusEndian
import com.crow.modbus.model.getFunction
import com.crow.modbus.serialport.SerialPortManager
import com.crow.modbus.tools.baseTenF
import com.crow.modbus.tools.error
import com.crow.modbus.tools.readBytes
import com.crow.modbus.tools.toInt16
import com.crow.modbus.tools.toUInt16LittleEndian
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.util.concurrent.Executors

/**
 * ● KModbusRtu
 *
 * ● 2023/12/1 10:14
 * @author crowforkotlin
 * @formatter:on
 */
class KModbusRtu : KModbus(), ISerialPortExt {

    /**
     * ● 读取监听
     *
     * ● 2024-01-10 18:19:38 周三 下午
     * @author crowforkotlin
     */
    private var mSlaveReadListener: ArrayList<(KModbusRtuSlaveResp) -> Unit>? = null
    private var mMasterReadListener: ArrayList<(ByteArray) -> Unit>? = null

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
    private var mCustomReadListener: ((BufferedInputStream) -> Unit)? = null

    /**
     * ● 串口管理
     *
     * ● 2024-01-10 19:23:42 周三 下午
     * @author crowforkotlin
     */
    private val mSerialPortManager by lazy { SerialPortManager() }

    private var mWriteTimeJob: Job? = null
    private var mWriteJob: Deferred<Unit>? = null
    private val mTaskJob by lazy { SupervisorJob() }
    private val mTaskScope by lazy { CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher() + mTaskJob) }

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
    private fun repeatSlaveActionOnRead(onReceive: suspend (KModbusRtuSlaveResp) -> Unit) {
        mSerialPortManager.onReadRepeatEnv {
            it?.let { bis ->
                val slave = bis.read()
                when(val function = getFunction(bis.read())) {
                    WRITE_SINGLE_REGISTER, WRITE_SINGLE_COIL -> {
                        val bytes = bis.readBytes(6)
                        val address = toInt16(bytes)
                        val values = byteArrayOf(bytes[2], bytes[3])
                        val crc = toUInt16LittleEndian(bytes, 4)
                        onReceive(KModbusRtuSlaveResp(slave, function, address, null, values, crc))
                    }
                    READ_COILS, READ_DISCRETE_INPUTS, READ_INPUT_REGISTERS, READ_HOLDING_REGISTERS -> {
                        val bytes = bis.readBytes(6)
                        val address = toInt16(bytes)
                        val count = toInt16(bytes, 2)
                        val crc = toUInt16LittleEndian(bytes, 4)
                        onReceive(KModbusRtuSlaveResp(slave, function, address, count, null, crc))
                    }
                    WRITE_HOLDING_REGISTERS, WRITE_COILS -> {

                    }
                }
            }
        }
    }

    /**
     * ● 自定义读取规则 手动读取解析数据
     *
     * ● 2024-01-10 19:48:26 周三 下午
     * @author crowforkotlin
     */
    private fun repeatCustomActionOnRead(onReceive: suspend (BufferedInputStream) -> Unit) {
        mSerialPortManager.onReadRepeatEnv {
            onReceive(it ?: return@onReadRepeatEnv)
        }
    }

    /**
     * ● 写入数据
     *
     * ● 2024-01-10 20:07:29 周三 下午
     * @author crowforkotlin
     */
    fun writeData(array: ByteArray) { mSerialPortManager.mWriteContext.launch { mSerialPortManager.writeBytes(array) } }

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
                    mWriteTimeJob?.cancel()
                    mWriteJob?.cancel()
                    mMasterReadListener?.forEach { it.invoke(data) }
                }
            }
            KModbusType.SLAVE -> {
                repeatSlaveActionOnRead { data ->
                    mWriteTimeJob?.cancel()
                    mWriteJob?.cancel()
                    mSlaveReadListener?.forEach { it.invoke(data) }
                }
            }
            KModbusType.CUSTOM -> {
                mWriteTimeJob?.cancel()
                mWriteJob?.cancel()
                repeatCustomActionOnRead { data ->
                    mCustomReadListener?.invoke(data)
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
    fun runRepeatWriteDataTask(interval: Long, timeOut: Long) {
        if (mSerialPortManager.mFileOutputStream == null) {
            "The write stream has not been opened yet. Maybe the serial port is not open?".error()
            return
        }
        mSerialPortManager.writeRepeat(interval, { mWriteListener?.onWrite() }, { scope ->
            runCatching {
                mWriteTimeJob = scope.launch(start = CoroutineStart.LAZY) {
                    delay(timeOut)
                    mWriteJob?.cancel()
                }
                mWriteJob = scope.async {
                    mWriteTimeJob?.start()
                    delay(Long.MAX_VALUE)
                }
                mWriteJob?.await()
            }
        })
    }


    /**
     * ● 监听器
     * 
     * ● 2024-01-10 20:03:16 周三 下午
     * @author crowforkotlin
     */
    fun addOnSlaveReceiveListener(listener: (KModbusRtuSlaveResp) -> Unit) {
        if (mSlaveReadListener == null) {
            mSlaveReadListener = arrayListOf()
        }
        mSlaveReadListener?.add(listener)
    }
    fun addOnMasterReceiveListener(listener: (ByteArray) -> Unit) {
        if (mMasterReadListener == null) {
            mMasterReadListener = arrayListOf()
        }
        mMasterReadListener?.add(listener)
    }
    fun setOnDataWriteReadyListener (listener: IKModbusWriteData) { mWriteListener = listener }
    fun setOnCustomReadRulesListener(listener: (BufferedInputStream) -> Unit) { mCustomReadListener = listener }
    fun removeSlaveReceiveListener(listener: (KModbusRtuSlaveResp) -> Unit) { mSlaveReadListener?.remove(listener) }
    fun removeMasterReceiveListener(listener: (ByteArray) -> Unit) { mMasterReadListener?.remove(listener) }
    fun removeAllSlaveReceiveListener(listener: (KModbusRtuSlaveResp) -> Unit) { mSlaveReadListener?.clear() }
    fun removeAllMasterReceiveListener(listener: (KModbusRtuMasterResp) -> Unit) { mMasterReadListener?.clear() }
    fun removeOnWriteDataReadyListener() { mWriteListener = null }

    /**
     * ● 构建KModbusRTU 主站输出数据
     *
     * ● 2024-01-16 16:52:43 周二 下午
     * @author crowforkotlin
     * @param function 功能码
     * @param slaveAddress 设备地址
     * @param startAddress 寄存器起始地址
     * @param count 字节数 或 寄存器数量， 取决于功能码
     * @param value 单个数据 可以是任意整数 对应一个寄存器，内部会将十进制最终转成十六进制写入
     * @param values 多个数据 可以是任意整数数组，每一个元素都能代表一个寄存器，如果你想写入浮点数，
     * 需要吧浮点数转成一个四字节数组并拿到两个16位的short，最后toInt写入, 在ByteExt中封装了通用方法，可以使用 Float.toIntArray()直接拿到IntArray写入即可
     * 通过此方法拿到的IntArray 一般数组大小不超过2个
     */
    fun buildMasterOutput(
        function: KModbusFunction,
        slaveAddress: Int,
        startAddress: Int,
        count: Int = 1,
        value: Int? = null,
        values: IntArray? = null,
        endian: ModbusEndian = ModbusEndian.ARRAY_BIG_BYTE_BIG,
    ): ByteArray {
        return getArray(toCalculateCRC16(buildMasterRequestOutput(slaveAddress, function, startAddress, count, value, values)).toByteArray(), endian)
    }

    /**
     * ● 解析主站接收到的响应数据
     *
     * ● 2024-01-22 17:12:13 周一 下午
     * @author crowforkotlin
     * @param bytes 数据包
     * @param endian 字节序
     */
    fun resolveMasterResp(bytes: ByteArray, endian: ModbusEndian): KModbusRtuMasterResp? {
        return runCatching {
            val arrays = getArray(bytes, endian)
            val slaveId = arrays[0].toInt()
            val function= arrays[1].toInt() and 0xFF
            val isFunctionCodeError = (function and baseTenF) + 0x80 == function
            if(isFunctionCodeError) {
                val dataSize = arrays.size - 4
                val valueBytes = ByteArray(dataSize)
                System.arraycopy(arrays, 3, valueBytes, 0, dataSize)
                KModbusRtuMasterResp(
                    mSlaveID = slaveId,
                    mFunction = function,
                    mByteCount = dataSize,
                    mValues =  valueBytes
                )
            } else {
                val byteCount = arrays[2].toInt()
                val valueBytes = ByteArray(byteCount)
                System.arraycopy(arrays, 3, valueBytes, 0, byteCount)
                KModbusRtuMasterResp(
                    mSlaveID = slaveId,
                    mFunction = function,
                    mByteCount = byteCount,
                    mValues =  valueBytes
                )
            }
        }
            .onFailure { it.stackTraceToString().error() }
            .getOrElse { null }
    }

    /**
     * ● 清除所有的任务,包括串口的内部任务, 并不会清除协程上下文, 任然可以继续launch
     *
     * ● 2024-01-22 18:38:54 周一 下午
     * @author crowforkotlin
     */
    fun cleanAll(): Boolean {
        return runCatching {
            mTaskJob.cancel()
            mWriteJob?.cancel()
            mWriteTimeJob?.cancel()
            mSlaveReadListener?.clear()
            mMasterReadListener?.clear()
            mWriteListener = null
            mSerialPortManager.cancelAllJob()
            mSerialPortManager.closeSerialPort()
            true
        }
            .onFailure { cause -> cause.stackTraceToString().error() }
            .getOrElse { false }
    }
}