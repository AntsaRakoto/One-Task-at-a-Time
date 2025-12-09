package com.example.personalapp.models

data class TacheStatus(
    val id: Long,
    val name: String,
    val durationMinutes: Int,
    val completed: Boolean
)
