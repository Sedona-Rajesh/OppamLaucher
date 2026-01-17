package com.oppam.oppamlauncher.ui.caregiver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.oppam.oppamlauncher.status.LocationStatus

@Composable
fun LocationDashboard(elderName: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val status = remember { LocationStatus(context) }
    val last = status.get()
    val cameraPositionState = rememberCameraPositionState()
    val latLng = if (last != null) LatLng(last.lat, last.lng) else null
    val mapsAvailable = remember {
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Elder: $elderName")
                val text = if (last != null) {
                    val seconds = ((System.currentTimeMillis() - last.timestamp) / 1000).toInt()
                    "Last location: ${last.lat}, ${last.lng} (±${last.accuracy} m) • updated ${seconds}s ago"
                } else {
                    "No location yet"
                }
                Text(text, color = Color.Gray)
                if (last == null) {
                    Text("Location not updating", color = Color(0xFFD32F2F))
                }
            }
        }
        if (mapsAvailable) {
            Box(modifier = Modifier.fillMaxWidth().height(300.dp).background(Color(0xFFECEFF1))) {
                if (latLng != null) {
                    cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                    GoogleMap(cameraPositionState = cameraPositionState) {
                        Marker(state = MarkerState(position = latLng), title = elderName)
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Map ready, waiting for location…")
                    }
                }
            }
        } else {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Map not available", color = Color(0xFFD32F2F))
                    Text(
                        "Google Play services or Maps API key is missing. Location will show as text only.",
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
