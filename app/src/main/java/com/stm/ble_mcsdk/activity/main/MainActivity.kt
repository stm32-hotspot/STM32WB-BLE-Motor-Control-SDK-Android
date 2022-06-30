package com.stm.ble_mcsdk.activity.main

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.databinding.DataBindingUtil
import com.stm.ble_mcsdk.MCSDKViewModel
import com.stm.ble_mcsdk.R
import com.stm.ble_mcsdk.activity.intro.IntroActivity
import com.stm.ble_mcsdk.ble.BLEManager
import com.stm.ble_mcsdk.ble.BLEManager.bAdapter
import com.stm.ble_mcsdk.ble.ENABLE_BLUETOOTH_REQUEST_CODE
import com.stm.ble_mcsdk.databinding.ActivityMainBinding
import com.stm.ble_mcsdk.fragment.log.LogFragment
import com.stm.ble_mcsdk.fragment.reset.ResetFragment
import com.stm.ble_mcsdk.fragment.scan.ScanFragment
import com.stm.ble_mcsdk.fragment.speed.MinMaxSpeedFragment
import com.stm.ble_mcsdk.fragment.speed.SetSpeedFragment
import com.stm.ble_mcsdk.log.LogManager
import com.google.android.material.slider.Slider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@SuppressLint("SetTextI18n", "MissingPermission")
class MainActivity : AppCompatActivity(), MainInterface {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MCSDKViewModel by viewModels()
    private val sharedPrefs by lazy { getSharedPreferences("AppIntro", MODE_PRIVATE) }
    private var searchItem: MenuItem? = null

    private val scanFragment = ScanFragment()
    private val logFragment = LogFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this
        binding.main = this
        binding.vm = viewModel
        viewModel.mainInterface = this
        BLEManager.mainInterface = this
        BLEManager.viewModel = viewModel
        LogManager.logInterface = logFragment
        setSupportActionBar(binding.toolbar)

