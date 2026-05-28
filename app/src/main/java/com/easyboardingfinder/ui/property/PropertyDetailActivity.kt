package com.easyboardingfinder.ui.property

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.easyboardingfinder.R
import com.easyboardingfinder.data.model.Property
import com.easyboardingfinder.data.repository.PropertyRepository
import com.easyboardingfinder.databinding.ActivityPropertyDetailBinding
import com.easyboardingfinder.ui.contact.ChatActivity
import com.easyboardingfinder.ui.contact.AddContactActivity
import com.easyboardingfinder.ui.adapter.ImageSlideshowAdapter
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class PropertyDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPropertyDetailBinding
    private val repository = PropertyRepository()
    private val auth = FirebaseAuth.getInstance()
    private var property: Property? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPropertyDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val propertyId = intent.getStringExtra("PROPERTY_ID")
        if (propertyId == null) {
            finish()
            return
        }

        setupToolbar()
        loadPropertyDetails(propertyId)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadPropertyDetails(id: String) {
        lifecycleScope.launch {
            try {
                property = repository.getPropertyById(id)
                property?.let { populateUi(it) }
            } catch (e: Exception) {
                Toast.makeText(this@PropertyDetailActivity, "Error loading details", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun populateUi(property: Property) {
        binding.tvTitle.text = property.title
        binding.tvPrice.text = "Rs. ${property.price} / month"
        binding.tvLocation.text = property.location
        binding.tvDescription.text = property.description
        binding.tvAmenities.text = property.amenities

        val imageUrls = property.imageUrls.ifEmpty { listOf(property.imageUrl) }.filter { it.isNotEmpty() }
        if (imageUrls.isNotEmpty()) {
            binding.viewPagerImages.visibility = View.VISIBLE
            binding.ivPropertyImage.visibility = View.GONE
            val slideshowAdapter = ImageSlideshowAdapter(imageUrls)
            binding.viewPagerImages.adapter = slideshowAdapter

            if (imageUrls.size > 1) {
                binding.tvImageCount.text = "1 / ${imageUrls.size}"
                binding.tvImageCount.visibility = View.VISIBLE
                binding.viewPagerImages.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)
                        binding.tvImageCount.text = "${position + 1} / ${imageUrls.size}"
                    }
                })
            } else {
                binding.tvImageCount.visibility = View.GONE
            }
        } else {
            binding.viewPagerImages.visibility = View.GONE
            binding.ivPropertyImage.visibility = View.VISIBLE
            binding.ivPropertyImage.setImageResource(R.drawable.bg_image_placeholder)
            binding.tvImageCount.visibility = View.GONE
        }

        // If it's the owner viewing, hide the action layout
        if (property.userId == auth.currentUser?.uid) {
            binding.layoutActions.visibility = View.GONE
        }

        binding.btnCallOwner.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:${property.phone}")
            startActivity(intent)
        }

        binding.btnChatSeller.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("RECIPIENT_ID", property.userId)
                putExtra("RECIPIENT_NAME", property.ownerName)
            }
            startActivity(intent)
        }

        binding.btnSendInquiry.setOnClickListener {
            val intent = Intent(this, AddContactActivity::class.java).apply {
                putExtra("RECIPIENT_ID", property.userId)
                putExtra("ITEM_TITLE", property.title)
                putExtra("ITEM_TYPE", "Property")
                putExtra("ITEM_ID", property.id)
            }
            startActivity(intent)
        }
    }
}
