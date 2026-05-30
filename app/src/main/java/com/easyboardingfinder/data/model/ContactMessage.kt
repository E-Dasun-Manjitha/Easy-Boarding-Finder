package com.easyboardingfinder.data.model

import com.google.firebase.Timestamp

data class ContactMessage(
    var id: String = "",
    val name: String = "",
    val email: String = "",
    val subject: String = "",
    val message: String = "",
    val phone: String = "",
    val userId: String = "",
    val isRead: Boolean = false,
    val reply: String = "",
    val recipientId: String = "",
    val itemTitle: String = "",
    val itemType: String = "",
    val itemId: String = "",
    val createdAt: Timestamp = Timestamp.now()
)
