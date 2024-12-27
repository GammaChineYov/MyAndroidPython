// xxx/BluetoothActivity.kt
// 蓝牙页面，主要用于扫描和显示蓝牙设备，并建立连接

package com.example.unimpdemo
import java.nio.charset.Charset // 导入 Charset 类
import android.widget.Spinner
import android.widget.AdapterView

import android.app.AlertDialog
import android.content.DialogInterface
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import android.widget.LinearLayout
import android.widget.TextView

class BluetoothActivity : AppCompatActivity(), CoroutineScope {

    // 创建 Job 对象来管理协程
    private val job = Job()

    // 定义 coroutineContext
    override val coroutineContext = Dispatchers.Main + job

    private lateinit var bluetoothContainer: LinearLayout
    private lateinit var messageContainer: LinearLayout
    private lateinit var receivedMessages: TextView
    private lateinit var inputMessage: EditText
    private lateinit var btnSend: Button

    // 声明变量
    private lateinit var bluetoothAdapter: BluetoothAdapter // 蓝牙适配器
    private lateinit var devicesArrayAdapter: ArrayAdapter<String> // 蓝牙设备列表适配器
    private var bluetoothSocket: BluetoothSocket? = null // 蓝牙套接字

    // 定义统一的编码格式
    private var encoding: Charset = Charset.forName("UTF-8") // 默认编码为 UTF-8

    // 换行符定义，可以是 "\n" 或 "\r\n"
    private var lineSeparator: String = "\n"
    private val receiveTimeout: Long = 1000 // 超时设置为1000毫秒

    private val messageBuffer = StringBuilder() // 二级缓冲区
    // 定义一个 UUID，用于 SPP
    companion object {
        private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothSocket?.close() // 关闭蓝牙套接字
        job.cancel() // Activity 销毁时取消所有协程
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth)

        val scanButton: Button = findViewById(R.id.btn_scan_bluetooth)
        bluetoothContainer = findViewById(R.id.bluetooth_container)
        messageContainer = findViewById(R.id.message_container)
        receivedMessages = findViewById(R.id.received_messages)
        inputMessage = findViewById(R.id.input_message)
        btnSend = findViewById(R.id.btn_send)
        
        val btnStop: Button = findViewById(R.id.btn_stop)
        val btnClear: Button = findViewById(R.id.btn_clear)

        // 在 onCreate 中配置 encodingSpinner
        val encodingSpinner: Spinner = findViewById(R.id.encoding_spinner)
        val encodingOptions = resources.getStringArray(R.array.encoding_options) // 从 XML 获取编码选项
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, encodingOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        encodingSpinner.adapter = spinnerAdapter
        

