package com.ivanarroyo.pesito.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {

    @Query("SELECT * FROM alerts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE isActive = 1")
    suspend fun getActive(): List<AlertEntity>

    @Insert
    suspend fun insert(alert: AlertEntity): Long

    @Query("DELETE FROM alerts WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE alerts SET isActive = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)
}
