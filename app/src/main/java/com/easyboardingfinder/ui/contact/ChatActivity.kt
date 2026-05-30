package com.easyboardingfinder.ui.contact

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.easyboardingfinder.R
import com.easyboardingfinder.data.model.ChatMessage
import com.easyboardingfinder.data.model.User
import com.easyboardingfinder.data.repository.ChatRepository
import com.easyboardingfinder.databinding.ActivityChatBinding
import com.easyboardingfinder.databinding.ItemMessageReceivedBinding
import com.easyboardingfinder.databinding.ItemMessageSentBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val repository = ChatRepository()
    private val auth = FirebaseAuth.getInstance()
    
    private var channelId: String? = null
    private var recipientId: String? = null
    private var recipientName: String? = null
    private var recipientUser: User? = null
    private var currentUser: User? = null
    
    private var listenerRegistration: ListenerRegistration? = null
    private lateinit var messageAdapter: MessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Parse intents
        channelId = intent.getStringExtra("CHANNEL_ID")
        recipientId = intent.getStringExtra("RECIPIENT_ID")
        recipientName = intent.getStringExtra("RECIPIENT_NAME")

        setupToolbar()
        setupRecyclerView()
        setupInputBar()
        loadProfilesAndChannel()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        binding.tvRecipientName.text = recipientName ?: "Chat"
        binding.tvLastSeen.text = "Offline" // Default status or dynamic if available
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(auth.currentUser?.uid ?: "")
        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = messageAdapter
    }

    private fun setupInputBar() {
        // Text watcher to toggle Send / Mic icon
        binding.etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty()) {
                    binding.btnSendOrMic.setImageResource(R.drawable.ic_send)
                } else {
                    binding.btnSendOrMic.setImageResource(R.drawable.ic_mic)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Copy button action: copy if there's text, paste if it's empty
        binding.btnCopyText.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val textToCopy = binding.etMessage.text.toString().trim()
            if (textToCopy.isNotEmpty()) {
                val clip = ClipData.newPlainText("chat_message", textToCopy)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            } else {
                val clipData = clipboard.primaryClip
                if (clipData != null && clipData.itemCount > 0) {
                    val pasteText = clipData.getItemAt(0).text
                    if (!pasteText.isNullOrEmpty()) {
                        binding.etMessage.setText(pasteText)
                        binding.etMessage.setSelection(pasteText.length)
                        Toast.makeText(this, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnAttach.setOnClickListener {
            Toast.makeText(this, "Attachments coming soon!", Toast.LENGTH_SHORT).show()
        }

        binding.btnCamera.setOnClickListener {
            Toast.makeText(this, "Camera capture coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Send or Mic button listener
        binding.btnSendOrMic.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
            } else {
                Toast.makeText(this, "Voice messaging is coming soon!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadProfilesAndChannel() {
        val currentUserId = auth.currentUser?.uid ?: return
        val otherId = recipientId ?: return

        lifecycleScope.launch {
            // Load recipient info
            recipientUser = repository.getUserDetails(otherId)
            recipientUser?.let { user ->
                binding.tvRecipientName.text = user.displayName
                if (user.profileImageUrl.isNotEmpty()) {
                    Glide.with(this@ChatActivity)
                        .load(user.profileImageUrl)
                        .placeholder(R.drawable.ic_person)
                        .circleCrop()
                        .into(binding.ivRecipientAvatar)
                }
            }

            // Load current user info for adapter profile image if needed
            currentUser = repository.getUserDetails(currentUserId)

            // Resolve or create channel ID
            if (channelId.isNullOrEmpty()) {
                channelId = repository.createChatChannel(currentUserId, otherId)
            }
            
            // Start listening for messages
            channelId?.let { cid ->
                startListeningForMessages(cid)
            }
        }
    }

    private fun startListeningForMessages(cid: String) {
        listenerRegistration = repository.listenToMessages(cid) { messages ->
            messageAdapter.setMessages(messages)
            if (messages.isNotEmpty()) {
                binding.rvMessages.smoothScrollToPosition(messages.size - 1)
            }
        }
    }

    private fun sendMessage(text: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val otherId = recipientId ?: return
        val cid = channelId ?: return

        binding.etMessage.text.clear()

        lifecycleScope.launch {
            try {
                repository.sendMessage(cid, currentUserId, otherId, text)
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "Failed to send message: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove()
    }

    // RecyclerView Adapter
    inner class MessageAdapter(private val currentUserId: String) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val messages = mutableListOf<ChatMessage>()
        
        private val VIEW_TYPE_SENT = 1
        private val VIEW_TYPE_RECEIVED = 2

        inner class SentViewHolder(val binding: ItemMessageSentBinding) : RecyclerView.ViewHolder(binding.root)
        inner class ReceivedViewHolder(val binding: ItemMessageReceivedBinding) : RecyclerView.ViewHolder(binding.root)

        override fun getItemViewType(position: Int): Int {
            return if (messages[position].senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == VIEW_TYPE_SENT) {
                val b = ItemMessageSentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                SentViewHolder(b)
            } else {
                val b = ItemMessageReceivedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ReceivedViewHolder(b)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val message = messages[position]
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val timeString = sdf.format(message.timestamp.toDate())

            if (holder is SentViewHolder) {
                holder.binding.tvMessage.text = message.messageText
                holder.binding.tvTime.text = timeString
                
                // Show avatar
                val imgUrl = currentUser?.profileImageUrl ?: ""
                if (imgUrl.isNotEmpty()) {
                    Glide.with(holder.binding.ivAvatar.context)
                        .load(imgUrl)
                        .placeholder(R.drawable.ic_person)
                        .circleCrop()
                        .into(holder.binding.ivAvatar)
                } else {
                    holder.binding.ivAvatar.setImageResource(R.drawable.ic_person)
                }
            } else if (holder is ReceivedViewHolder) {
                holder.binding.tvMessage.text = message.messageText
                holder.binding.tvTime.text = timeString
                
                // Show avatar
                val imgUrl = recipientUser?.profileImageUrl ?: ""
                if (imgUrl.isNotEmpty()) {
                    Glide.with(holder.binding.ivAvatar.context)
                        .load(imgUrl)
                        .placeholder(R.drawable.ic_person)
                        .circleCrop()
                        .into(holder.binding.ivAvatar)
                } else {
                    holder.binding.ivAvatar.setImageResource(R.drawable.ic_person)
                }
            }
        }

        override fun getItemCount(): Int = messages.size

        fun setMessages(newMessages: List<ChatMessage>) {
            messages.clear()
            messages.addAll(newMessages)
            notifyDataSetChanged()
        }
    }
}
