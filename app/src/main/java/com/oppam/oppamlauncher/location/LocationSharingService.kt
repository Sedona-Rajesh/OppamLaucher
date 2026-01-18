package com.oppam.oppamlauncher.location

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.IBinder
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.oppam.oppamlauncher.MainActivity
import com.oppam.oppamlauncher.R
import com.oppam.oppamlauncher.status.LocationStatus
import com.oppam.oppamlauncher.util.OppamLogger

/**
 * Foreground service for consent-based, temporary location sharing.
 */
class LocationSharingService : Service() {

    companion object {
        const val CHANNEL_ID = "oppam_location"
        const val NOTIFICATION_ID = 2001
        const val EXTRA_REASON = "reason" // SOS | MANUAL | RISK
        const val EXTRA_DURATION_MS = "duration_ms" // default 30 minutes
        const val DEFAULT_DURATION_MS = 30 * 60 * 1000L

        fun start(context: Context, reason: String, durationMs: Long = DEFAULT_DURATION_MS) {
            val intent = Intent(context, LocationSharingService::class.java).apply {
                putExtra(EXTRA_REASON, reason)
                putExtra(EXTRA_DURATION_MS, durationMs)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LocationSharingService::class.java))
        }
    }

    private lateinit var fusedClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private val handler = Handler(Looper.getMainLooper())
    private var stopRunnable: Runnable? = null
    private val status by lazy { LocationStatus(this) }
    private val transmitter: LocationTransmitter by lazy {
        CompositeLocationTransmitter(
            listOf(
                SMSLocationTransmitter(),
                WebhookLocationTransmitter()
            )
        )
    }
    private var hasSentOnce: Boolean = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        OppamLogger.logEvent(this, "Location sharing service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val reason = intent?.getStringExtra(EXTRA_REASON) ?: "MANUAL"
        val durationMs = intent?.getLongExtra(EXTRA_DURATION_MS, DEFAULT_DURATION_MS) ?: DEFAULT_DURATION_MS
        startForeground(NOTIFICATION_ID, buildNotification())
        OppamLogger.logEvent(this, "Location sharing started. Reason=$reason, duration=${durationMs}ms")

        // Schedule stop
        stopRunnable = Runnable { stopSharing("TIME_LIMIT_EXPIRED") }
        handler.postDelayed(stopRunnable!!, durationMs)

        startLocationUpdates()
        return START_STICKY
    }

    override fun onDestroy() {
        stopLocationUpdates()
        stopRunnable?.let { handler.removeCallbacks(it) }
        OppamLogger.logEvent(this, "Location sharing service destroyed")
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val pi = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Oppam is sharing your location with your family")
            .setContentText("Tap to manage sharing")
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Oppam Location Sharing",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Visible when location sharing is active"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun startLocationUpdates() {
        if (!hasLocationPermission()) {
            OppamLogger.w("Missing location permission; stopping service")
            stopSelf()
            return
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 20_000L)
            .setMinUpdateIntervalMillis(15_000L)
            .setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            .setWaitForAccurateLocation(false)
            .build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val lat = loc.latitude
                val lng = loc.longitude
                val acc = loc.accuracy
                val ts = System.currentTimeMillis()

                // Cache last location locally (no history)
                status.update(lat, lng, acc, ts)
                OppamLogger.logEvent(this@LocationSharingService, "Location updated: $lat,$lng acc=$acc ts=$ts")

                // Send a ONE-TIME location SMS to caregiver for this sharing session.
                // This avoids continuous SMS spam but still lets caregiver see location.
                if (!hasSentOnce) {
                    hasSentOnce = true
                    transmitter.sendLocationUpdate(this@LocationSharingService, lat, lng, acc, ts)
                }
            }
        }
        fusedClient.requestLocationUpdates(request, locationCallback!!, Looper.getMainLooper())
    }

    private fun stopLocationUpdates() {
        locationCallback?.let { fusedClient.removeLocationUpdates(it) }
        locationCallback = null
    }

    private fun isInternetAvailable(): Boolean {
        val cm = getSystemService(ConnectivityManager::class.java)
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun stopSharing(reason: String) {
        OppamLogger.logEvent(this, "Location sharing stopped. Reason=$reason")
        stopSelf()
    }
}
