package com.example.noted

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {

    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()

    suspend fun insert(note: Note): Long {
        return noteDao.insertNote(note)
    }

    suspend fun update(note: Note) {
        noteDao.updateNote(note)
    }

    suspend fun delete(note: Note) {
        noteDao.deleteNote(note)
    }

    val getTitles: Flow<List<String>> = noteDao.getTitles()

    val getFavouriteNotes: Flow<List<Note>> = noteDao.getFavouriteNotes()

    val getTrashNotes: Flow<List<Note>> = noteDao.getTrashNotes()

    val getArchivedNotes: Flow<List<Note>> = noteDao.getArchivedNotes()

    val getSecureNotes: Flow<List<Note>> = noteDao.getSecureNotes()

    fun getNotesWithRemindersFlow(): Flow<List<Note>> = noteDao.getNotesWithRemindersFlow()
}