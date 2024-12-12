package com.ethereallab.fb_todo.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Blob

@Entity(tableName = "Todo")
data class Todo(
    @PrimaryKey(autoGenerate = true) val localId: Int = 0,
    var firestoreId: String? = null,
    val content: String,
    val isDone: Boolean = false,
    val completedDate: Long? = null,
    val userId: String, // author
    val username: String? = "John Doe",
    val song: String? = null,
    val imageUrl: ByteArray? = null,
)
