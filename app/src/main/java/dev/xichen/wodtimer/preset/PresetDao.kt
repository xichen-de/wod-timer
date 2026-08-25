package dev.xichen.wodtimer.preset

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets ORDER BY position, id")
    fun observeAll(): Flow<List<PresetEntity>>

    @Query("SELECT * FROM presets ORDER BY position, id")
    suspend fun findAllOrdered(): List<PresetEntity>

    @Query("SELECT * FROM presets WHERE id = :id")
    suspend fun find(id: Long): PresetEntity?

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM presets")
    suspend fun nextPosition(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PresetEntity): Long

    @Update suspend fun update(entity: PresetEntity)
    @Delete suspend fun delete(entity: PresetEntity)

    @Query("UPDATE presets SET position = :position WHERE id = :id")
    suspend fun setPosition(id: Long, position: Int)
}

