package com.example.noted

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "notes_table")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val timestamp: Long,
    val trash: Boolean,
    val favourite: Boolean,
    val archived: Boolean = false,
    val secure: Boolean = false,
    val reminderTime: Long? = null,
    val elements: List<NoteElement> = emptyList()
)

data class NoteElement(
    val uri: String? = null,
    val text: String? = null,
    val x: Float,
    val y: Float,
    val height: Float = 0f, // Track height for smart staggering
    val type: String, // "image", "pdf", "text", or "path"
    val pathData: List<SerializablePathPoint>? = null,
    val color: Int = 0 // Stored as Int (ARGB)
)

data class SerializablePathPoint(
    val x: Float,
    val y: Float,
    val isMoveTo: Boolean = false
)

class Converters {
    @TypeConverter
    fun fromNoteElementList(value: List<NoteElement>): String {
        val gson = Gson()
        val type = object : TypeToken<List<NoteElement>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toNoteElementList(value: String): List<NoteElement> {
        val gson = Gson()
        val type = object : TypeToken<List<NoteElement>>() {}.type
        return gson.fromJson(value, type)
    }
}
