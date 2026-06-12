package com.example.noted

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel

class NoteEditorViewModel : ViewModel() {
    var panOffset by mutableStateOf(Offset.Zero)
    var zoomScale by mutableStateOf(1f)
    var importedElements by mutableStateOf(listOf<NoteElement>())
    
    var isDrawingMode by mutableStateOf(false)
    var currentTool by mutableStateOf("Pen")
    var currentPathPoints by mutableStateOf(listOf<SerializablePathPoint>())
    var strokeColor by mutableStateOf(Color.Black)
    var showToolMenu by mutableStateOf(false)

    var noteTitle by mutableStateOf("")
    var noteContent by mutableStateOf("")
    var reminderTime by mutableStateOf<Long?>(null)
    
    var editingNote by mutableStateOf<Note?>(null)
    var isAddingNote by mutableStateOf(false)

    fun reset() {
        panOffset = Offset.Zero
        zoomScale = 1f
        importedElements = emptyList()
        isDrawingMode = false
        currentTool = "Pen"
        currentPathPoints = emptyList()
        strokeColor = Color.Black
        showToolMenu = false
        noteTitle = ""
        noteContent = ""
        reminderTime = null
        editingNote = null
        isAddingNote = false
    }

    fun loadNote(note: Note) {
        editingNote = note
        noteTitle = note.title
        noteContent = note.content
        importedElements = note.elements
        reminderTime = note.reminderTime
        isAddingNote = false
    }

    fun startNewNote() {
        reset()
        isAddingNote = true
    }
}
