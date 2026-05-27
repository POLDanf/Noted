package com.example.noted

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    // Use Flow so Room streams changes live to your UI
    @Query("SELECT * FROM notes_table WHERE trash = 0 ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @androidx.room.Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT * FROM notes_table WHERE trash = 1 ORDER BY timestamp DESC")
    fun getTrashNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes_table WHERE favourite = 1 AND trash = 0 ORDER BY timestamp DESC")
    fun getFavouriteNotes(): Flow<List<Note>>

    @Query("SELECT title FROM notes_table WHERE trash = 0 ORDER BY timestamp DESC")
    fun getTitles(): Flow<List<String>>
}