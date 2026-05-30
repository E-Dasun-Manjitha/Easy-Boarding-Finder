package com.easyboardingfinder.data.model

import com.google.firebase.Timestamp

data class ChatMessage(
    var id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val messageText: String = "",
    val timestamp: Timestamp = Timestamp.now()
)
