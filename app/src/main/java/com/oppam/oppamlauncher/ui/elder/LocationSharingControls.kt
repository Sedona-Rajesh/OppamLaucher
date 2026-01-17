package com.oppam.oppamlauncher.ui.elder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.oppam.oppamlauncher.location.LocationSharingService
import com.oppam.oppamlauncher.location.SMSLocationTransmitter
import com.oppam.oppamlauncher.status.LocationStatus

@Composable
fun LocationSharingControls() {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "സ്ഥാനം പങ്കിടൽ (സുരക്ഷ/അപത്ത് സമയത്ത് മാത്രം)",
                fontWeight = FontWeight.Bold
            )
            Text(
                "Oppam നിങ്ങളുടെ സ്ഥാനം നിങ്ങളുടെ കുടുംബത്തോട് ONLY SOS, അപകട സാധ്യത, അല്ലെങ്കിൽ നിങ്ങളുടെ അനുമതിയോടെയാണ് പങ്കിടുക.",
                color = Color(0xFF5F6368)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    if (hasLocationPermission(context)) {
                        LocationSharingService.start(context, reason = "MANUAL")
                        Toast.makeText(context, "Location sharing started", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Allow location permission first", Toast.LENGTH_LONG).show()
                    }
                }) { Text("Share Location") }
                Button(onClick = {
                    LocationSharingService.stop(context)
                    Toast.makeText(context, "Stopped sharing", Toast.LENGTH_SHORT).show()
                }) { Text("Stop Sharing") }
                Button(onClick = {
                    val last = LocationStatus(context).get()
                    if (last != null) {
                        SMSLocationTransmitter().sendLocationUpdate(context, last.lat, last.lng, last.accuracy, last.timestamp)
                        Toast.makeText(context, "Sent last location to caregiver", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "No location yet—start sharing first", Toast.LENGTH_LONG).show()
                    }
                }) { Text("Send Last Fix") }
            }
        }
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}
