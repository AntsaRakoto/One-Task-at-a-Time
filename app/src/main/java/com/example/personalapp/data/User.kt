package com.example.personalapp.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users", indices = [Index(value = ["userName"], unique = true)])
data class User (
    @ColumnInfo(name = "userName") val userName : String,
    @ColumnInfo(name = "password") val password : String,
    @PrimaryKey(autoGenerate = true) val id : Long = 0
)