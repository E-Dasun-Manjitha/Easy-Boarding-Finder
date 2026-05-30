package com.easyboardingfinder.data.repository

import com.easyboardingfinder.data.model.ChatChannel
import com.easyboardingfinder.data.model.ChatMessage
import com.easyboardingfinder.data.model.User
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class ChatRepository {

    private val db = FirebaseFirestore.getInstance()

    // Create or retrieve channel
    suspend fun createChatChannel(uid1: String, uid2: String): String {
        val channelId = if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
        val docRef = db.collection("chats").document(channelId)
        val doc = docRef.get().await()
        if (!doc.exists()) {
            val channel = ChatChannel(
                channelId = channelId,
                participants = listOf(uid1, uid2),
                lastMessage = "",
                lastSenderId = "",
                lastTimestamp = Timestamp.now()
            )
            docRef.set(channel).await()
        }
        return channelId
    }

    // Send Message
    suspend fun sendMessage(channelId: String, senderId: String, receiverId: String, text: String) {
        val messageRef = db.collection("chats").document(channelId).collection("messages").document()
        val message = ChatMessage(
            id = messageRef.id,
            senderId = senderId,
            receiverId = receiverId,
            messageText = text,
            timestamp = Timestamp.now()
        )
        messageRef.set(message).await()

        // Update channel last message info
        db.collection("chats").document(channelId).update(
            mapOf(
                "lastMessage" to text,
                "lastSenderId" to senderId,
                "lastTimestamp" to Timestamp.now()
            )
        ).await()
    }

    // Real-time Chat Channels Listener
    fun listenToChatChannels(userId: String, onUpdate: (List<ChatChannel>) -> Unit): ListenerRegistration {
        return db.collection("chats")
            .whereArrayContains("participants", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val channels = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ChatChannel::class.java)?.copy(channelId = doc.id)
                }.sortedByDescending { it.lastTimestamp }
                onUpdate(channels)
            }
    }

    // Real-time Messages Listener
    fun listenToMessages(channelId: String, onUpdate: (List<ChatMessage>) -> Unit): ListenerRegistration {
        return db.collection("chats").document(channelId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val messages = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                }
                onUpdate(messages)
            }
    }

    // Retrieve User Details
    suspend fun getUserDetails(userId: String): User? {
        return try {
            val doc = db.collection("users").document(userId).get().await()
            doc.toObject(User::class.java)?.copy(uid = doc.id)
        } catch (e: Exception) {
            null
        }
    }
}
