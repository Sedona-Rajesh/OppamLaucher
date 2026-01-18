package com.oppam.oppamlauncher.location

import android.content.Context

/**
 * Sends location via multiple channels (e.g., SMS + Webhook).
 */
class CompositeLocationTransmitter(
    private val transmitters: List<LocationTransmitter>
) : LocationTransmitter {
    override fun sendLocationUpdate(
        context: Context,
        lat: Double,
        lng: Double,
        accuracy: Float,
        timestamp: Long
    ) {
        transmitters.forEach { tx ->
            try {
                tx.sendLocationUpdate(context, lat, lng, accuracy, timestamp)
            } catch (_: Exception) {
                // individual transmitter failures are logged within each implementation
            }
        }
    }
}
