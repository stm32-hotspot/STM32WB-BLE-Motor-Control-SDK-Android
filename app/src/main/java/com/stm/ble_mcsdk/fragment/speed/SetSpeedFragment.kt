package com.stm.ble_mcsdk.fragment.speed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.widget.doAfterTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.stm.ble_mcsdk.MCSDKViewModel
import com.stm.ble_mcsdk.R
import com.stm.ble_mcsdk.ble.BLEManager
import com.stm.ble_mcsdk.databinding.FragmentSetSpeedBinding

class SetSpeedFragment: DialogFragment() {

    private lateinit var binding: FragmentSetSpeedBinding
    private lateinit var viewModel: MCSDKViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_set_speed,
            container,
            false
        )
        binding.fragment = this
        viewModel = BLEManager.viewModel!!

        binding.speedEditText.setText(viewModel.setSpeed.value.toString())
        setEditTextLimits()

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

    // Setup EditText Range
    private fun setEditTextLimits() {
        with (binding) {
            speedEditText.doAfterTextChanged {
                val text = speedEditText.text.toString()
                val max = viewModel.maxSpeed.value ?: 20000
                val min = max * -1

                if (text.isNotEmpty() && text != "-") {
                    if (text.toInt() < min) speedEditText.setText(min.toString())
                    if (text.toInt() > max) speedEditText.setText(max.toString())
                }
            }
        }
    }

    // Set Speed Button Clicked
    fun onButtonClick() {
        val speed = binding.speedEditText.text.toString().toInt()

        if (viewModel.isMotorOn) {
            viewModel.setSpeed(speed)
        } else {
            viewModel.setSpeed.value = speed
        }

        dismiss()
    }

}