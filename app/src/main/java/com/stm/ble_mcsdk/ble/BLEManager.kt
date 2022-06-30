package com.stm.ble_mcsdk.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.stm.ble_mcsdk.MCSDKApplication.Companion.app
import com.stm.ble_mcsdk.MCSDKViewModel
import com.stm.ble_mcsdk.activity.main.MainInterface
import com.stm.ble_mcsdk.extension.toHexString
import com.stm.ble_mcsdk.fragment.scan.ScanAdapter
import com.stm.ble_mcsdk.fragment.scan.ScanInterface
import com.stm.ble_mcsdk.log.LogManager
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
private const val GATT_MAX_MTU_SIZE = 517
private const val CCC_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb"

@SuppressLint("NotifyDataSetChanged", "MissingPermission")
object BLEManager {

    var viewModel: MCSDKViewModel? = null
    var mainInterface: MainInterface? = null
    var scanInterface: ScanInterface? = null

    // List of BLE Scan Results
    val scanResults = mutableListOf<ScanResult>()

    var bGatt: BluetoothGatt? = null
    val scanAdapter: ScanAdapter = ScanAdapter(scanResults)

    // BLE Queue System (Coroutines)
    private val channel = Channel<BLEResult>()
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var isScanning = false
    var isConnected = false
    var filter = true

    val bAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = app.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val bleScanner: BluetoothLeScanner by lazy {
        bAdapter.bluetoothLeScanner
    }

    /** BLE Scan */

    fun startScan() {
        if (!isScanning) {
            scanResults.clear()
            scanAdapter.notifyDataSetChanged()

            bleScanner.startScan(null, scanSettings, scanCallback)

            isScanning = true
            mainInterface?.updateStatus("Scanning")
            LogManager.addLog("Scan", "Started")
            Timber.i("BLE Scan Started")
        }
    }

    private fun stopScan() {
        if (isScanning) {
            bleScanner.stopScan(scanCallback)
            isScanning = false
            mainInterface?.updateStatus("Scan Stopped")
            LogManager.addLog("Scan", "Stopped")
            Timber.i("BLE Scan Stopped")
        }
    }

    // Scan Settings (Low Latency High Power Usage)
    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    // Scan Result Callback
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val indexQuery = scanResults.indexOfFirst { it.device.address == result.device.address }

            if (indexQuery != -1) { // Updates Existing Scan Result
                scanResults[indexQuery] = result
                scanAdapter.notifyItemChanged(indexQuery)
            } else { // Adds New Scan Result
                with(result.device) {
                    Timber.i("Found BLE device! Name: ${name ?: "Unnamed"}, address: $address")
                }

                // Filter by Device Name if Filter is On
                if (!filter || result.device.name == "MCSDK") {
                    scanResults.add(result)
                    scanAdapter.notifyItemInserted(scanResults.size - 1)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            mainInterface?.updateStatus("Scan Failed")
            LogManager.addLog("Scan", "Failed")
            Timber.e("BLE Scan Failed! Error Code: $errorCode")
        }
    }

    /** BLE Connection */

    // Connects to Scan Result Device
    fun connect(result: ScanResult) {
        if (!isConnected) {
            stopScan()

            with(result.device) {
                connectGatt(app, false, gattCallback)

                mainInterface?.updateStatus("Connecting")
                LogManager.addLog("Connection", "Started")
                Timber.w("Connecting to $address")
            }
        }
    }

    // Disconnects from Device
    fun disconnect() {
        if (isConnected) bGatt?.disconnect()
    }

    // Connection Callback
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device?.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    isConnected = true

                    bGatt = gatt
                    scanInterface?.dismissFragment()
                    mainInterface?.toggleFunctionality(true)

                    Handler(Looper.getMainLooper()).post {
                        bGatt?.discoverServices()
                    }

                    mainInterface?.updateStatus("Connected")
                    LogManager.addLog("Connection", "Successful")
                    Timber.i("Successfully connected to $deviceAddress")
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    isConnected = false
                    mainInterface?.toggleFunctionality(false)
                    gatt.close()

