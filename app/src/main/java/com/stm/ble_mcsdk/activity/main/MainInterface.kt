package com.stm.ble_mcsdk.activity.main

interface MainInterface {
    fun promptEnableBluetooth()
    fun toggleFunctionality(connected: Boolean)
    fun rotateMotor(value: Int)
    fun setBellColor(color: Int)
    fun updateStatus(status: String)
    fun showToast(message: String)
}