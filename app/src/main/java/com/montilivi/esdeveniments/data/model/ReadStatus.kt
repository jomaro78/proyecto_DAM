package com.montilivi.esdeveniments.data.model

data class ReadStatus(
    val lastReadMessageId: String = "",
    val timestamp: Long = 0L
)