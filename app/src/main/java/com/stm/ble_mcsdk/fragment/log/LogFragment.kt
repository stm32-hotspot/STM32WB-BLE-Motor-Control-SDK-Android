package com.stm.ble_mcsdk.fragment.log

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stm.ble_mcsdk.R
import com.stm.ble_mcsdk.databinding.FragmentLogBinding
import com.stm.ble_mcsdk.log.LogManager

class LogFragment: DialogFragment(), LogInterface {

    private lateinit var binding: FragmentLogBinding
    private var viewCreated = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_log,
            container,
            false
        )

        viewCreated = true
        binding.fragment = this
        setupRecyclerView()

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        scrollRecyclerView()

        // Set Fragment Dimensions
        val width = WindowManager.LayoutParams.MATCH_PARENT
        val height = resources.displayMetrics.heightPixels * 0.8
        dialog?.window?.setLayout(width, height.toInt())
    }

    // Sets Up the Recycler View for BLE Scan List
    private fun setupRecyclerView() {
        binding.logRecyclerView.apply {
            adapter = LogManager.logAdapter
            layoutManager = LinearLayoutManager(
                activity,
                RecyclerView.VERTICAL,
                false
            )
            isNestedScrollingEnabled = false
        }
    }

    // Scroll to Bottom of Recycler View
    override fun scrollRecyclerView() {
        if (viewCreated && binding.scrollCheckBox.isChecked) {
            val position = LogManager.logs.size - 1
            if (position != -1) {
                binding.logRecyclerView.smoothScrollToPosition(position)
            }
        }
    }

}