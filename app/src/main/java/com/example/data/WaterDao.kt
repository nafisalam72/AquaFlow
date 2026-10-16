package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WaterEntry): Long

    @Delete
    suspend fun delete(entry: WaterEntry)

    @Query("DELETE FROM water_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM water_entries WHERE localDateString = :dateString ORDER BY utcTimestampMillis DESC")
    fun getEntriesForDate(dateString: String): Flow<List<WaterEntry>>

    @Query("SELECT * FROM water_entries WHERE localDateString = :dateString ORDER BY utcTimestampMillis DESC")
    suspend fun getEntriesForDateSync(dateString: String): List<WaterEntry>

    @Query("SELECT COALESCE(SUM(amountMl), 0) FROM water_entries WHERE localDateString = :dateString")
    fun getTotalForDate(dateString: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(amountMl), 0) FROM water_entries WHERE localDateString = :dateString")
    suspend fun getTotalForDateSync(dateString: String): Int

    @Query("SELECT * FROM water_entries WHERE utcTimestampMillis BETWEEN :startUtcMillis AND :endUtcMillis ORDER BY utcTimestampMillis DESC")
    fun getEntriesBetween(startUtcMillis: Long, endUtcMillis: Long): Flow<List<WaterEntry>>

    @Query("SELECT COALESCE(SUM(amountMl), 0) FROM water_entries WHERE utcTimestampMillis BETWEEN :startUtcMillis AND :endUtcMillis")
    suspend fun getTotalBetween(startUtcMillis: Long, endUtcMillis: Long): Int

    @Query("SELECT * FROM water_entries ORDER BY utcTimestampMillis DESC LIMIT :limit")
    fun getRecentEntries(limit: Int = 50): Flow<List<WaterEntry>>

    @Query("SELECT * FROM water_entries ORDER BY utcTimestampMillis DESC")
    fun getAllEntries(): Flow<List<WaterEntry>>

    @Query("SELECT * FROM water_entries ORDER BY utcTimestampMillis DESC")
    suspend fun getAllEntriesSync(): List<WaterEntry>

    @Query("SELECT localDateString as dateString, SUM(amountMl) as totalMl, COUNT(*) as count FROM water_entries GROUP BY localDateString ORDER BY localDateString DESC LIMIT :limit")
    fun getDailySummaries(limit: Int = 30): Flow<List<DailySummary>>

    @Query("SELECT localDateString as dateString, SUM(amountMl) as totalMl, COUNT(*) as count FROM water_entries GROUP BY localDateString ORDER BY localDateString DESC LIMIT :limit")
    suspend fun getDailySummariesSync(limit: Int = 30): List<DailySummary>

    @Query("DELETE FROM water_entries WHERE localDateString = :dateString")
    suspend fun clearDate(dateString: String)

    @Query("DELETE FROM water_entries")
    suspend fun clearAll()
}
