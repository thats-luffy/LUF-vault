package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultFileDao {
    @Query("SELECT * FROM vault_files ORDER BY addedDate DESC")
    fun getAllFiles(): Flow<List<VaultFile>>

    @Query("SELECT * FROM vault_files WHERE category = :category ORDER BY addedDate DESC")
    fun getFilesByCategory(category: String): Flow<List<VaultFile>>

    @Query("SELECT * FROM vault_files WHERE id = :id")
    suspend fun getFileById(id: Int): VaultFile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: VaultFile): Long

    @Delete
    suspend fun deleteFile(file: VaultFile)
}
