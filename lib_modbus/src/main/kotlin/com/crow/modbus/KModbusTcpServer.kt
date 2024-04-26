@file:Suppress("SpellCheckingInspection", "unused", "LocalVariableName")

package com.crow.modbus

import com.crow.modbus.model.KModbusFunction
import com.crow.modbus.model.KModbusRetryCancellationException
import com.crow.modbus.model.KModbusTcpSlaveResp
import com.crow.modbus.model.KModbusType
import com.crow.modbus.model.getKModbusFunction
import com.crow.modbus.tools.error
import com.crow.modbus.tools.info
import com.crow.modbus.tools.message
import com.crow.modbus.tools.readBytes
import com.crow.modbus.tools.toInt16
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.SocketException

/**
 * ⦁  KModbusTcp
 *
 * ⦁  2023/12/1 10:50
 * @author crowforkotlin
 * @formatter:on
 */
class KModbusTcpServer(mDispatcher: CoroutineDispatcher = Dispatchers.IO) : KModbus() {

    /**
     * ⦁  从站是否自动回包
     *
     * ⦁ 2024-04-09 14:29:11 周二 下午
     * @author crowforkotlin
     */
    private var mSlaveResponseEnable: Boolean = true

    private val mServerJob = SupervisorJob()
    private val mServerScope = CoroutineScope(mDispatcher + mServerJob)

    private fun getModbusSlaveResp(ins: BufferedInputStream, ops: BufferedOutputStream, byteReadLen: Int = 6): KModbusTcpSlaveResp {
        var byteReaded = 0
        val mbap = ByteArray(byteReadLen)
        while (byteReaded < byteReadLen) {
            val bytes = ins.read(mbap, byteReaded, byteReadLen)
            if (bytes == -1) { break }
            byteReaded += bytes
        }
        val dataLen = mbap.last().toInt()
        val pdu = ins.readBytes(dataLen)
        if (pdu.isEmpty()) { ins.close() }
        val functionByte = pdu[1]
        val slaveId = pdu[0]
        val addressHigh = pdu[2]
        val addressLow = pdu[3]
        val slaveResp = when(val function = getKModbusFunction(functionByte.toInt())) {
            KModbusFunction.WRITE_SINGLE_REGISTER, KModbusFunction.WRITE_SINGLE_COIL -> {
                val valueHigh = pdu[4]
                val valueLow = pdu[5]
                ops.tryRespond(mbap, slaveId, functionByte, addressHigh, addressLow, valueHigh, valueLow)
                KModbusTcpSlaveResp(
                    mMbap = mbap,
                    mSlaveID = slaveId.toInt(),
                    mFunction = function,
                    mAddress = toInt16(byteArrayOf(addressHigh, addressLow)),
                    mCount = null,
                    mByteCount = null,
                    mValues = byteArrayOf(valueHigh, valueLow)
                )
            }
            KModbusFunction.WRITE_MULTIPLE_REGISTERS -> {
                val regCountHigh = pdu[4]
                val regCountLow = pdu[5]
                ops.tryRespond(mbap, slaveId, functionByte, addressHigh, addressLow, regCountHigh, regCountLow)
                val byteCount = pdu[6].toInt()
                val values = ByteArray(byteCount)
                System.arraycopy(pdu, 7, values, 0, byteCount)
                KModbusTcpSlaveResp(
                    mMbap = mbap,
                    mSlaveID = slaveId.toInt(),
                    mFunction = function,
                    mAddress = toInt16(byteArrayOf(addressHigh, addressLow)),
                    mCount = toInt16(byteArrayOf(regCountHigh, regCountLow)),
                    mByteCount = byteCount,
                    mValues = values
                )
            }
            KModbusFunction.WRITE_MULTIPLE_COILS -> {
                val regCountHigh = pdu[4]
                val regCountLow = pdu[5]
                ops.tryRespond(mbap, slaveId, functionByte, addressHigh, addressLow, regCountHigh, regCountLow)
                val byteCount = pdu[6].toInt()
                val values = ByteArray(byteCount)
                repeat(byteCount) { values[it] = pdu[7 + it] }
                KModbusTcpSlaveResp(
                    mMbap = mbap,
                    mSlaveID = slaveId.toInt(),
                    mFunction = function,
                    mAddress = toInt16(byteArrayOf(addressHigh, addressLow)),
                    mCount = toInt16(byteArrayOf(regCountHigh, regCountLow)),
                    mByteCount = byteCount,
                    mValues = values
                )
            }
            else -> { kotlin.error("kmodbus server get unknow function!") }
        }
        return slaveResp
    }

