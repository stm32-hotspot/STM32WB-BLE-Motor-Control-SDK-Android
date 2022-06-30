package com.stm.ble_mcsdk

import android.widget.CheckBox
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.stm.ble_mcsdk.activity.main.MainInterface
import com.stm.ble_mcsdk.ble.BLEManager
import com.stm.ble_mcsdk.ble.BLEResult
import com.stm.ble_mcsdk.ble.BLETimeoutException
import com.stm.ble_mcsdk.extension.hexCalculateSum
import com.stm.ble_mcsdk.extension.hexToBigEndian
import com.stm.ble_mcsdk.extension.hexToByteArray
import com.stm.ble_mcsdk.extension.hexToLittleEndian
import com.stm.ble_mcsdk.log.LogManager
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

class MCSDKViewModel: ViewModel() {

    lateinit var mainInterface: MainInterface

    private var timer: Timer? = null
    private var isTimerRunning = false

    private var maxCommand = false
    val setSpeed = MutableLiveData(0)
    val getSpeed = MutableLiveData(0)
    val maxSpeed = MutableLiveData(2000)

    var isMotorOn = false
    var isCRCEnabled = false

    /** Motor Speed */

    // Set Motor Speed
    fun setSpeed(value: Int) {
        setSpeed.postValue(value)
        setSpeedMessage(value)
        if (isMotorOn) mainInterface.rotateMotor(value)
    }

    // Set Motor MIN & MAX
    fun setMinMax(max: Int) {
        // Keep Speed within Range
        if (setSpeed.value!! > max) {
            setSpeed(max)
        } else if (setSpeed.value!! < -max) {
            setSpeed(-max)
        }

        maxSpeed.postValue(max)
        LogManager.addLog("MIN/MAX", "(${max * -1}/${max})")
    }

    // Get MAX or Current Motor Speed
    private fun getSpeed(hexValue: String) {
        val uSpeed = hexValue
            .filter { !it.isWhitespace() }
            .substring(6, 14)
            .hexToBigEndian()
            .toUInt(16)
        var speed = uSpeed.toInt()

        // Speed is Negative (Convert 2's Complement)
        if (uSpeed > Int.MAX_VALUE.toUInt()) {
            speed = ((UInt.MAX_VALUE - uSpeed) + 1u).toInt() * -1
        }

        if (maxCommand) {
            if (speed < 0) speed = Int.MAX_VALUE
            setMinMax(speed)
            maxCommand = false
        } else if (isMotorOn) {
            getSpeed.postValue(speed)
        }
    }

    // Write Set Speed Message from Slider
    private fun setSpeedMessage(value: Int) {
        val command = "07"
        val size = "06"

        var speed = if (value < 0) {
            // Negative HEX (2's Complement)
            ((UInt.MAX_VALUE.toLong() + value) + 1).toString(16)
        } else {
            // Positive HEX
            value.toString(16)
        }
        speed = speed.padStart(8, '0').hexToLittleEndian()

        val rampDuration = "0000"

        val hexMessage = command + size + speed + rampDuration
        val crc = calculateCRC(hexMessage)

        BLEManager.scope.launch {
            writeCharacteristic(hexMessage + crc)
            LogManager.addLog("Write SET SPEED", "(${value})")
        }
    }

    // Write Get Speed Message Every X Milliseconds
    fun getSpeedMessage() {
        timer = Timer().apply {
            scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    BLEManager.scope.launch {
                        val message = if (isCRCEnabled) "02011E21" else "02011E"
                        writeCharacteristic(message)
                        LogManager.addLog("Write GET SPEED", "")
                    }
                }
            }, 0, 100)
        }
        isTimerRunning = true
    }

    /** Write Message */

    suspend fun writeCharacteristic(message: String, max: Boolean): BLEResult? {
        // Write is a Get Max Command
        if (max) maxCommand = true

        // P2P Write Characteristic
        val characteristic = BLEManager.getCharacteristic(
            "0000fe40-cc7a-482a-984a-7f2ed5b3e58f",
            "0000fe41-8e22-4541-9d4c-21edae82ed19"
        )
        val byteMessage = message.hexToByteArray()

        if (characteristic != null) {
            try {
                return BLEManager.writeCharacteristic(characteristic, byteMessage)
            } catch (e: BLETimeoutException) {
                Timber.e("BLE Timeout Error - Write")
            }
        }
        return null
    }

    suspend fun writeCharacteristic(message: String): BLEResult? {
        return writeCharacteristic(message, false)
    }

    // Response from Write Command
    fun writeResponse(hexValue: String, error: Boolean) {
        var mHexValue = if (error) "0xF0 XX" else hexValue
        var mError = error
        var mColor = R.color.st_light_blue
        var mEndMessage = ""

        // Check 2nd Hex Value to Determine Response
        with (mainInterface) {
            when (mHexValue.substring(5, 7)) {
                "00" -> setBellColor(R.color.st_light_blue)
                "01" -> {
                    // Check 3rd Hex Value
                    when (mHexValue.substring(8, 10).uppercase()) {
                        "03" -> {
                            mainInterface.showToast("Reading max speed is not allowed.")
                            mEndMessage = " (Read not allowed)"
                            maxCommand = false
                        }
                        "0A" -> {
                            mEndMessage = " (Bad CRC)"
                        }
                    }

                    mError = true
                }
                "04" -> {
                    setBellColor(R.color.st_yellow)
                    mColor = R.color.st_yellow
                    getSpeed(mHexValue)
                }
                else -> {
                    mError = true
                }
            }

            if (mError)  {
                mColor = R.color.st_pink
                setBellColor(R.color.st_pink)
            }
        }

        // Log Response
        mHexValue = hexValue.substring(0, 2) + hexValue.substring(2).uppercase()
        LogManager.addLog("Notify", mHexValue + mEndMessage, mColor)
    }

    /** CRC */

    fun calculateCRC(hexMessage: String): String {
        if (!isCRCEnabled) return ""

        // Calculate Sum of Hex Message
        val sum = hexMessage.hexCalculateSum()

        // Calculate Sum of HighByte + LowByte
        val crc = sum.padStart(4, '0').hexCalculateSum()

        // Return CRC (Truncated to a Byte)
        return crc.substring(crc.length - 2)
    }

    // Check that CRC is Correct
    fun checkCRC(hexMessage: String): Boolean {
        if (!isCRCEnabled) return true

        val message = hexMessage.filter { !it.isWhitespace() }

        val messageWithoutCRC = message.substring(2, message.length - 2)
        val crc = message.substring(message.length - 2)

        return calculateCRC(messageWithoutCRC).uppercase() == crc.uppercase()
    }

    // Enables / Disables CRC Based on CRC Checkbox
    fun toggleCRC(checkBox: CheckBox) {
        isCRCEnabled = checkBox.isChecked
    }

    /** Helper Functions */

    fun stopTimer() {
        if (isTimerRunning) {
            timer?.cancel()
            isTimerRunning = false
        }
    }

}