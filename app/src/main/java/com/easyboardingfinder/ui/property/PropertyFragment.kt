package com.easyboardingfinder.ui.property

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.easyboardingfinder.R
import com.easyboardingfinder.data.model.Property
import com.easyboardingfinder.data.repository.PropertyRepository
import com.easyboardingfinder.databinding.FragmentPropertyBinding
import com.easyboardingfinder.ui.adapter.PropertyAdapter
import com.easyboardingfinder.ui.main.MainActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class PropertyFragment : Fragment() {

    private var _binding: FragmentPropertyBinding? = null
    private val binding get() = _binding!!

    private val repository = PropertyRepository()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: PropertyAdapter
    private var allProperties: List<Property> = emptyList()

    // Filter states
    private var filterType: String = "All"
    private var filterBedrooms: String = "Any"
    private var filterBathrooms: String = "Any"
    private var maxPrice: Double? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPropertyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearch()
        setupFilter()
        setupFab()
        loadData()

        binding.btnBack.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_home)
        }

        // Pull-to-refresh
        binding.swipeRefresh.setColorSchemeResources(com.easyboardingfinder.R.color.primary_blue)
        binding.swipeRefresh.setOnRefreshListener {
            loadData()
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun setupRecyclerView() {
        val userId = auth.currentUser?.uid ?: ""
        adapter = PropertyAdapter(
            currentUserId = userId,
            onEditClick = { property ->
                val intent = Intent(requireContext(), AddPropertyActivity::class.java)
                intent.putExtra("EDIT_ID", property.id)
                startActivity(intent)
            },
            onDeleteClick = { property -> confirmDelete(property) },
            onItemClick = { property ->
                val intent = Intent(requireContext(), PropertyDetailActivity::class.java)
                intent.putExtra("PROPERTY_ID", property.id)
                startActivity(intent)
            }
        )
        val sharedPrefs = requireContext().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE)
        val role = sharedPrefs.getString("user_role", "USER") ?: "USER"
        adapter.currentUserRole = role

        binding.rvProperties.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProperties.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                applyFiltersAndSearch()
            }
        })
    }

    private fun setupFilter() {
        binding.btnFilter.setOnClickListener {
            showFilterDialog()
        }
    }

    private fun showFilterDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_filter_property, null)
        val cgType = dialogView.findViewById<com.google.android.material.chip.ChipGroup>(R.id.cgType)
        val cgBedrooms = dialogView.findViewById<com.google.android.material.chip.ChipGroup>(R.id.cgBedrooms)
        val cgBathrooms = dialogView.findViewById<com.google.android.material.chip.ChipGroup>(R.id.cgBathrooms)
        val sliderPrice = dialogView.findViewById<com.google.android.material.slider.Slider>(R.id.sliderPrice)
        val tvPriceValue = dialogView.findViewById<android.widget.TextView>(R.id.tvPriceValue)
        val btnReset = dialogView.findViewById<Button>(R.id.btnReset)
        val btnApply = dialogView.findViewById<Button>(R.id.btnApply)

        // Set type checked
        when (filterType) {
            "All" -> cgType.check(R.id.chipTypeAll)
            "Apartment" -> cgType.check(R.id.chipTypeApartment)
            "House" -> cgType.check(R.id.chipTypeHouse)
            "Room" -> cgType.check(R.id.chipTypeRoom)
            "Annex" -> cgType.check(R.id.chipTypeAnnex)
            "Studio" -> cgType.check(R.id.chipTypeStudio)
            "Hostel" -> cgType.check(R.id.chipTypeHostel)
        }

        // Set bedrooms checked
        when (filterBedrooms) {
            "Any" -> cgBedrooms.check(R.id.chipBedAny)
            "1" -> cgBedrooms.check(R.id.chipBed1)
            "2" -> cgBedrooms.check(R.id.chipBed2)
            "3" -> cgBedrooms.check(R.id.chipBed3)
            "4+" -> cgBedrooms.check(R.id.chipBed4Plus)
        }

        // Set bathrooms checked
        when (filterBathrooms) {
            "Any" -> cgBathrooms.check(R.id.chipBathAny)
            "1" -> cgBathrooms.check(R.id.chipBath1)
            "2" -> cgBathrooms.check(R.id.chipBath2)
            "3+" -> cgBathrooms.check(R.id.chipBath3Plus)
        }

        // Set price
        val initialPrice = maxPrice ?: 150000.0
        sliderPrice.value = initialPrice.toFloat()
        if (maxPrice == null || maxPrice == 150000.0) {
            tvPriceValue.text = "Any price"
        } else {
            tvPriceValue.text = "Rs. ${initialPrice.toInt()}"
        }

        sliderPrice.addOnChangeListener { _, value, _ ->
            if (value >= 150000.0) {
                tvPriceValue.text = "Any price"
            } else {
                tvPriceValue.text = "Rs. ${value.toInt()}"
            }
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        btnReset.setOnClickListener {
            filterType = "All"
            filterBedrooms = "Any"
            filterBathrooms = "Any"
            maxPrice = null
            applyFiltersAndSearch()
            dialog.dismiss()
        }

        btnApply.setOnClickListener {
            // Get selected type
            filterType = when (cgType.checkedChipId) {
                R.id.chipTypeApartment -> "Apartment"
                R.id.chipTypeHouse -> "House"
                R.id.chipTypeRoom -> "Room"
                R.id.chipTypeAnnex -> "Annex"
                R.id.chipTypeStudio -> "Studio"
                R.id.chipTypeHostel -> "Hostel"
                else -> "All"
            }

            // Get selected bedrooms
            filterBedrooms = when (cgBedrooms.checkedChipId) {
                R.id.chipBed1 -> "1"
                R.id.chipBed2 -> "2"
                R.id.chipBed3 -> "3"
                R.id.chipBed4Plus -> "4+"
                else -> "Any"
            }

            // Get selected bathrooms
            filterBathrooms = when (cgBathrooms.checkedChipId) {
                R.id.chipBath1 -> "1"
                R.id.chipBath2 -> "2"
                R.id.chipBath3Plus -> "3+"
                else -> "Any"
            }

            val selectedPrice = sliderPrice.value.toDouble()
            maxPrice = if (selectedPrice >= 150000.0) null else selectedPrice

            applyFiltersAndSearch()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun applyFiltersAndSearch() {
        val query = binding.etSearch.text.toString().trim()
        
        var filtered = allProperties

        // Search filter
        if (query.isNotEmpty()) {
            filtered = filtered.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.location.contains(query, ignoreCase = true)
            }
        }

        // Type filter
        if (filterType != "All") {
            filtered = filtered.filter { it.type == filterType }
        }

        // Bedrooms filter
        if (filterBedrooms != "Any") {
            filtered = when (filterBedrooms) {
                "4+" -> filtered.filter { it.bedrooms >= 4 }
                else -> {
                    val beds = filterBedrooms.toIntOrNull() ?: 0
                    filtered.filter { it.bedrooms == beds }
                }
            }
        }

        // Bathrooms filter
        if (filterBathrooms != "Any") {
            filtered = when (filterBathrooms) {
                "3+" -> filtered.filter { it.bathrooms >= 3 }
                else -> {
                    val baths = filterBathrooms.toIntOrNull() ?: 0
                    filtered.filter { it.bathrooms == baths }
                }
            }
        }

        // Price filter
        maxPrice?.let { max ->
            filtered = filtered.filter { it.price <= max }
        }

        adapter.updateData(filtered)
        binding.emptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            startActivity(Intent(requireContext(), AddPropertyActivity::class.java))
        }
    }

    private fun loadData() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE

        lifecycleScope.launch {
            try {
                allProperties = repository.getAllProperties()
                applyFiltersAndSearch()
                binding.progressBar.visibility = View.GONE
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.emptyState.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                if (_binding != null) binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun confirmDelete(property: Property) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Property")
            .setMessage("Are you sure you want to delete \"${property.title}\"?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        repository.deleteProperty(property.id)
                        Toast.makeText(requireContext(), "Deleted successfully", Toast.LENGTH_SHORT).show()
                        loadData()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
