package com.example.a_track

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.example.geofence") {
            val geofencingEvent = GeofencingEvent.fromIntent(intent)
            if (geofencingEvent!!.hasError()) {
                Toast.makeText(context, "error", Toast.LENGTH_SHORT).show()
                return
            }

            val transition = geofencingEvent?.geofenceTransition
            if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                // Handle entering a geofence
            } else if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                val notificationBuilder = NotificationCompat.Builder(context, "channel_id")
                    .setSmallIcon(R.drawable.ic_notifications)
                    .setContentTitle("Geofence Exit")
                    .setContentText("An asset has left the geofence.")
                    .setAutoCancel(true)

                notificationManager.notify(0, notificationBuilder.build())
            }
        }
    }
}
