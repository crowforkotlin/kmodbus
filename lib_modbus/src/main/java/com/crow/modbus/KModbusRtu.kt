@file:Suppress("SpellCheckingInspection")

package com.crow.modbus

import com.crow.modbus.serialport.SerialPortManager
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ● KModbusRtu
 *
 * ● 2023/12/1 10:14
 * @author crowforkotlin
 * @formatter:on
 */
class KModbusRtu : SerialPortManager() {

    /**
     * ● RTU数据类型
     *
     * ● 2023-12-05 18:30:05 周二 下午
     * @author crowforkotlin
     */
    private val mRtu by lazy { KModbusRtuMaster.getInstance() }

    /**
     * ● 循环写入数据
     *
     * ● 2023-12-04 15:02:25 周一 下午
     * @author crowforkotlin
     */
    override fun writeRepeat(interval: Long, onWrite: suspend () -> ByteArray) {
        super.writeRepeat(interval, onWrite)
    }

    /**
     * ● 循环读取数据
     *
     * ● 2023-12-04 15:02:40 周一 下午
     * @author crowforkotlin
     */
    override fun onReadRepeat(onReceive: suspend (ByteArray) -> Unit) {
        val headByteSize = 3
        // 取消所有读取的子任务
        mReadJob.cancelChildren()
        mReadContext.launch {
            mFileInputStream?.let {  ins ->
                while (true) {
                    val headBytes = ByteArray(headByteSize)
                    var headReaded = 0
                    while (headReaded < headByteSize) {
                        val readed = ins.read(headBytes, headReaded, headByteSize - headReaded)
                        if (readed == - 1) { break}
                        headReaded += readed
                    }
                    val completeByteSize = headBytes.last().toInt() + 5
                    val completeBytes = ByteArray(completeByteSize)
                    var completeReaded = 3
                    completeBytes[0] = headBytes[0]
                    completeBytes[1] = headBytes[1]
                    completeBytes[2] = headBytes[2]
                    while (completeReaded < completeByteSize) {
                        val readed = ins.read(completeBytes, completeReaded, completeByteSize - completeReaded)
                        if (readed == -1) break
                        completeReaded += readed
                    }
                    onReceive(completeBytes)
                }
            }
        }
    }
}