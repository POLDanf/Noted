package com.example.noted

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.example.noted.utils.NotificationHelper

class NoteViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NoteRepository
    val allNotes: StateFlow<List<Note>>
    val trashNotes: StateFlow<List<Note>>
    val archivedNotes: StateFlow<List<Note>>
    val favouriteNotes: StateFlow<List<Note>>
    val secureNotes: StateFlow<List<Note>>
    val reminderNotes: StateFlow<List<Note>>

    init {
        // Initialize DB, DAO, and Repo
        val noteDao = AppDatabase.getDatabase(application).noteDao()
        repository = NoteRepository(noteDao)

        // Convert Flow to StateFlow for easy UI consumption
        allNotes = repository.allNotes
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        trashNotes = repository.getTrashNotes
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        archivedNotes = repository.getArchivedNotes
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        favouriteNotes = repository.getFavouriteNotes
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        secureNotes = repository.getSecureNotes
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
            
        reminderNotes = repository.getNotesWithRemindersFlow()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    fun addNote(title: String, content: String, elements: List<NoteElement> = emptyList(), secure: Boolean = false, reminderTime: Long? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val newNote = Note(
                title = title,
                content = content,
                timestamp = System.currentTimeMillis(),
                trash = false,
                favourite = false,
                archived = false,
                secure = secure,
                reminderTime = reminderTime,
                elements = elements
            )
            val id = repository.insert(newNote).toInt()
            
            if (reminderTime != null && reminderTime > System.currentTimeMillis()) {
                NotificationHelper.scheduleReminder(getApplication(), id, title, reminderTime)
            }
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.update(note.copy(timestamp = System.currentTimeMillis()))
            
            // Manage reminder
            val context = getApplication<Application>()
            if (note.reminderTime != null && note.reminderTime > System.currentTimeMillis() && !note.trash) {
                NotificationHelper.scheduleReminder(context, note.id, note.title, note.reminderTime)
            } else {
                NotificationHelper.cancelReminder(context, note.id)
            }
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(note)
        }
    }
}
