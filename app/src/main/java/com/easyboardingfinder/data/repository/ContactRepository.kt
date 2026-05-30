package com.easyboardingfinder.data.repository

import com.easyboardingfinder.data.model.ContactMessage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class ContactRepository {

    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("contact_messages")

    // CREATE
    suspend fun addMessage(message: ContactMessage): String {
        val docRef = collection.document()
        val messageWithId = message.copy(id = docRef.id)
        docRef.set(messageWithId).await()
        return docRef.id
    }

    // READ - All
    suspend fun getAllMessages(): List<ContactMessage> {
        val snapshot = collection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(ContactMessage::class.java)?.copy(id = doc.id)
        }
    }

    // READ - By User
    suspend fun getMessagesByUser(userId: String): List<ContactMessage> {
        val snapshot = collection
            .whereEqualTo("userId", userId)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(ContactMessage::class.java)?.copy(id = doc.id)
        }.sortedByDescending { it.createdAt }
    }

    // READ - Single
    suspend fun getMessageById(id: String): ContactMessage? {
        val doc = collection.document(id).get().await()
        return doc.toObject(ContactMessage::class.java)?.copy(id = doc.id)
    }

    // UPDATE
    suspend fun updateMessage(message: ContactMessage) {
        collection.document(message.id).set(message).await()
    }

    // DELETE
    suspend fun deleteMessage(id: String) {
        collection.document(id).delete().await()
    }

    // SEARCH
    suspend fun searchMessages(query: String): List<ContactMessage> {
        val all = getAllMessages()
        return all.filter {
            it.subject.contains(query, ignoreCase = true) ||
            it.message.contains(query, ignoreCase = true) ||
            it.name.contains(query, ignoreCase = true)
        }
    }
}
