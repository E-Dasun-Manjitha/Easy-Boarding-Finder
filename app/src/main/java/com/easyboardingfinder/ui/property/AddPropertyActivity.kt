package com.easyboardingfinder.ui.property

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.easyboardingfinder.R
import com.easyboardingfinder.data.model.Property
import com.easyboardingfinder.data.repository.PropertyRepository
import com.easyboardingfinder.data.repository.ImageRepository
import com.easyboardingfinder.databinding.ActivityAddPropertyBinding
import com.easyboardingfinder.ui.main.MapPickerActivity
import com.easyboardingfinder.ui.adapter.SelectedImagesAdapter
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Locale

class AddPropertyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddPropertyBinding
    private val repository = PropertyRepository()
    private val imageRepo = ImageRepository()
    private val auth = FirebaseAuth.getInstance()
    private var editId: String? = null
    private var existingProperty: Property? = null
    private var selectedImageUris = mutableListOf<Uri>()
    private lateinit var imagesAdapter: SelectedImagesAdapter

    private val mapPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val address = result.data?.getStringExtra("address")
            binding.etLocation.setText(address)
        }
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val newUris = mutableListOf<Uri>()
            if (data?.clipData != null) {
                val count = data.clipData!!.itemCount
                for (i in 0 until count) {
                    newUris.add(data.clipData!!.getItemAt(i).uri)
                }
            } else if (data?.data != null) {
                newUris.add(data.data!!)
            }

            if (selectedImageUris.size + newUris.size > 5) {
                Toast.makeText(this, "Maximum 5 photos are allowed", Toast.LENGTH_SHORT).show()
                val allowedCount = 5 - selectedImageUris.size
                for (i in 0 until minOf(allowedCount, newUris.size)) {
                    selectedImageUris.add(newUris[i])
                }
            } else {
                selectedImageUris.addAll(newUris)
            }
            imagesAdapter.notifyDataSetChanged()
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            getCurrentLocation()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPropertyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        editId = intent.getStringExtra("EDIT_ID")

        setupRecyclerView()
        setupSpinners()
        setupClickListeners()

        if (editId != null) {
            binding.tvTitle.text = "Edit Property"
            binding.btnSubmit.text = "Update Property"
            loadExistingData()
        }
    }

    private fun setupRecyclerView() {
        imagesAdapter = SelectedImagesAdapter(selectedImageUris) { position ->
            selectedImageUris.removeAt(position)
            imagesAdapter.notifyDataSetChanged()
        }
        binding.rvSelectedImages.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
            this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false
        )
        binding.rvSelectedImages.adapter = imagesAdapter
    }

    private fun setupSpinners() {
        val types = arrayOf("Select Type", "Apartment", "House", "Room", "Annex", "Studio", "Hostel")
        binding.spinnerType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)
    }

    private fun setupClickListeners() {
        binding.ivBack.setOnClickListener { finish() }

        binding.cardAddPhoto.setOnClickListener {
            if (selectedImageUris.size >= 5) {
                Toast.makeText(this, "Maximum 5 photos are allowed", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            imagePickerLauncher.launch(intent)
        }

        binding.btnSubmit.setOnClickListener {
            if (validateInputs()) {
                saveProperty()
            }
        }

        binding.btnMap.setOnClickListener {
            val intent = Intent(this, MapPickerActivity::class.java)
            mapPickerLauncher.launch(intent)
        }

        binding.btnGps.setOnClickListener {
            checkLocationPermissions()
        }
    }

    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        } else {
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    private fun getCurrentLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val address = getAddressFromLatLng(location.latitude, location.longitude)
                    binding.etLocation.setText(address)
                }
            }
        } catch (e: SecurityException) {}
    }

    private fun getAddressFromLatLng(lat: Double, lng: Double): String {
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            addresses?.get(0)?.getAddressLine(0) ?: "Unknown"
        } catch (e: Exception) { "Unknown" }
    }

    private fun loadExistingData() {
        lifecycleScope.launch {
            try {
                val property = repository.getPropertyById(editId!!)
                property?.let {
                    existingProperty = it
                    binding.etTitle.setText(it.title)
                    binding.etLocation.setText(it.location)
                    binding.etPrice.setText(it.price.toString())
                    binding.etBedrooms.setText(it.bedrooms.toString())
                    binding.etBathrooms.setText(it.bathrooms.toString())
                    binding.etPhone.setText(it.phone)
                    binding.etDescription.setText(it.description)
                    
                    val types = arrayOf("Select Type", "Apartment", "House", "Room", "Annex", "Studio", "Hostel")
                    val typeIndex = types.indexOf(it.type)
                    if (typeIndex >= 0) binding.spinnerType.setSelection(typeIndex)

                    if (it.imageUrls.isNotEmpty()) {
                        selectedImageUris.clear()
                        selectedImageUris.addAll(it.imageUrls.map { url -> Uri.parse(url) })
                        imagesAdapter.notifyDataSetChanged()
                    } else if (it.imageUrl.isNotEmpty()) {
                        selectedImageUris.clear()
                        selectedImageUris.add(Uri.parse(it.imageUrl))
                        imagesAdapter.notifyDataSetChanged()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddPropertyActivity, "Failed to load data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateInputs(): Boolean {
        if (binding.etTitle.text.toString().trim().isEmpty()) {
            binding.etTitle.error = "Title is required"; return false
        }
        if (binding.spinnerType.selectedItemPosition == 0) {
            Toast.makeText(this, "Select type", Toast.LENGTH_SHORT).show(); return false
        }
        if (binding.etLocation.text.toString().trim().isEmpty()) {
            binding.etLocation.error = "Location is required"; return false
        }
        if (binding.etPrice.text.toString().trim().isEmpty()) {
            binding.etPrice.error = "Price required"; return false
        }

        val phone = binding.etPhone.text.toString().trim()
        var cleanedPhone = phone.replace("\\s".toRegex(), "").trim()
        if (cleanedPhone.startsWith("+94")) {
            cleanedPhone = "0" + cleanedPhone.substring(3)
        } else if (cleanedPhone.startsWith("94") && cleanedPhone.length == 11) {
            cleanedPhone = "0" + cleanedPhone.substring(2)
        }
        if (cleanedPhone.length != 10 || !cleanedPhone.all { it.isDigit() }) {
            binding.etPhone.error = "Phone number must be exactly 10 digits"; return false
        }
        binding.etPhone.setText(cleanedPhone)

        return true
    }

    private fun saveProperty() {
        binding.btnSubmit.isEnabled = false
        binding.btnSubmit.text = "Uploading..."

        lifecycleScope.launch {
            try {
                val uploadedUrls = mutableListOf<String>()
                for (uri in selectedImageUris) {
                    if (uri.scheme == "http" || uri.scheme == "https") {
                        // Already uploaded URL
                        uploadedUrls.add(uri.toString())
                    } else {
                        // Upload local uri
                        val uploadedUrl = imageRepo.uploadImage(uri, "properties")
                        uploadedUrls.add(uploadedUrl)
                    }
                }

                val primaryImageUrl = uploadedUrls.firstOrNull() ?: ""

                val property = Property(
                    id = editId ?: "",
                    title = binding.etTitle.text.toString().trim(),
                    type = binding.spinnerType.selectedItem.toString(),
                    location = binding.etLocation.text.toString().trim(),
                    price = binding.etPrice.text.toString().trim().toDoubleOrNull() ?: 0.0,
                    bedrooms = binding.etBedrooms.text.toString().trim().toIntOrNull() ?: 0,
                    bathrooms = binding.etBathrooms.text.toString().trim().toIntOrNull() ?: 0,
                    description = binding.etDescription.text.toString().trim(),
                    phone = binding.etPhone.text.toString().trim(),
                    imageUrl = primaryImageUrl,
                    imageUrls = uploadedUrls,
                    ownerName = auth.currentUser?.displayName ?: "",
                    userId = auth.currentUser?.uid ?: ""
                )

                if (editId != null) {
                    repository.updateProperty(property)
                    Toast.makeText(this@AddPropertyActivity, "Updated!", Toast.LENGTH_SHORT).show()
                } else {
                    repository.addProperty(property)
                    Toast.makeText(this@AddPropertyActivity, "Added!", Toast.LENGTH_SHORT).show()
                }
                finish()
            } catch (e: Exception) {
                binding.btnSubmit.isEnabled = true
                binding.btnSubmit.text = if (editId != null) "Update Property" else "Submit"
                Toast.makeText(this@AddPropertyActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
