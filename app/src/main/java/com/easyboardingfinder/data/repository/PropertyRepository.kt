package com.easyboardingfinder.data.repository

import com.easyboardingfinder.data.model.Property
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class PropertyRepository {

    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("properties")

    // CREATE
    suspend fun addProperty(property: Property): String {
        val docRef = collection.document()
        val propertyWithId = property.copy(id = docRef.id)
        docRef.set(propertyWithId).await()
        return docRef.id
    }

    // READ - All
    suspend fun getAllProperties(): List<Property> {
        val snapshot = collection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Property::class.java)?.copy(id = doc.id)
        }
    }

    // READ - By User
    suspend fun getPropertiesByUser(userId: String): List<Property> {
        val snapshot = collection
            .whereEqualTo("userId", userId)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Property::class.java)?.copy(id = doc.id)
        }.sortedByDescending { it.createdAt }
    }

    // READ - Single
    suspend fun getPropertyById(id: String): Property? {
        val doc = collection.document(id).get().await()
        return doc.toObject(Property::class.java)?.copy(id = doc.id)
    }

    // UPDATE
    suspend fun updateProperty(property: Property) {
        collection.document(property.id).set(property).await()
    }

    // DELETE
    suspend fun deleteProperty(id: String) {
        collection.document(id).delete().await()
    }

    // SEARCH
    suspend fun searchProperties(query: String): List<Property> {
        val all = getAllProperties()
        return all.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.location.contains(query, ignoreCase = true) ||
            it.type.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true)
        }
    }
}
