package com.easyboardingfinder.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.easyboardingfinder.data.model.ContactMessage
import com.easyboardingfinder.databinding.ItemContactMessageBinding
import java.text.SimpleDateFormat
import java.util.Locale

class ContactAdapter(
    private var messages: MutableList<ContactMessage> = mutableListOf(),
    private val currentUserId: String = "",
    private val onEditClick: (ContactMessage) -> Unit,
    private val onDeleteClick: (ContactMessage) -> Unit,
    private val onReplyClick: (ContactMessage) -> Unit,
    private val onItemClick: (ContactMessage) -> Unit
) : RecyclerView.Adapter<ContactAdapter.ViewHolder>() {

    var currentUserRole: String = ""
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    inner class ViewHolder(val binding: ItemContactMessageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemContactMessageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messages[position]
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        holder.binding.apply {
            tvSubject.text = message.subject
            tvMessage.text = message.message
            tvName.text = message.name
            tvDate.text = dateFormat.format(message.createdAt.toDate())

            val isOwner = message.userId == currentUserId
            val canModify = isOwner || currentUserRole == "ADMIN"
            ivEdit.visibility = if (canModify) android.view.View.VISIBLE else android.view.View.GONE
            ivDelete.visibility = if (canModify) android.view.View.VISIBLE else android.view.View.GONE

            // Can reply if recipient is current user (and not owner) OR role is ADMIN
            val canReply = (message.recipientId == currentUserId && !isOwner) || currentUserRole == "ADMIN"
            ivReply.visibility = if (canReply) android.view.View.VISIBLE else android.view.View.GONE

            // Show reply content if it exists
            if (message.reply.isNotEmpty()) {
                layoutReply.visibility = android.view.View.VISIBLE
                tvReply.text = message.reply
            } else {
                layoutReply.visibility = android.view.View.GONE
            }

            ivEdit.setOnClickListener { onEditClick(message) }
            ivDelete.setOnClickListener { onDeleteClick(message) }
            ivReply.setOnClickListener { onReplyClick(message) }
            root.setOnClickListener { onItemClick(message) }
        }
    }

    override fun getItemCount(): Int = messages.size

    fun updateData(newMessages: List<ContactMessage>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }
}