    private fun BufferedOutputStream.tryRespond(mbap: ByteArray, slaveId: Byte, function: Byte, addressHigh: Byte, addressLow: Byte, endHigh: Byte, endLow: Byte) {
        if (mSlaveResponseEnable) {
            val respond = ByteArray(12)
            repeat(6) { respond[it] = mbap[it] }
            respond[6] = slaveId
            respond[7] = function
            respond[8] = addressHigh
            respond[9] = addressLow
            respond[10] = endHigh
            respond[11] = endLow
            write(respond)
            flush()
        }
    }

    /**
     * ⦁  TCP 循环读取
     *
     * ⦁  2023-12-05 18:50:42 周二 下午
     * @author crowforkotlin
     */
    private suspend fun repeatSlaveActionOnRead(ins: BufferedInputStream, ops: BufferedOutputStream, serveJob: Job, onReceive: suspend (KModbusTcpSlaveResp) -> Unit) {
        onReadRepeatEnv(serveJob) {
            onReceive(getModbusSlaveResp(ins, ops))
        }
    }

    /**
     * ⦁  启用接受数据的任务
     *
     * ⦁  2024-01-10 18:23:52 周三 下午
     * @author crowforkotlin
     */
    suspend fun continuouslyReadData(ins: BufferedInputStream, ops: BufferedOutputStream, clientJob: Job, kModbusType: KModbusType, onReceive: ((KModbusTcpSlaveResp) -> Unit)): Job {
        return mServerScope.launch {
            when(kModbusType) {
                KModbusType.MASTER -> { error("kmodbus tcp server cannot be set as a master mode!") }
                KModbusType.SLAVE -> {
                    repeatSlaveActionOnRead(ins, ops, clientJob) { slaveResp ->
                        onReceive(slaveResp)
                    }
                }
                KModbusType.CUSTOM -> {}
            }
        }
    }

    /**
     * ⦁  启动TCP任务
     *
     * ⦁  2024-01-23 17:30:56 周二 下午
     * @author crowforkotlin
     */
    fun tcpServer(port: Int, isRetry: Boolean = true, retryDuration: Long = 2000L, onClientConnected: suspend (BufferedInputStream, BufferedOutputStream, clientJob: Job, serverJob: Job) -> Unit) {
        mServerScope.launch {
            val serverJob = coroutineContext.job
            var serverSocket: ServerSocket? = null
            runCatching {
                 val server = ServerSocket(port).also { serverSocket = it }
                "kmodbus tcp server started! $server".info()
                while (isActive) {
                    val socket = runCatching { server.accept() }
                        .onFailure { cause ->
                            if (cause is SocketException || cause is IOException) {
                                throw cause
                            }
                        }
                        .getOrNull()
                    socket?.apply {
                        launch {
                            "kmodbus tcp client connected!".info()
                            var ins: BufferedInputStream? = null
                            var ops: BufferedOutputStream? = null
                            runCatching {
                                val _ins = BufferedInputStream(getInputStream())
                                val _ops = BufferedOutputStream(getOutputStream())
                                ins = _ins
                                ops = _ops
                                val socketJob = coroutineContext.job
                                onClientConnected(_ins, _ops, socketJob, serverJob)
                                socketJob.join()
                            }
                                .onFailure { cause -> "kmodbus tcp client cause error : ${cause.stackTraceToString()}".error() }
                            ins?.close()
                            ops?.close()
                            close()
                        }
                    }
                }
                server.close()
                if (isRetry) {
                    delay(retryDuration)
                    tcpServer(port, true, retryDuration, onClientConnected)
                }
            }
                .onFailure { cause ->
                    "kmodbus start server error : ${cause.stackTraceToString()}".error()
                    serverSocket?.close()
                    if (isRetry) {
                        delay(retryDuration)
                        tcpServer(port, true, retryDuration, onClientConnected)
                    }
                }
        }
    }

    /**
     * ⦁  取消所有任务
     *
     * ⦁  2023-12-05 18:49:53 周二 下午
     * @author crowforkotlin
     */
    fun cancelAll() {
        mServerJob.cancelChildren()
    }

    private suspend inline fun onReadRepeatEnv(clientJob: Job, crossinline onRepat: suspend CoroutineScope.() -> Unit) {
        coroutineScope {
                while (isActive) {
                    runCatching {
                        onRepat()
                    }
                        .onFailure { cause ->
                            "kmodbus tcp server read exception : ${cause.message()}".error()
                            if (cause is SocketException || cause is IOException) {
                                cancel()
                                clientJob.cancel(KModbusRetryCancellationException())
                            }
                        }
                }
        }
    }
}