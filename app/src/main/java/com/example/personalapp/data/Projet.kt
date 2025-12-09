package com.example.personalapp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "projets",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["userId"])]
)
data class Projet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val durationMinutes: Int,
    val userId: Long,
    val isCompleted: Boolean = false
)
