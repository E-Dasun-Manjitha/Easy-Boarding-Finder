package com.easyboardingfinder.data.model

import com.google.firebase.Timestamp

data class ChatChannel(
    var channelId: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastSenderId: String = "",
    val lastTimestamp: Timestamp = Timestamp.now(),
    var otherUserName: String = "",
    var otherUserProfileUrl: String = ""
)
