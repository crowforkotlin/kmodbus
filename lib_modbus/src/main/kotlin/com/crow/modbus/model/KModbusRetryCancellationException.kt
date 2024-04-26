package com.crow.modbus.model

import kotlin.coroutines.cancellation.CancellationException

class KModbusRetryCancellationException : CancellationException()