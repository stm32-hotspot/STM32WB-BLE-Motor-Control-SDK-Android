package com.stm.ble_mcsdk.fragment.speed

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
import com.stm.ble_mcsdk.databinding.FragmentMinmaxSpeedBinding

class MinMaxSpeedFragment: DialogFragment() {

    private lateinit var binding: FragmentMinmaxSpeedBinding
    private lateinit var viewModel: MCSDKViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_minmax_speed,
            container,
            false
        )
        binding.fragment = this
        viewModel = BLEManager.viewModel!!

        binding.speedEditText.setText(viewModel.maxSpeed.value.toString())

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

    // Set Speed Button Clicked
    fun onButtonClick() {
        viewModel.setMinMax(
            binding.speedEditText.text.toString().toInt()
        )
        dismiss()
    }

}