package com.easyboardingfinder.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.easyboardingfinder.R
import com.easyboardingfinder.data.model.Property
import com.easyboardingfinder.databinding.ItemPropertyBinding
import java.text.NumberFormat
import java.util.Locale

class PropertyAdapter(
    private var properties: MutableList<Property> = mutableListOf(),
    private val currentUserId: String = "",
    private val onEditClick: (Property) -> Unit,
    private val onDeleteClick: (Property) -> Unit,
    private val onItemClick: (Property) -> Unit
) : RecyclerView.Adapter<PropertyAdapter.ViewHolder>() {

    var currentUserRole: String = ""
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    inner class ViewHolder(val binding: ItemPropertyBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPropertyBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val property = properties[position]
        val fmt = NumberFormat.getNumberInstance(Locale("en", "LK"))

        holder.binding.apply {
            tvTitle.text = property.title
            tvLocation.text = property.location
            tvPrice.text = "Rs. ${fmt.format(property.price)}"
            tvType.text = property.type
            tvBedrooms.text = "${property.bedrooms} Beds"
            tvBathrooms.text = "${property.bathrooms} Bath"

            // LOAD IMAGE
            if (property.imageUrl.isNotEmpty()) {
                Glide.with(ivPropertyImage.context)
                    .load(property.imageUrl)
                    .placeholder(R.drawable.bg_image_placeholder)
                    .into(ivPropertyImage)
            } else {
                ivPropertyImage.setImageResource(R.drawable.bg_image_placeholder)
            }

            // FAVORITE LOCAL PERSISTENCE
            val context = root.context
            val sharedPrefs = context.getSharedPreferences("PropertyPrefs", Context.MODE_PRIVATE)
            val favKey = "fav_${property.id}"
            var isFav = sharedPrefs.getBoolean(favKey, false)

            ivFavorite.setImageResource(if (isFav) R.drawable.ic_heart_filled else R.drawable.ic_heart_empty)
            ivFavorite.setOnClickListener {
                isFav = !isFav
                sharedPrefs.edit().putBoolean(favKey, isFav).apply()
                ivFavorite.setImageResource(if (isFav) R.drawable.ic_heart_filled else R.drawable.ic_heart_empty)
            }

            val isOwner = property.userId == currentUserId
            val canModify = isOwner || currentUserRole == "ADMIN"
            ivEdit.visibility = if (canModify) android.view.View.VISIBLE else android.view.View.GONE
            ivDelete.visibility = if (canModify) android.view.View.VISIBLE else android.view.View.GONE

            ivEdit.setOnClickListener { onEditClick(property) }
            ivDelete.setOnClickListener { onDeleteClick(property) }
            root.setOnClickListener { onItemClick(property) }
        }
    }

    override fun getItemCount(): Int = properties.size

    fun updateData(newProperties: List<Property>) {
        properties.clear()
        properties.addAll(newProperties)
        notifyDataSetChanged()
    }
}
