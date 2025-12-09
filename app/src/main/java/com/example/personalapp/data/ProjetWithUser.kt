package com.example.personalapp.data

import androidx.room.ColumnInfo

data class ProjetWithUser(
    @ColumnInfo(name = "projetId") val projetId: Long,
    @ColumnInfo(name = "projetName") val projetName: String,
    @ColumnInfo(name = "userName") val userName: String,
    @ColumnInfo(name = "durationMinutes") val durationMinutes: Int,
    @ColumnInfo(name = "userId") val userId: Long,
    @ColumnInfo(name = "isCompleted") val isCompleted: Boolean
)
