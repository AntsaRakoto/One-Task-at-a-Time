package com.example.personalapp.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert( onConflict = OnConflictStrategy.ABORT)
    suspend fun addUser(user : User) : Long

    @Query("SELECT * FROM users WHERE userName = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): User?


    @Query("SELECT * FROM users ORDER BY id ASC")
    fun readAllData(): Flow<List<User>>

    @Query("SELECT * FROM users ORDER BY id DESC LIMIT 1")
    suspend fun getLastUser(): User?
}