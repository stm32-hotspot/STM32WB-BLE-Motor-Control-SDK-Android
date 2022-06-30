package com.stm.ble_mcsdk.extension

// Converts Hexadecimal to ASCII
fun String.hexToASCII(): String {
    val charList = chunked(2).map { it.toInt(16).toChar() }
    return String(charList.toCharArray())
}

// Converts Hex String to ByteArray
// https://stackoverflow.com/questions/66613717/kotlin-convert-hex-string-to-bytearray
fun String.hexToByteArray(): ByteArray {
    return chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}

// Flips Hex String to Little Endian
fun String.hexToLittleEndian(): String {
    var littleEndian = ""
    for (i in (length - 1) downTo 0 step 2) {
        littleEndian += (this[i - 1] + this[i].toString())
    }
    return littleEndian
}

// Flips Hex String to Big Endian
fun String.hexToBigEndian(): String {
    return this.hexToLittleEndian()
}

// Calculates Hex-Sum of Hex String
fun String.hexCalculateSum():String  {
    var sum = 0

    for (i in indices step 2) {
        val hex = this[i] + this[i + 1].toString()
        val dec = hex.toInt(16)
        sum += dec
    }

    // Returns Sum in Hex
    return sum.toString(16).padStart(2, '0')
}