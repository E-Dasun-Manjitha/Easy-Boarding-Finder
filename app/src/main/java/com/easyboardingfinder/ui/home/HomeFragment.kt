package com.easyboardingfinder.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.easyboardingfinder.R
import com.easyboardingfinder.data.repository.FurnitureRepository
import com.easyboardingfinder.data.repository.PropertyRepository
import com.easyboardingfinder.databinding.FragmentHomeBinding
import com.easyboardingfinder.ui.adapter.FurnitureAdapter
import com.easyboardingfinder.ui.adapter.PropertyAdapter
import com.easyboardingfinder.ui.main.MainActivity
import com.easyboardingfinder.ui.main.ProfileFragment
import com.easyboardingfinder.ui.contact.ChatListActivity
import com.easyboardingfinder.ui.property.PropertyDetailActivity
import com.easyboardingfinder.ui.property.AddPropertyActivity
import com.easyboardingfinder.ui.furniture.FurnitureDetailActivity
import com.easyboardingfinder.ui.furniture.AddFurnitureActivity
import com.bumptech.glide.Glide
import com.easyboardingfinder.data.model.Furniture
import com.easyboardingfinder.data.model.Property
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val propertyRepo = PropertyRepository()
    private val furnitureRepo = FurnitureRepository()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Keep references for search filtering
    private lateinit var propertyAdapter: PropertyAdapter
    private lateinit var furnitureAdapter: FurnitureAdapter
    private var allRecentProperties: List<Property> = emptyList()
    private var allRecentFurniture: List<Furniture> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set greeting — show only first name
        val fullName = auth.currentUser?.displayName ?: "User"
        val firstName = fullName.trim().split(" ").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "User"
        binding.tvGreeting.text = "Welcome, $firstName!"

        loadUserProfileImage()
        setupClickListeners()
        setupSearch()
        loadRecentData()

        // Pull-to-refresh
        binding.swipeRefresh.setColorSchemeResources(R.color.primary_blue)
        binding.swipeRefresh.setOnRefreshListener {
            loadRecentData()
        }
    }

    private fun loadUserProfileImage() {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                if (document.exists() && _binding != null) {
                    val profileImageUrl = document.getString("profileImageUrl") ?: ""
                    if (profileImageUrl.isNotEmpty()) {
                        binding.ivProfile.imageTintList = null
                        Glide.with(this)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.ic_person)
                            .circleCrop()
                            .into(binding.ivProfile)
                    } else {
                        binding.ivProfile.imageTintList = android.content.res.ColorStateList.valueOf(
                            androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white)
                        )
                        binding.ivProfile.setImageResource(R.drawable.ic_person)
                    }
                }
            }
    }

    private fun setupSearch() {
        binding.etHomeSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s?.toString()?.trim() ?: ""
                filterRecentLists(query)
            }
        })
    }

    private fun filterRecentLists(query: String) {
        val filteredProperties = if (query.isEmpty()) allRecentProperties
        else allRecentProperties.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.location.contains(query, ignoreCase = true)
        }
        val filteredFurniture = if (query.isEmpty()) allRecentFurniture
        else allRecentFurniture.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.location.contains(query, ignoreCase = true)
        }
        if (::propertyAdapter.isInitialized) propertyAdapter.updateData(filteredProperties)
        if (::furnitureAdapter.isInitialized) furnitureAdapter.updateData(filteredFurniture)
    }

    override fun onResume() {
        super.onResume()
        loadUserProfileImage()
    }

    private fun setupClickListeners() {
        // NEW: Navigate to Profile
        binding.ivProfile.setOnClickListener {
            (activity as? MainActivity)?.loadFragment(ProfileFragment())
        }

        // NEW: Navigate to Chats list
        binding.ivChat.setOnClickListener {
            startActivity(Intent(requireContext(), ChatListActivity::class.java))
        }

        binding.cardSharePerson.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_share_person)
        }
        binding.cardFurniture.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_furniture)
        }
        binding.cardProperty.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_property)
        }
        binding.cardContact.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_contact)
        }
    }

    private fun loadRecentData() {
        val userId = auth.currentUser?.uid ?: ""

        // Recent Properties
        binding.rvRecentProperties.layoutManager = LinearLayoutManager(requireContext())
        propertyAdapter = PropertyAdapter(
            currentUserId = userId,
            onEditClick = { property ->
                val intent = Intent(requireContext(), AddPropertyActivity::class.java)
                intent.putExtra("EDIT_ID", property.id)
                startActivity(intent)
            },
            onDeleteClick = { property -> confirmDeleteProperty(property) },
            onItemClick = { property ->
                val intent = Intent(requireContext(), PropertyDetailActivity::class.java)
                intent.putExtra("PROPERTY_ID", property.id)
                startActivity(intent)
            }
        )
        binding.rvRecentProperties.adapter = propertyAdapter

        // Recent Furniture
        binding.rvRecentFurniture.layoutManager = LinearLayoutManager(requireContext())
        furnitureAdapter = FurnitureAdapter(
            currentUserId = userId,
            onEditClick = { furniture ->
                val intent = Intent(requireContext(), AddFurnitureActivity::class.java)
                intent.putExtra("EDIT_ID", furniture.id)
                startActivity(intent)
            },
            onDeleteClick = { furniture -> confirmDeleteFurniture(furniture) },
            onItemClick = { furniture ->
                val intent = Intent(requireContext(), FurnitureDetailActivity::class.java)
                intent.putExtra("FURNITURE_ID", furniture.id)
                startActivity(intent)
            }
        )
        binding.rvRecentFurniture.adapter = furnitureAdapter

        val sharedPrefs = requireContext().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE)
        val role = sharedPrefs.getString("user_role", "USER") ?: "USER"
        propertyAdapter.currentUserRole = role
        furnitureAdapter.currentUserRole = role

        lifecycleScope.launch {
            try {
                val properties = propertyRepo.getAllProperties().take(5)
                allRecentProperties = properties
                propertyAdapter.updateData(properties)

                val furniture = furnitureRepo.getAllFurniture().take(5)
                allRecentFurniture = furniture
                furnitureAdapter.updateData(furniture)
            } catch (e: Exception) {
                // Silently handle
            } finally {
                if (_binding != null) binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun confirmDeleteProperty(property: Property) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Property")
            .setMessage("Are you sure you want to delete \"${property.title}\"?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        propertyRepo.deleteProperty(property.id)
                        Toast.makeText(requireContext(), "Deleted successfully", Toast.LENGTH_SHORT).show()
                        loadRecentData()
                    } catch (e: java.lang.Exception) {
                        Toast.makeText(requireContext(), "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteFurniture(furniture: Furniture) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Listing")
            .setMessage("Are you sure you want to delete \"${furniture.title}\"?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        furnitureRepo.deleteFurniture(furniture.id)
                        Toast.makeText(requireContext(), "Deleted successfully", Toast.LENGTH_SHORT).show()
                        loadRecentData()
                    } catch (e: java.lang.Exception) {
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
