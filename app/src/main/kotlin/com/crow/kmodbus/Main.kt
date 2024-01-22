package com.crow.kmodbus

import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import com.crow.modbus.KModbusRtu
import com.crow.modbus.model.KModbusFunction
import com.crow.modbus.tools.fromFloat32
import com.crow.modbus.tools.fromInt32
import com.crow.modbus.tools.toFloat32
import com.crow.modbus.tools.toFloatData
import com.crow.modbus.tools.toHexList
import com.crow.modbus.tools.toInt32
import com.crow.modbus.tools.toIntArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.Date

@OptIn(ExperimentalStdlibApi::class)
suspend fun main() {
    runCatching {
        var job1: Deferred<Unit>? = null
        var job2: Job? = null
        val scope = CoroutineScope(Dispatchers.IO)
        println("RUNNING")
        val timeJob = scope.launch(start = CoroutineStart.LAZY) {
            delay(2000)
            job1?.cancel()
            println("CANCEL")
        }
        job1 = scope.async {
            timeJob.start()
            delay(500)
            timeJob.cancel()
            delay(Long.MAX_VALUE)
        }
        job1.await()
    }
        .onFailure { println(it.stackTraceToString()) }
    println("END")
}