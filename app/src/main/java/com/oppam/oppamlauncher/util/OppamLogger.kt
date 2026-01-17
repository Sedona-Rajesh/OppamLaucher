package com.oppam.oppamlauncher.util

import android.content.Context
import android.util.Log

object OppamLogger {
    private const val TAG = "Oppam"

    fun i(msg: String) = Log.i(TAG, msg)
    fun d(msg: String) = Log.d(TAG, msg)
    fun w(msg: String) = Log.w(TAG, msg)
    fun e(msg: String, t: Throwable? = null) = Log.e(TAG, msg, t)

    fun logEvent(context: Context, event: String) {
        // For Play Store compliance, keep lightweight logging. No PII persistence.
        Log.i(TAG, event)
        // Optionally extend to send to backend in future.
    }
}
