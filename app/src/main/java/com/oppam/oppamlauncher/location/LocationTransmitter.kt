package com.oppam.oppamlauncher.location

import android.content.Context

interface LocationTransmitter {
    fun sendLocationUpdate(
        context: Context,
        lat: Double,
        lng: Double,
        accuracy: Float,
        timestamp: Long
    )
}
