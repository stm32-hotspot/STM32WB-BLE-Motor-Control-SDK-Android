package com.stm.ble_mcsdk.fragment.reset

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.stm.ble_mcsdk.MCSDKViewModel
import com.stm.ble_mcsdk.R
import com.stm.ble_mcsdk.ble.BLEManager
import com.stm.ble_mcsdk.databinding.FragmentResetBinding
import com.stm.ble_mcsdk.log.LogManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ResetFragment: DialogFragment() {

    private lateinit var binding: FragmentResetBinding
    private lateinit var viewModel: MCSDKViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_reset,
            container,
            false
        )

        binding.fragment = this
        viewModel = BLEManager.viewModel!!

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        // Set Fragment Dimensions
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    /** Buttons */

    // Bluetooth Button Clicked
    fun resetBlueTooth() {
        BLEManager.scope.launch {
            viewModel.writeCharacteristic("DEADBEEF42")
            LogManager.addLog("Write RESET", "Bluetooth")
        }
        dismissFragment()
    }

    // Motor Button Clicked
    fun resetMotor() {
        BLEManager.scope.launch {
            // Stop Get Speed Timer
            viewModel.stopTimer()

            delay(200)

            // Turn Off Motor
            viewModel.writeCharacteristic("030102" + viewModel.calculateCRC("030102"))
            LogManager.addLog("Write Motor", "OFF")

            delay(200)

            // Reset Motor
            viewModel.writeCharacteristic("DEADBEEF4D")
            LogManager.addLog("Write RESET", "Motor")
        }
        dismissFragment()
    }

    // Cancel Button Clicked
    fun dismissFragment() {
        dismiss()
    }

}