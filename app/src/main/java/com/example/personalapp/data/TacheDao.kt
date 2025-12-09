package com.example.personalapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TacheDao {

    @Insert
    suspend fun insert(tache: Tache): Long

    @Query("SELECT * FROM taches WHERE projectId = :projectId")
    fun getTachesForProjectFlow(projectId: Long): Flow<List<Tache>>
    @Query("SELECT * FROM taches WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Tache?

    @Query("UPDATE taches SET completed = :completed WHERE id = :id")
    fun updateCompleted(id: Long, completed: Boolean)
    @Update
    suspend fun update(tache: Tache)

    @Delete
    suspend fun delete(tache: Tache)

    @Query("SELECT * FROM taches WHERE projectId = :projectId ORDER BY name")
    suspend fun getTachesForProject(projectId: Long): List<Tache>
}