        toggleFunctionality(false)
        updateStatus("Idle")
        slider()
        appIntro()
    }

    override fun onResume() {
        super.onResume()

        if (!bAdapter.isEnabled) {
            promptEnableBluetooth()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        toggleFunctionality(false)
        BLEManager.bGatt?.disconnect()
    }

    // Starts App Introduction if First Time Launching App
    private fun appIntro() {
        if (sharedPrefs.getBoolean("firstLaunch", true)) {
            sharedPrefs.edit { putBoolean("firstLaunch", false) }
            val intent = Intent(this, IntroActivity::class.java)
            startActivity(intent)
        }
    }

    /** Permission & Bluetooth Requests */

    // Prompt to Enable BT
    override fun promptEnableBluetooth() {
        if(!bAdapter.isEnabled){
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            ActivityCompat.startActivityForResult(
                this, enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE, null
            )
        }
    }

    // Request Runtime Permissions (Based on Android Version)
    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissions.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            ))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestMultiplePermissions.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            ))
        }
    }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    // Rerequest Permissions if Not Given by User (Limit 2)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (BLEManager.hasPermissions(this)) {
            scanFragment.show(supportFragmentManager, "scanFragment")
        } else {
            requestPermissions()
        }
    }

    /** Toolbar Menu */

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar, menu)
        searchItem = menu.findItem(R.id.searchItem)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.searchItem -> {
                if (BLEManager.isConnected) {
                    // Toggle Views & Set Speed Before Disconnecting
                    toggleFunctionality(false)
                    BLEManager.disconnect()
                } else {
                    if (!BLEManager.hasPermissions(this)) {
                        requestPermissions()
                        return false
                    }

                    scanFragment.show(supportFragmentManager, "scanFragment")
                }
            }
            R.id.logItem -> {
                logFragment.show(supportFragmentManager, "logFragment")
            }
        }

        return false
    }

    /** Motor Functionality */

    // Motor Image Clicked
    fun onMotorClick() {
        with (viewModel) {
            isMotorOn = !isMotorOn

            if (isMotorOn) {
                BLEManager.scope.launch {
                    // Turn Off Motor
                    writeCharacteristic("030102" + calculateCRC("030102"))
                    LogManager.addLog("Write Motor", "OFF")

                    delay(200)

                    // Turn On Motor
                    writeCharacteristic("030101" + calculateCRC("030101"))
                    LogManager.addLog("Write Motor", "ON")

                    delay(200)

                    // Write Get Max Speed Command
                    writeCharacteristic("02013F" + calculateCRC("02013F"), true)
                    LogManager.addLog("Write GET MAX", "")

                    delay(200)

                    // Write Speed Slider Value
                    setSpeed(binding.speedSlider.value.toInt())

                    delay(200)

                    // Start Get Speed Timer
                    getSpeedMessage()
                }
                setMotorColor(R.attr.colorPrimary)
            } else {
                BLEManager.scope.launch {
                    // Turn Off Motor
                    writeCharacteristic("030102" + calculateCRC("030102"))
                    LogManager.addLog("Write Motor", "OFF")
                }
                setMotorColor(null)
            }

            toggleSpeed(isMotorOn)
        }
    }

    // Rotate Motor Animation
    override fun rotateMotor(value: Int) {
        runOnUiThread {
            // Create Animation
            val rotate = RotateAnimation(
                if (value < 0) 360f else 0f,
                if (value < 0) 0f else 360f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
            )
            rotate.interpolator = LinearInterpolator()
            rotate.repeatCount = -1

            // Calculate Animation Speed (Based on Slider Value)
            val valPercent = abs(value) / viewModel.maxSpeed.value!!.toFloat()
            val speed = 5000

            rotate.duration = when (valPercent) {
                0f -> 0
                1f -> 500
                else -> ((speed + 500) - (valPercent * speed)).toLong()
            }

            // Start Animation
            binding.motorImage.startAnimation(rotate)
        }
    }

    @SuppressLint("RestrictedApi")
    private fun slider() {
        with(binding) {
            speedSlider.addOnSliderTouchListener(object: Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {}

                override fun onStopTrackingTouch(slider: Slider) {
                    if (viewModel.isMotorOn) {
                        viewModel.setSpeed(slider.value.toInt())
                    }
                }
            })

            speedSlider.addOnChangeListener(Slider.OnChangeListener { _, value, _ ->
                viewModel.setSpeed.value = value.toInt()
            })
        }
    }

    // Set Speed Text Clicked
    fun onSetSpeedClick() {
        SetSpeedFragment().show(supportFragmentManager, "setSpeedFragment")
    }

    // Min Max Button Clicked
    fun onMinMaxClick() {
        MinMaxSpeedFragment().show(supportFragmentManager, "minMaxSpeedFragment")
    }

    // ST Logo Clicked
    fun onSTLogoClick() {
        if (BLEManager.isConnected) {
            ResetFragment().show(supportFragmentManager, "resetFragment")
        }
    }

    /** Helper Functions */

    // Toggle Functionality based on BLE Connection
    override fun toggleFunctionality(connected: Boolean) {
        runOnUiThread {
            with(binding) {
                motorImage.isEnabled = connected

                if (connected) {
                    // Connected to BLE Device
                    searchItem?.setIcon(R.drawable.ic_cancel)
                    batteryText.text = "${(80..90).random()}%"
                } else {
                    // Not Connected to BLE Device
                    searchItem?.setIcon(R.drawable.ic_search)
                    setMotorColor(null)
                    viewModel.isMotorOn = false
                    toggleSpeed(false)
                    batteryText.text = ""
                }
            }
        }
    }

    // Toggle Speed Values & Timer
    private fun toggleSpeed(toggle: Boolean) {
        with (viewModel) {
            if (!toggle) {
                stopTimer()
                rotateMotor(0)
                if (setSpeed.value != 0) setSpeed(0)
                if (getSpeed.value != 0) getSpeed.value = 0
            }
        }
    }

    // Set Motor Image Color
    private fun setMotorColor(color: Int?) {
        if (color == null) {
            // Reset Color
            binding.motorImage.setImageResource(R.drawable.ic_settings)
        } else {
            // Change Color to Theme Color
            val value = TypedValue()
            theme.resolveAttribute (color, value, true)
            binding.motorImage.drawable.setTint(
                value.data
            )
        }
    }

    // Set Bell Image Color
    override fun setBellColor(color: Int) {
        runOnUiThread {
            binding.bellImage.drawable.setTint(
                ContextCompat.getColor(
                    this@MainActivity, color
                )
            )

            // Change Color Back After a Few Seconds
            Handler(Looper.getMainLooper()).postDelayed({
                binding.bellImage.drawable.setTint(
                    ContextCompat.getColor(
                        this@MainActivity, R.color.grey
                    )
                )
            }, 1000)
        }
    }

    // Update BLE Status Text
    override fun updateStatus(status: String) {
        runOnUiThread {
            binding.statusText.text = status
        }
    }

    // Show Toast Message
    override fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

}