                    mainInterface?.updateStatus("Disconnected")
                    LogManager.addLog("Connection", "Disconnected")
                    Timber.i("Successfully disconnected from $deviceAddress")
                }
            } else {
                isConnected = false
                mainInterface?.toggleFunctionality(false)
                gatt.close()

                mainInterface?.updateStatus("Connection Failed")
                LogManager.addLog("Connection", "Failed")
                Timber.e("Connection Attempt Failed for $deviceAddress! Error: $status")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                Timber.i("Discovered ${services.size} services for ${device.address}")
                printGattTable()

                scope.launch {
                    try {
                        requestMTU(GATT_MAX_MTU_SIZE)
                    } catch (e: BLETimeoutException) {
                        Timber.e("BLE Timeout Error - MTU")
                    }

                    try {
                        // Enable Write Response / Get Speed Notification
                        val notifyCharacteristic = getCharacteristic(
                            "0000fe40-cc7a-482a-984a-7f2ed5b3e58f",
                            "0000fe42-8e22-4541-9d4c-21edae82ed19"
                        )
                        enableNotifications(notifyCharacteristic)
                    } catch (e: BLETimeoutException) {
                        Timber.e("BLE Timeout Error - Notify")
                        disconnect()
                    }
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Timber.i("ATT MTU changed to $mtu, Success: ${status == BluetoothGatt.GATT_SUCCESS}")
            channel.trySendBlocking(BLEResult("MTU", null, status))
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Timber.i("Read characteristic $uuid:\n${value.toHexString()}")
                    }
                    BluetoothGatt.GATT_READ_NOT_PERMITTED -> {
                        Timber.e("Read not permitted for $uuid!")
                    }
                    else -> {
                        Timber.e("Characteristic read failed for $uuid, error: $status")
                    }
                }
            }

            channel.trySendBlocking(BLEResult(characteristic.uuid.toString(), characteristic.value, status))
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Timber.i("Wrote to characteristic ${this.uuid} | value: ${this.value?.toHexString()}")
                    }
                    BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> {
                        Timber.e("Write exceeded connection ATT MTU!")
                    }
                    BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> {
                        Timber.e("Write not permitted for ${this.uuid}!")
                    }
                    else -> {
                        Timber.e("Characteristic write failed for ${this.uuid}, error: $status")
                    }
                }
            }

            channel.trySendBlocking(BLEResult(characteristic.uuid.toString(), characteristic.value, status))
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic
        ) {
            with(characteristic) {
                val hexValue = this.value.toHexString()
                val isCRCCorrect = viewModel?.checkCRC(hexValue)

                viewModel?.writeResponse(hexValue, isCRCCorrect == false)

                Timber.i("Characteristic ${this.uuid} Changed | " +
                    "Value: $hexValue | Is CRC Correct?: $isCRCCorrect"
                )
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            Timber.i("Descriptor Write")
            channel.trySendBlocking(BLEResult(descriptor.uuid.toString(), descriptor.value, status))
        }
    } // End of Connection Callback

    // Prints Available Services & Characteristics from Bluetooth Gatt
    private fun BluetoothGatt.printGattTable() {
        if (services.isEmpty()) {
            Timber.i("No service and characteristic available, call discoverServices() first?")
            return
        }

        services.forEach { service ->
            val characteristicsTable = service.characteristics.joinToString (
                separator = "\n|--",
                prefix = "|--"
            ) { it.uuid.toString() }

            Timber.i("\nService: ${service.uuid}\nCharacteristics:\n$characteristicsTable")
        }
    }

    // Request to Change MTU Size
    suspend fun requestMTU(size: Int):BLEResult {
        bGatt?.requestMtu(size)
        return waitForResult("MTU")
    }

    // Get a Characteristic using Service & Characteristic UUIDs
    fun getCharacteristic(
        serviceUUIDString: String, characteristicUUIDString: String
    ): BluetoothGattCharacteristic? {
        val serviceUUID = UUID.fromString(serviceUUIDString)
        val characteristicUUID = UUID.fromString(characteristicUUIDString)

        return bGatt?.getService(serviceUUID)?.getCharacteristic(characteristicUUID)
    }

    // Read from a Characteristic
    suspend fun readCharacteristic(characteristic: BluetoothGattCharacteristic?): BLEResult {
        if (characteristic!= null && characteristic.isReadable()) {
            bGatt?.readCharacteristic(characteristic)
            return waitForResult(characteristic.uuid.toString())
        } else error("Characteristic ${characteristic?.uuid} cannot be read")
    }

    // Writes to a Characteristic
    suspend fun writeCharacteristic(characteristic: BluetoothGattCharacteristic?, payload: ByteArray): BLEResult {
        val writeType = when {
            characteristic?.isWritable() == true -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            characteristic?.isWritableWithoutResponse() == true -> {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            }
            else -> error("Characteristic ${characteristic?.uuid} cannot be written to")
        }

        bGatt.let { gatt ->
            characteristic.writeType = writeType
            characteristic.value = payload
            gatt?.writeCharacteristic(characteristic)
        }
        return waitForResult(characteristic.uuid.toString())
    }

    /** Notifications / Indications */

    suspend fun enableNotifications(characteristic: BluetoothGattCharacteristic?): BLEResult? {
        val cccdUuid = UUID.fromString(CCC_DESCRIPTOR_UUID)
        val payload = when {
            characteristic?.isIndicatable() == true -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            characteristic?.isNotifiable() == true -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else -> {
                Timber.e("${characteristic?.uuid} doesn't support notifications/indications")
                return null
            }
        }

        characteristic.getDescriptor(cccdUuid)?.let { cccDescriptor ->
            if (bGatt?.setCharacteristicNotification(characteristic, true) == false) {
                Timber.e("setCharacteristicNotification failed for ${characteristic.uuid}")
                return null
            }
            return writeDescriptor(cccDescriptor, payload)
        } ?: Timber.e("${characteristic.uuid} doesn't contain the CCC descriptor!")
        return null
    }

    suspend fun disableNotifications(characteristic: BluetoothGattCharacteristic?): BLEResult? {
        if (characteristic != null) {
            if (!characteristic.isNotifiable() && !characteristic.isIndicatable()) {
                Timber.e("${characteristic.uuid} doesn't support indications/notifications")
                return null
            }
        }

        val cccdUUID = UUID.fromString(CCC_DESCRIPTOR_UUID)
        characteristic?.getDescriptor(cccdUUID)?.let { cccDescriptor ->
            if (bGatt?.setCharacteristicNotification(characteristic, false) == false) {
                Timber.e("setCharacteristicNotification failed for ${characteristic.uuid}")
                return null
            }
            return writeDescriptor(cccDescriptor, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)
        } ?: Timber.e("${characteristic?.uuid} doesn't contain the CCC descriptor")
        return null
    }

    private suspend fun writeDescriptor(descriptor: BluetoothGattDescriptor, payload: ByteArray): BLEResult {
        bGatt?.let { gatt ->
            descriptor.value = payload
            gatt.writeDescriptor(descriptor)
            return waitForResult(descriptor.uuid.toString())
        } ?: error("Not connected to a BLE device!")
    }

    /** Helper Functions */

    fun hasPermissions(context: Context): Boolean {
        return hasLocationPermission(context) && hasBluetoothPermission(context)
    }

    private fun hasLocationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true

        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasBluetoothPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true

        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun BluetoothGattCharacteristic.isReadable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_READ)

    private fun BluetoothGattCharacteristic.isWritable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE)

    private fun BluetoothGattCharacteristic.isWritableWithoutResponse(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)

    private fun BluetoothGattCharacteristic.isIndicatable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_INDICATE)

    private fun BluetoothGattCharacteristic.isNotifiable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_NOTIFY)

    private fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean {
        return properties and property != 0
    }

    // Wait for BLE Operation Result
    private suspend fun waitForResult(id: String): BLEResult {
        return withTimeoutOrNull(TimeUnit.SECONDS.toMillis(5)) {
            var bleResult: BLEResult = channel.receive()
            while (bleResult.id != id) {
                bleResult = channel.receive()
            }
            bleResult
        } ?: run {
            throw BLETimeoutException("BLE Operation Timed Out!")
        }
    }
}