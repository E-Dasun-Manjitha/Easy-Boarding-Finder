package com.easyboardingfinder.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.easyboardingfinder.R
import com.easyboardingfinder.databinding.ActivityMainBinding
import com.easyboardingfinder.ui.auth.LoginActivity
import com.easyboardingfinder.ui.contact.ContactFragment
import com.easyboardingfinder.ui.furniture.FurnitureFragment
import com.easyboardingfinder.ui.home.HomeFragment
import com.easyboardingfinder.ui.property.PropertyFragment
import com.easyboardingfinder.ui.shareperson.SharePersonFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Cache the user's role in SharedPreferences
        val userId = auth.currentUser?.uid
        if (userId != null) {
            FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val role = document.getString("role") ?: "USER"
                        getSharedPreferences("UserPrefs", MODE_PRIVATE).edit()
                            .putString("user_role", role)
                            .apply()
                    }
                }
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                } else {
                    val currentTabId = binding.bottomNav.selectedItemId
                    if (currentTabId != R.id.nav_home) {
                        navigateToTab(R.id.nav_home)
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            }
        })

        if (savedInstanceState == null) {
            // First time loading - no backstack needed
            loadFragment(HomeFragment(), false)
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_share_person -> SharePersonFragment()
                R.id.nav_furniture -> FurnitureFragment()
                R.id.nav_property -> PropertyFragment()
                R.id.nav_contact -> ContactFragment()
                else -> HomeFragment()
            }
            // Switch tabs - we usually clear history when switching main tabs
            supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            loadFragment(fragment, false)
            true
        }
    }

    /**
     * Loads a fragment.
     * @param addToBackStack Set to true for sub-screens (like Profile) so back gestures work.
     */
    public fun loadFragment(fragment: Fragment, addToBackStack: Boolean = true) {
        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
        
        if (addToBackStack) {
            transaction.addToBackStack(null)
        }
        
        transaction.commit()
    }

    public fun navigateToTab(tabId: Int) {
        binding.bottomNav.selectedItemId = tabId
    }
}
