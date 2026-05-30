package com.easyboardingfinder.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.easyboardingfinder.R
import com.easyboardingfinder.data.model.User
import com.easyboardingfinder.databinding.ItemAdminBinding

class AdminAdapter(
    private var admins: MutableList<User> = mutableListOf()
) : RecyclerView.Adapter<AdminAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemAdminBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val admin = admins[position]

        holder.binding.apply {
            tvAdminName.text = if (admin.displayName.isNullOrBlank()) "Admin" else admin.displayName
            tvAdminEmail.text = admin.email

            if (admin.isMainAdmin) {
                tvAdminBadge.text = "Main Admin"
                tvAdminBadge.setTextColor(root.context.getColor(R.color.white))
                tvAdminBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(root.context.getColor(R.color.primary_blue))
            } else {
                tvAdminBadge.text = "Admin"
                tvAdminBadge.setTextColor(root.context.getColor(R.color.primary_blue))
                tvAdminBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#EFF6FF"))
            }

            if (admin.profileImageUrl.isNotEmpty()) {
                Glide.with(root.context)
                    .load(admin.profileImageUrl)
                    .placeholder(R.drawable.ic_person)
                    .circleCrop()
                    .into(ivAdminProfile)
            } else {
                ivAdminProfile.setImageResource(R.drawable.ic_person)
            }
        }
    }

    override fun getItemCount(): Int = admins.size

    fun updateData(newAdmins: List<User>) {
        admins.clear()
        admins.addAll(newAdmins)
        notifyDataSetChanged()
    }
}