        // 设置编码格式的选择事件监听器
        encodingSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedEncoding = encodingOptions[position]
                encoding = Charset.forName(selectedEncoding) // 更新编码
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // 默认编码保持不变
            }
        }

        val lineSeparatorSpinner: Spinner = findViewById(R.id.line_separator_spinner) // 下拉框选择换行符

        // 初始化换行符下拉框
        val lineSeparatorOptions = arrayOf("\n", "\r\n")
        val lineSeparatorAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, lineSeparatorOptions)
        lineSeparatorSpinner.adapter = lineSeparatorAdapter

        // 设置换行符下拉框的选择事件
        lineSeparatorSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                lineSeparator = parent.getItemAtPosition(position).toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // 没有选择时的处理逻辑（可以留空）
            }
        }

        // 检查并请求权限
        requestBluetoothPermissions()

        // 初始化蓝牙适配器
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "此设备不支持蓝牙", Toast.LENGTH_SHORT).show()
            finish() // 关闭页面
            return
        }

        // 初始化设备列表
        devicesArrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        val devicesListView: ListView = findViewById(R.id.bluetooth_devices_list)
        devicesListView.adapter = devicesArrayAdapter

        // 设置扫描按钮
        scanButton.setOnClickListener {
            scanBluetoothDevices() // 扫描设备
        }

        // 设置停止按钮的点击事件
        btnStop.setOnClickListener {
            sendData(byteArrayOf(3)) // Ctrl+C 的 ASCII 码为 3
        }

        // 设置清屏按钮的点击事件
        btnClear.setOnClickListener {
            receivedMessages.text = ""
        }



        devicesListView.setOnItemClickListener { parent, view, position, id ->
            val info = devicesArrayAdapter.getItem(position)
            val address = info?.substring(info.length - 17) // 获取设备地址
            if (address != null) {
                connectToDevice(address) // 连接设备
            }
        }

        // 设置发送按钮点击事件
        btnSend.setOnClickListener {
            val message = inputMessage.text.toString()
            if (message.isNotEmpty()) {
                sendData(message) // 发送数据
                inputMessage.text.clear() // 清空输入框
            }
        }
    }

    // 扫描蓝牙设备
    private fun scanBluetoothDevices() {
        // 检查蓝牙是否启用
        if (!bluetoothAdapter.isEnabled) {
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBluetoothIntent, 1)
            return
        }

        // 获取已配对设备
        devicesArrayAdapter.clear()
        val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
        if (pairedDevices.isNotEmpty()) {
            for (device in pairedDevices) {
                devicesArrayAdapter.add("${device.name}\n${device.address}")
            }
        } else {
            Toast.makeText(this, "没有已配对设备", Toast.LENGTH_SHORT).show()
        }
    }

    // 蓝牙启用请求的回调
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                scanBluetoothDevices()
            } else {
                Toast.makeText(this, "蓝牙未启用", Toast.LENGTH_SHORT).show()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT), 2)
            }
        }
    }
    private fun connectToDevice(address: String) {
        launch(Dispatchers.IO) {
            val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(address)
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
                bluetoothSocket?.connect() // 使用安全调用

                // 获取设备的所有 UUID
                val uuids = device.uuids // 获取 UUID 列表
                val uuidInfo = uuids?.joinToString(separator = "\n") { it.uuid.toString() } ?: "没有可用的 UUID"
                
                withContext(Dispatchers.Main) {
                    // 连接成功后，隐藏蓝牙相关组件，显示消息组件
                    bluetoothContainer.visibility = View.GONE
                    messageContainer.visibility = View.VISIBLE // 显示消息相关组件
                    receivedMessages.text = "连接成功\n可用 UUID:\n$uuidInfo" // 显示 UUID 信息
                
                    Toast.makeText(this@BluetoothActivity, "连接成功", Toast.LENGTH_SHORT).show()
                }
                // 连接成功后，可以启动接收数据的协程
                launch { startReceivingData() } // 进入接收数据的协程
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@BluetoothActivity, "连接失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    

// 修改接收数据函数以支持二级缓冲区和超时处理
    private suspend fun startReceivingData() {
        val buffer = ByteArray(1024)
        var bytes: Int

        while (true) {
            val startTime = System.currentTimeMillis()
            val receivedData = StringBuilder()

            while (true) {
                // 检查是否超时
                if (System.currentTimeMillis() - startTime > receiveTimeout) break

                // 尝试读取数据
                try {
                    bytes = bluetoothSocket?.inputStream?.read(buffer) ?: break
                    receivedData.append(String(buffer.copyOf(bytes), encoding)) // 使用最新的编码

                    // 检查换行符
                    if (receivedData.contains(lineSeparator)) {
                        messageBuffer.append(receivedData.toString())
                        break // 找到换行符，退出读取循环
                    }
                } catch (e: IOException) {
                    break // 处理异常或退出循环
                }
            }

            // 将接收到的消息更新到 UI
            withContext(Dispatchers.Main) {
                findViewById<TextView>(R.id.received_messages).append(messageBuffer.toString())
                messageBuffer.clear() // 清空缓冲区以准备下一次接收
            }
        }
    }


// 编码并发送数据// 发送数据的重载函数 - 接受字符串
fun sendData(message: String) {
    // 根据设定的编码格式，将字符串转换为字节数组
    val data = (message + lineSeparator).toByteArray(encoding)
    sendData(data) // 调用 ByteArray 版本的 sendData
}

// 发送数据的重载函数 - 接受字节数组
fun sendData(data: ByteArray) {
    launch(Dispatchers.IO) {
        try {
            bluetoothSocket?.outputStream?.write(data) // 发送数据
            withContext(Dispatchers.Main) {
                Toast.makeText(this@BluetoothActivity, "数据发送成功", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@BluetoothActivity, "发送失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}



}
