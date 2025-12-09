package com.example.personalapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import java.util.List;

@Dao
interface ProjetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(projet: Projet) : Long

    @Query("SELECT * FROM projets")
    suspend fun getAllProjets(): List<Projet>

    @Query("SELECT * FROM projets WHERE id = :id LIMIT 1")
    suspend fun getProjetById(id: Long): Projet?


    @Query("SELECT * FROM projets WHERE userId = :userId")
    suspend fun getProjetsForUser(userId: Int): List<Projet>

    @Query("""
        SELECT p.id as projetId, p.name as projetName, p.durationMinutes as durationMinutes, u.id as userId, u.userName as userName
        FROM projets p
        INNER JOIN users u ON p.userId = u.id
        ORDER BY p.name
    """)
    suspend fun getAllProjetsWithUser(): List<ProjetWithUser>
}
