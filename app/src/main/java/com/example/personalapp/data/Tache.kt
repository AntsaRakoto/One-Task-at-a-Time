package com.example.personalapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

@Entity(
    tableName = "taches",
    foreignKeys = [ForeignKey(
        entity = Projet::class,
        parentColumns = ["id"],
        childColumns = ["projectId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Tache(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val durationMinutes: Int,
    val completed: Boolean = false,
    val projectId: Long
)
