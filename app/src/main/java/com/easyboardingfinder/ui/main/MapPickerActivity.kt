package com.easyboardingfinder.ui.main

import android.app.Activity
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.easyboardingfinder.R
import com.easyboardingfinder.databinding.ActivityMapPickerBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import java.util.Locale

class MapPickerActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapPickerBinding
    private lateinit var mMap: GoogleMap
    private var selectedLatLng: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.btnConfirmLocation.setOnClickListener {
            selectedLatLng?.let { latLng ->
                val address = getAddressFromLatLng(latLng)
                val resultIntent = Intent().apply {
                    putExtra("address", address)
                    putExtra("lat", latLng.latitude)
                    putExtra("lng", latLng.longitude)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        
        // Default to Sri Lanka
        val defaultLocation = LatLng(6.9271, 79.8612)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f))
        selectedLatLng = defaultLocation

        mMap.setOnCameraIdleListener {
            selectedLatLng = mMap.cameraPosition.target
        }
    }

    private fun getAddressFromLatLng(latLng: LatLng): String {
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                addresses[0].getAddressLine(0) ?: "Unknown Location"
            } else {
                "Unknown Location"
            }
        } catch (e: Exception) {
            "Unknown Location"
        }
    }
}
