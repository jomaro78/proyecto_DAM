package com.montilivi.esdeveniments.data.model

data class CategoryRequest(
    val name: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val timestamp: com.google.firebase.Timestamp? = null
)