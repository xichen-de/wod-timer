package dev.xichen.wodtimer.preset

import androidx.room.withTransaction
import kotlinx.coroutines.flow.map

class PresetRepository(private val database: AppDatabase) {
    private val dao = database.presetDao()
    val presets = dao.observeAll().map { rows -> rows.map(PresetEntity::toDomain) }

    suspend fun save(preset: Preset): Long = database.withTransaction {
        val clean = preset.normalized()
        if (clean.id == 0L) dao.insert(clean.copy(position = dao.nextPosition()).toEntity())
        else {
            dao.update(clean.toEntity())
            clean.id
        }
    }

    suspend fun delete(preset: Preset) = dao.delete(preset.toEntity())

    suspend fun duplicate(preset: Preset): Long = save(
        preset.copy(id = 0, name = "${preset.name} copy", position = 0),
    )

    suspend fun restore(presets: List<Preset>) = database.withTransaction {
        var position = dao.nextPosition()
        presets.forEach { preset ->
            dao.insert(preset.normalized().copy(id = 0, position = position++).toEntity())
        }
    }

    suspend fun move(id: Long, delta: Int) = database.withTransaction {
        val entities = dao.findAllOrdered()
        val from = entities.indexOfFirst { it.id == id }
        if (from < 0) return@withTransaction
        val to = (from + delta).coerceIn(0, entities.lastIndex)
        if (from == to) return@withTransaction
        val reordered = entities.toMutableList().apply { add(to, removeAt(from)) }
        val range = minOf(from, to)..maxOf(from, to)
        range.forEach { index -> dao.setPosition(reordered[index].id, index) }
    }

}
