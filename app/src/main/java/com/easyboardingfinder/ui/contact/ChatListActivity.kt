package com.easyboardingfinder.ui.contact

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.easyboardingfinder.R
import com.easyboardingfinder.data.model.ChatChannel
import com.easyboardingfinder.data.model.User
import com.easyboardingfinder.data.repository.ChatRepository
import com.easyboardingfinder.databinding.ActivityChatListBinding
import com.easyboardingfinder.databinding.ItemChatChannelBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatListBinding
    private val repository = ChatRepository()
    private val auth = FirebaseAuth.getInstance()
    private var listenerRegistration: ListenerRegistration? = null
    
    private val userCache = mutableMapOf<String, User>()
    private lateinit var channelAdapter: ChannelAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        startListening()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        channelAdapter = ChannelAdapter(
            currentUserId = auth.currentUser?.uid ?: "",
            onChannelClick = { channel ->
                val otherId = channel.participants.firstOrNull { it != auth.currentUser?.uid } ?: ""
                val otherName = userCache[otherId]?.displayName ?: "Chat"
                val intent = Intent(this, ChatActivity::class.java).apply {
                    putExtra("CHANNEL_ID", channel.channelId)
                    putExtra("RECIPIENT_ID", otherId)
                    putExtra("RECIPIENT_NAME", otherName)
                }
                startActivity(intent)
            }
        )
        binding.rvChats.layoutManager = LinearLayoutManager(this)
        binding.rvChats.adapter = channelAdapter
    }

    private fun startListening() {
        val currentUserId = auth.currentUser?.uid ?: return
        listenerRegistration = repository.listenToChatChannels(currentUserId) { channels ->
            channelAdapter.setChannels(channels)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove()
    }

    // Inner adapter for chat channels
    inner class ChannelAdapter(
        private val currentUserId: String,
        private val onChannelClick: (ChatChannel) -> Unit
    ) : RecyclerView.Adapter<ChannelAdapter.ViewHolder>() {

        private val channels = mutableListOf<ChatChannel>()

        inner class ViewHolder(val binding: ItemChatChannelBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemChatChannelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val channel = channels[position]
            val otherId = channel.participants.firstOrNull { it != currentUserId } ?: ""

            // Bind values
            holder.binding.tvLastMessage.text = channel.lastMessage
            
            // Format timestamp
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val date = channel.lastTimestamp.toDate()
            holder.binding.tvTimestamp.text = sdf.format(date)

            // Double check indicator
            if (channel.lastSenderId == currentUserId) {
                holder.binding.ivSentStatus.visibility = View.VISIBLE
            } else {
                holder.binding.ivSentStatus.visibility = View.GONE
            }

            // Load user profile details
            val cachedUser = userCache[otherId]
            if (cachedUser != null) {
                bindUser(holder, cachedUser)
            } else {
                holder.binding.tvName.text = "Loading..."
                holder.binding.ivAvatar.setImageResource(R.drawable.ic_person)
                
                lifecycleScope.launch {
                    val user = repository.getUserDetails(otherId)
                    if (user != null) {
                        userCache[otherId] = user
                        notifyItemChanged(position)
                    } else {
                        holder.binding.tvName.text = "User"
                    }
                }
            }

            holder.binding.root.setOnClickListener { onChannelClick(channel) }
        }

        private fun bindUser(holder: ViewHolder, user: User) {
            holder.binding.tvName.text = user.displayName
            if (user.profileImageUrl.isNotEmpty()) {
                Glide.with(holder.binding.ivAvatar.context)
                    .load(user.profileImageUrl)
                    .placeholder(R.drawable.ic_person)
                    .circleCrop()
                    .into(holder.binding.ivAvatar)
            } else {
                holder.binding.ivAvatar.setImageResource(R.drawable.ic_person)
            }
        }

        override fun getItemCount(): Int = channels.size

        fun setChannels(newChannels: List<ChatChannel>) {
            channels.clear()
            channels.addAll(newChannels)
            notifyDataSetChanged()
        }
    }
}
