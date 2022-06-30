package com.stm.ble_mcsdk.fragment.scan

import android.annotation.SuppressLint
import android.bluetooth.le.ScanResult
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.stm.ble_mcsdk.R
import com.stm.ble_mcsdk.ble.BLEManager
import com.stm.ble_mcsdk.databinding.RowScanResultBinding

@SuppressLint("MissingPermission")
class ScanAdapter (
    private val scanResults: List<ScanResult>
) : RecyclerView.Adapter<ScanAdapter.ViewHolder>()  {

    private val resultsCopy: ArrayList<ScanResult> = arrayListOf()

    inner class ViewHolder(val binding: RowScanResultBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                val result = scanResults[adapterPosition]
                BLEManager.connect(result)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DataBindingUtil.inflate<RowScanResultBinding>(
            inflater,
            R.layout.row_scan_result,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val result = scanResults[position]

        with(holder.binding) {
            deviceName.text = result.device.name ?: "Unnamed"
            macAddress.text = result.device.address
            signalStrength.text = "${result.rssi} dBm"
        }
    }

    override fun getItemCount() = scanResults.size

    @SuppressLint("NotifyDataSetChanged")
    fun filter(checkBox: CheckBox) {
        BLEManager.filter = checkBox.isChecked

        if (checkBox.isChecked) {
            resultsCopy.clear()
            resultsCopy.addAll(scanResults)
            BLEManager.scanResults.clear()

            for (result in resultsCopy) {
                if (result.device.name == "MCSDK") {
                    BLEManager.scanResults.add(result)
                }
            }

            notifyDataSetChanged()
        }
    }

}