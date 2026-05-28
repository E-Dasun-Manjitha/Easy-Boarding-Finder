package com.easyboardingfinder.data.model

import com.google.firebase.Timestamp

data class Property(
    var id: String = "",
    val title: String = "",
    val type: String = "",           // Apartment, House, Room, Annex
    val location: String = "",
    val price: Double = 0.0,
    val bedrooms: Int = 0,
    val bathrooms: Int = 0,
    val description: String = "",
    val amenities: String = "",      // Comma-separated: WiFi, Parking, AC, etc.
    val phone: String = "",
    val imageUrl: String = "",
    val imageUrls: List<String> = emptyList(),
    val userId: String = "",
    val ownerName: String = "",
    val createdAt: Timestamp = Timestamp.now()
)
