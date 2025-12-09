package com.example.personalapp.data

data class ProjetWithUser(
    val projetId: Long,
    val projetName: String,
    val durationMinutes: Int,
    val userId: Long,
    val userName: String
)
