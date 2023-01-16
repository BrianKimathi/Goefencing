package com.example.a_track

import android.Manifest
import android.app.AlertDialog
import android.app.PendingIntent
import android.app.PendingIntent.*
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.a_track.databinding.FragmentHomeBinding
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class HomeFragment : Fragment(), OnMapReadyCallback {

    val radius = 500F

    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationManager: LocationManager
    private lateinit var mapView: MapView
    private lateinit var database: FirebaseDatabase
    private lateinit var geoFire: GeoFire
    private lateinit var geofencingClient: GeofencingClient

    //binding
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // permissions
    private val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        locationManager = requireContext().getSystemService(LOCATION_SERVICE) as LocationManager
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .build()
        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        database = FirebaseDatabase.getInstance()
        val geofireRef = database.getReference("locations")

        geoFire = GeoFire(geofireRef)
//        requestLocationUpdates()

    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun createGeofencePendingIntent(): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        return getBroadcast(context, 0, intent, FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)
    }

//    private fun deleteLocation() {
//        geoFire.removeLocation(FirebaseAuth.getInstance().currentUser!!.uid)
//    }

    private fun createGeofence(location: LatLng, key: String){
        val geofence = Geofence.Builder()
            .setRequestId("assets_geofence")
            .setCircularRegion(location.latitude, location.longitude, radius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        val geofenceRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT)
            .addGeofence(geofence)
            .build()

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(requireActivity(), permissions, 101)
            return
        }
        geofencingClient.addGeofences(geofenceRequest, createGeofencePendingIntent())
    }

    private fun requestLocationUpdates() {
        val locationCallback = object : LocationCallback() {
            @RequiresApi(Build.VERSION_CODES.M)
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    // Update UI with location data

                    val latLng = LatLng(location.latitude, location.longitude)
                    val circle = CircleOptions()
                        .center(latLng)
                        .radius(radius.toDouble())
                        .fillColor(Color.argb(64, 0, 0, 255))
                        .strokeColor(Color.GREEN)
                        .strokeWidth(5f)

                    googleMap.clear()
                    googleMap.addCircle(circle)
                    googleMap.addMarker(MarkerOptions().position(latLng).title("Current Location"))
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    val currentLocation = GeoLocation(location.latitude, location.longitude)
                    storeLocationInFirestore(currentLocation)
                }
            }
        }
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(requireActivity(), permissions, 101)
            return
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun storeLocationInFirestore(currentLocation: GeoLocation) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            geoFire.setLocation(user.uid, currentLocation) { key, error ->
                if (error != null) {
//                    Toast.makeText(requireContext(), error.message, Toast.LENGTH_SHORT).show()
                } else {
//                    Toast.makeText(requireContext(), key, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            enableGPS()
        }
    }

    private fun enableGPS() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Enable GPS")
            .setMessage("Please enable GPS to use this feature")
            .setPositiveButton("Enable") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Cancel") { _, _ ->
                // Handle negative button click
            }
        builder.create().show()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(requireActivity(), permissions, 101)
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val geofence = Geofence.Builder()
                        .setRequestId("assets_geofence")
                        .setCircularRegion(location.latitude, location.longitude, radius)
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                        .build()

                    val geofenceRequest = GeofencingRequest.Builder()
                        .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT)
                        .addGeofence(geofence)
                        .build()
                    geofencingClient.addGeofences(geofenceRequest, createGeofencePendingIntent())

                    val latLng = LatLng(location.latitude, location.longitude)
                    val circle = CircleOptions()
                        .center(latLng)
                        .radius(radius.toDouble())
                        .fillColor(Color.argb(64, 0, 0, 255))
                        .strokeColor(Color.GREEN)
                        .strokeWidth(5f)
                    googleMap.addCircle(circle)
                    googleMap.addMarker(MarkerOptions().position(latLng).title("Current Location"))
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

                    val currentLocation = GeoLocation(location.latitude, location.longitude)
                    storeLocationInFirestore(currentLocation)
                }else{
                    requestLocationUpdates()
                }
            }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater,container,false)
        val view = binding.root

        mapView = binding.googleMapsFrag
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        return view
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

//    private fun deleteRecord() = CoroutineScope(Dispatchers.IO).launch {
//        try {
//            firestore.collection("locations").document(FirebaseAuth.getInstance().currentUser!!.uid).delete().await()
//        }catch (e: Exception){
//            withContext(Dispatchers.Main){
//                Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
//            }
//        }
//    }

}