package com.stm.ble_mcsdk.fragment.scan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.stm.ble_mcsdk.R
import com.stm.ble_mcsdk.ble.BLEManager
import com.stm.ble_mcsdk.databinding.FragmentScanBinding

class ScanFragment: DialogFragment(), ScanInterface {

    private lateinit var binding: FragmentScanBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_scan,
            container,
            false
        )

        BLEManager.scanInterface = this
        setupRecyclerView()
        BLEManager.startScan()

        binding.filterCheckBox.isChecked = BLEManager.filter

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        // Set Fragment Dimensions
        val width = WindowManager.LayoutParams.MATCH_PARENT
        val height = resources.displayMetrics.heightPixels * 0.8
        dialog?.window?.setLayout(width, height.toInt())
    }

    /** Recycler View */

    // Sets Up the Recycler View for BLE Scan List
    private fun setupRecyclerView() {
        with (binding) {
            adapter = BLEManager.scanAdapter

            scanRecyclerView.apply {
                layoutManager = LinearLayoutManager(
                    activity,
                    RecyclerView.VERTICAL,
                    false
                )
                isNestedScrollingEnabled = false
            }

            // Turns Off Update Animation
            val animator = scanRecyclerView.itemAnimator
            if (animator is SimpleItemAnimator) {
                animator.supportsChangeAnimations = false
            }
        }
    }

    /** Helper Functions */

    override fun dismissFragment() {
        dismiss()
    }

}