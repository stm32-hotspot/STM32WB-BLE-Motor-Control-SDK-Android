package com.stm.ble_mcsdk.databinding

import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView

object DataBinding {

    @JvmStatic
    @BindingAdapter("recyclerViewAdapter")
    fun RecyclerView.bindRecyclerViewAdapter(adapter: RecyclerView.Adapter<*>) {
        this.adapter = adapter
    }

}