package com.stm.ble_mcsdk.log

import android.os.Handler
import android.os.Looper
import com.stm.ble_mcsdk.R
import com.stm.ble_mcsdk.fragment.log.LogAdapter
import com.stm.ble_mcsdk.fragment.log.LogInterface
import java.util.*

object LogManager {

    val logs = mutableListOf<Log>()
    val logAdapter = LogAdapter(logs)
    var logInterface: LogInterface? = null

    // Create & Add New Log
    fun addLog(action: String, result: String, color: Int) {
        Handler(Looper.getMainLooper()).post {
            logs.add(Log(getTime(), action, result, color))
            logAdapter.notifyItemInserted(logs.size - 1)
            logInterface?.scrollRecyclerView()
        }
    }

    fun addLog(action: String, result: String) {
        addLog(action, result, R.color.grey)
    }

    // Get Current Time
    private fun getTime(): String {
        with(Calendar.getInstance()) {
            val hour = timeFormat(get(Calendar.HOUR_OF_DAY))
            val min = timeFormat(get(Calendar.MINUTE))
            val sec = timeFormat(get(Calendar.SECOND))

            return "$hour:$min:$sec"
        }
    }

    private fun timeFormat(time: Int): String {
        return time.toString().padStart(2, '0')
    }

}