# **KModbus**

<p>
    <img alt="ViewCount" src="https://views.whatilearened.today/views/github/crowforkotlin/KModbus.svg">
</p>

- **Support Rtu、Tcp、Ascii**
- **Built-in polling and reconnection processing mechanism**

```kotlin
repositories { mavenCentral() }

implementation("com.kotlincrow.android.component:KModbus:1.0")

// 此外你还需要在你的项目引入libSerialPort.so,可在Release找到
```

```kotlin
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
```

```kotlin 
fun main() {
    // If you want to write floating point data to a register
    KModbusRtu().buildMasterOutput(
        function = KModbusFunction.WRITE_HOLDING_REGISTERS,
        slaveAddress = 1,
        startAddress = 1,
        count = 2,
        values = 123.789f.toIntArray()
    )
}
```

[Docs Here](https://www.kotlincrow.com/2023/10/07/Modbus/): 