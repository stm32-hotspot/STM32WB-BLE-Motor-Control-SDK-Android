package com.stm.ble_mcsdk.fragment.log

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.stm.ble_mcsdk.MCSDKApplication.Companion.app
import com.stm.ble_mcsdk.R
import com.stm.ble_mcsdk.databinding.RowLogBinding
import com.stm.ble_mcsdk.log.Log

class LogAdapter(
    private val logs: List<Log>
): RecyclerView.Adapter<LogAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: RowLogBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DataBindingUtil.inflate<RowLogBinding>(
            inflater,
            R.layout.row_log,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val log = logs[position]

        with(holder.binding) {
            timeStamp.text = log.timeStamp
            action.text = log.action
            result.text = log.result

            dash2.text = if (log.result.isEmpty()) "" else " - "

            result.setTextColor(ContextCompat.getColor(
                app.applicationContext, log.color
            ))
        }
    }

    override fun getItemCount() = logs.size

}