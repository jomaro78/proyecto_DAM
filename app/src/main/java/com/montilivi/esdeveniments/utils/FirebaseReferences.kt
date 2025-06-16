package com.montilivi.esdeveniments.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

object FirebaseReferences {

    // Firebase Auth
    val auth get() = FirebaseAuth.getInstance()

    // Firebase Firestore
    val db get() = FirebaseFirestore.getInstance()

    val usersCollection get() = db.collection("users")
    val eventsCollection get() = db.collection("events")
    val categoriesCollection get() = db.collection("event_categories")
    val userEventSubscriptionsCollection get() = db.collection("user_event_subscriptions")
    val messagesCollection get() = db.collection("messages")
    val chatActivityCollection get() = db.collection("chat_activity")

    // Firebase Storage
    val storage get() = FirebaseStorage.getInstance()

    val profileImagesRef get() = storage.reference.child("profile_images")
    val eventImagesRef get() = storage.reference.child("event_images")
}
