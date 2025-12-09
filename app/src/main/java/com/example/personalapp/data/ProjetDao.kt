package com.example.personalapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(projet: Projet): Long

    @Query("SELECT * FROM projets")
    suspend fun getAllProjets(): List<Projet>

    @Query("SELECT * FROM projets WHERE id = :id LIMIT 1")
    suspend fun getProjetById(id: Long): Projet?

    @Query("SELECT * FROM projets WHERE userId = :userId AND isCompleted = 0 LIMIT 1")
    suspend fun getActiveProjectForUser(userId: Long): Projet?

    @Query("SELECT COUNT(*) FROM projets WHERE userId = :userId AND isCompleted = 0")
    suspend fun countActiveProjectsForUser(userId: Long): Int

    // Requête corrigée : alias explicites pour matcher ProjetWithUser
    @Query("""
        SELECT 
            p.id AS projetId,
            p.name AS projetName,
            p.durationMinutes AS durationMinutes,
            p.userId AS userId,
            u.userName AS userName,
            p.isCompleted AS isCompleted
        FROM projets p
        INNER JOIN users u ON p.userId = u.id
        WHERE p.userId = :userId AND p.isCompleted = 0
        ORDER BY p.name
    """)
    fun getActiveProjetsWithUser(userId: Long): Flow<List<ProjetWithUser>>

    @Update
    suspend fun update(projet: Projet)

    // Utiliser Long pour userId (cohérent avec Projet.userId: Long)
    @Query("SELECT * FROM projets WHERE userId = :userId")
    suspend fun getProjetsForUser(userId: Long): List<Projet>

    @Query("""
        SELECT 
            p.id AS projetId,
            p.name AS projetName,
            p.durationMinutes AS durationMinutes,
            p.userId AS userId,
            u.userName AS userName,
            p.isCompleted AS isCompleted
        FROM projets p
        INNER JOIN users u ON p.userId = u.id
        ORDER BY p.name
    """)
    suspend fun getAllProjetsWithUser(): List<ProjetWithUser>
}
