package com.example.noted

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import kotlin.OptIn
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.noted.ui.theme.NotedTheme
import dev.shreyaspatil.capturable.capturable
import dev.shreyaspatil.capturable.controller.CaptureController
import dev.shreyaspatil.capturable.controller.rememberCaptureController
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val captureController = rememberCaptureController()
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            val noteViewModel: NoteViewModel = viewModel()
            val notes by noteViewModel.allNotes.collectAsState()
            val trashNotes by noteViewModel.trashNotes.collectAsState()
            val favouriteNotes by noteViewModel.favouriteNotes.collectAsState()

            var currentView by remember { mutableStateOf("Notes") }
            var isAddingNote by remember { mutableStateOf(false) }
            var editingNote by remember { mutableStateOf<Note?>(null) }
            var noteTitle by remember { mutableStateOf("") }
            var noteContent by remember { mutableStateOf("") }

            var showSharePopup by remember { mutableStateOf(false) }

            var showExportPopup by remember {mutableStateOf(false)}
            var noteToExport by remember { mutableStateOf<Note?>(null) }

            NotedTheme {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = !isAddingNote && editingNote == null,
                    drawerContent = {
                        ModalDrawerSheet {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)

                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize()
                                        .padding(16.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            currentView = "Notes"
                                            scope.launch {
                                                drawerState.close()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (currentView == "Notes") MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                            contentColor = if (currentView == "Notes") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground
                                        ),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Start,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Notes")
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("Notes")
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            currentView = "Favourites"
                                            scope.launch {
                                                drawerState.close()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (currentView == "Favourites") MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                            contentColor = if (currentView == "Favourites") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground
                                        ),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Start,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Star, contentDescription = "Favourites")
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("Favourites")
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.Transparent,
                                            contentColor = MaterialTheme.colorScheme.onBackground
                                        ),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Start,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Lock, contentDescription = "Secure")
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("Secure")
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            currentView = "Trash"
                                            scope.launch{
                                                drawerState.close()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (currentView == "Trash") MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                            contentColor = if (currentView == "Trash") MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onBackground
                                        ),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Start,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Trash")
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("Trash")
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            currentView = "Settings"
                                            scope.launch {
                                                drawerState.close()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (currentView == "Settings") MaterialTheme.colorScheme.tertiaryContainer else Color.Transparent,
                                            contentColor = if (currentView == "Settings") MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onBackground
                                        ),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Start,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("Settings")
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            currentView = "Help & Feedback"
                                            scope.launch{
                                                drawerState.close()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (currentView == "Help & Feedback") MaterialTheme.colorScheme.tertiaryContainer else Color.Transparent,
                                            contentColor = if (currentView == "Help & Feedback") MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onBackground
                                        ),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Start,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Settings, contentDescription = "Help")
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("Help & Feedback")
                                        }
                                    }

                                }
                            }
                        }
                    }
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (!isAddingNote && editingNote == null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = {
                                                scope.launch {
                                                    drawerState.open()
                                                }
                                            }) {
                                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                                            }
                                            Text(
                                                text = currentView,
                                                style = MaterialTheme.typography.titleLarge,
                                                modifier = Modifier.padding(start = 8.dp)
                                            )
                                        }
                                        Row {
                                            IconButton(onClick = {

                                            }) {
                                                Icon(Icons.Default.Search, contentDescription = "Search")
                                            }
                                            if (currentView == "Notes" || currentView == "Favourites") {
                                                IconButton(onClick = {
                                                    isAddingNote = true
                                                    noteTitle = ""
                                                    noteContent = ""
                                                }) {
                                                    Icon(Icons.Default.Add, contentDescription = "Add")
                                                }
                                            }
                                        }
                                    }
                                }

                                val displayNotes = when (currentView) {
                                    "Notes" -> notes
                                    "Favourites" -> favouriteNotes
                                    else -> trashNotes
                                }

                                if (currentView == "Settings") {
                                    SettingsView(modifier = Modifier.fillMaxSize())
                                } else if (currentView == "Help & Feedback") {
                                    HelpFeedbackView(modifier = Modifier.fillMaxSize())
                                } else {
                                    HorizontalDivider()
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(displayNotes) { note ->
                                            Button(
                                                onClick = {
                                                    editingNote = note
                                                    noteTitle = note.title
                                                    noteContent = note.content
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color.Transparent,
                                                    contentColor = MaterialTheme.colorScheme.onBackground
                                                ),
                                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 1.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = note.title,
                                                            style = MaterialTheme.typography.bodyLarge
                                                        )
                                                        if (note.favourite) {
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Icon(
                                                                Icons.Default.Star,
                                                                contentDescription = "Favourite",
                                                                tint = Color(0xFFFFD700),
                                                                modifier = Modifier.height(16.dp)
                                                            )
                                                        }
                                                    }

                                                    var showMenu by remember { mutableStateOf(false) }

                                                    Box {
                                                        IconButton(
                                                            onClick = { showMenu = true }
                                                        ) {
                                                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                                                        }
                                                        DropdownMenu(
                                                            expanded = showMenu,
                                                            onDismissRequest = { showMenu = false }
                                                        ) {
                                                            if (currentView == "Notes") {
                                                                DropdownMenuItem(
                                                                    text = { Text("Move to Trash") },
                                                                    leadingIcon = {
                                                                        Icon(
                                                                            Icons.Default.Delete,
                                                                            contentDescription = "Trash"
                                                                        )
                                                                    },
                                                                    onClick = {
                                                                        noteViewModel.updateNote(note.copy(trash = true))
                                                                        showMenu = false
                                                                    }
                                                                )
                                                            } else {
                                                                DropdownMenuItem(
                                                                    text = { Text("Restore") },
                                                                    leadingIcon = {
                                                                        Icon(
                                                                            Icons.Default.Refresh,
                                                                            contentDescription = "Restore"
                                                                        )
                                                                    },
                                                                    onClick = {
                                                                        noteViewModel.updateNote(note.copy(trash = false))
                                                                        showMenu = false
                                                                    }
                                                                )
                                                                DropdownMenuItem(
                                                                    text = { Text("Delete Permanently") },
                                                                    leadingIcon = {
                                                                        Icon(
                                                                            Icons.Default.Delete,
                                                                            contentDescription = "Delete"
                                                                        )
                                                                    },
                                                                    onClick = {
                                                                        noteViewModel.deleteNote(note)
                                                                        showMenu = false
                                                                    }
                                                                )
                                                            }
                                                            DropdownMenuItem(
                                                                text = { Text("Share") },
                                                                leadingIcon = {
                                                                    Icon(
                                                                        Icons.Default.Share,
                                                                        contentDescription = "Share"
                                                                    )
                                                                },
                                                                onClick = {
                                                                    showSharePopup = true
                                                                    showMenu = false
                                                                }
                                                            )
                                                            DropdownMenuItem(
                                                                text = { Text(if (note.favourite) "Unstar" else "Star") },
                                                                leadingIcon = {
                                                                    Icon(
                                                                        if (note.favourite) Icons.Default.Star else Icons.Default.Star, // Could use a border icon if unstarred
                                                                        contentDescription = "Star",
                                                                        tint = if (note.favourite) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant
                                                                    )
                                                                },
                                                                onClick = {
                                                                    noteViewModel.updateNote(note.copy(favourite = !note.favourite))
                                                                    showMenu = false
                                                                }
                                                            )
                                                            DropdownMenuItem(
                                                                text = {Text("Export")},
                                                                leadingIcon = {
                                                                    Icon(
                                                                        Icons.Default.PlayArrow,
                                                                        contentDescription = "Export"
                                                                    )
                                                                },
                                                                onClick = {
                                                                    noteToExport = note
                                                                    showExportPopup = true
                                                                    showMenu = false
                                                                }
                                                            )
                                                        }
                                                    }
                                                }

                                            }
                                        }
                                    }
                                }
                            } else {
                                // Add/Edit Note Screen
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(onClick = {
                                            isAddingNote = false
                                            editingNote = null
                                            noteTitle = ""
                                            noteContent = ""
                                        }) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Back"
                                            )
                                        }

                                        IconButton(onClick = {
                                            if (noteTitle.isNotBlank() || noteContent.isNotBlank()) {
                                                val currentNote = editingNote
                                                if (currentNote != null) {
                                                    noteViewModel.updateNote(
                                                        currentNote.copy(
                                                            title = noteTitle,
                                                            content = noteContent
                                                        )
                                                    )
                                                } else {
                                                    noteViewModel.addNote(noteTitle, noteContent)
                                                }
                                            }
                                            isAddingNote = false
                                            editingNote = null
                                            noteTitle = ""
                                            noteContent = ""
                                        }) {
                                            Icon(Icons.Default.Check, contentDescription = "Save")
                                        }
                                    }

                                    TextField(
                                        value = noteTitle,
                                        onValueChange = { noteTitle = it },
                                        placeholder = {
                                            Text(
                                                "Title",
                                                style = MaterialTheme.typography.headlineMedium
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            disabledContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent
                                        ),
                                        textStyle = MaterialTheme.typography.headlineMedium
                                    )

                                    TextField(
                                        value = noteContent,
                                        onValueChange = { noteContent = it },
                                        placeholder = { Text("Note content") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            disabledContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent
                                        ),
                                        textStyle = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (showSharePopup) {
                Popup(
                    alignment = Alignment.Center,
                    onDismissRequest = { showSharePopup = false }
                ) {
                    Box(
                        modifier = Modifier
                            .width(240.dp)
                            .shadow(8.dp, MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                "Share Note",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Quick Share") },
                                onClick = { showSharePopup = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Share on Apps") },
                                onClick = { showSharePopup = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Friend Code") },
                                onClick = { showSharePopup = false }
                            )
                        }
                    }
                }
            }
            if (showExportPopup) {
                Box(
                    modifier = Modifier
                        .alpha(0f)
                        .width(IntrinsicSize.Max)
                        .capturable(captureController)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    noteToExport?.let { note ->
                        Column(
                            modifier = Modifier.width(300.dp) // Give it a fixed width for the "paper" look
                        ) {
                            Text(
                                text = note.title,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = note.content,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Popup(
                    alignment = Alignment.Center,
                    onDismissRequest = { showExportPopup = false }
                ) {
                    Box(
                        modifier = Modifier
                            .width(240.dp)
                            .shadow(8.dp, MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                "Export Note",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Export as PDF") },
                                onClick = {
                                    scope.launch {
                                        val bitmap = captureController.captureAsync().await()
                                        saveAsPdf(bitmap.asAndroidBitmap(), context, noteToExport?.title ?: "Note")
                                        showExportPopup = false
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as PNG") },
                                onClick = {
                                    scope.launch {
                                        val bitmap = captureController.captureAsync().await()
                                        saveAsPng(bitmap.asAndroidBitmap(), context, noteToExport?.title ?: "Note")
                                        showExportPopup = false
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as Text") },
                                onClick = {
                                    noteToExport?.let { note ->
                                        saveAsText(context, note.title, note.content)
                                    }
                                    showExportPopup = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun saveAsPng(bitmap: Bitmap, context: Context, title: String) {
        val softwareBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }
        val filename = "${title}_${System.currentTimeMillis()}.png"
        val outputStream: OutputStream?

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                outputStream = uri?.let { context.contentResolver.openOutputStream(it) }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val image = File(imagesDir, filename)
                outputStream = FileOutputStream(image)
            }

            outputStream?.use {
                softwareBitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                Toast.makeText(context, "Saved to Pictures", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveAsPdf(bitmap: Bitmap, context: Context, title: String) {
        val softwareBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }
        val filename = "${title}_${System.currentTimeMillis()}.pdf"
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(softwareBitmap.width, softwareBitmap.height, 1).create()
        val page = pdfDocument.startPage(pageInfo)

        val canvas = page.canvas
        canvas.drawBitmap(softwareBitmap, 0f, 0f, null)
        pdfDocument.finishPage(page)

        val outputStream: OutputStream?

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                outputStream = uri?.let { context.contentResolver.openOutputStream(it) }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val pdfFile = File(downloadsDir, filename)
                outputStream = FileOutputStream(pdfFile)
            }

            outputStream?.use {
                pdfDocument.writeTo(it)
                Toast.makeText(context, "Saved to Downloads", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }

    private fun saveAsText(context: Context, title: String, content: String) {
        val filename = "${title}_${System.currentTimeMillis()}.txt"
        val outputStream: OutputStream?

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                outputStream = uri?.let { context.contentResolver.openOutputStream(it) }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val textFile = File(downloadsDir, filename)
                outputStream = FileOutputStream(textFile)
            }

            outputStream?.use {
                it.write(content.toByteArray())
                Toast.makeText(context, "Saved to Downloads", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to save Text file", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun SettingsView(modifier: Modifier = Modifier) {
    HorizontalDivider()
    Column(
        modifier = modifier
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Dark Mode")
            // Add a Switch or other controls here
        }

    }
}

@Composable
fun HelpFeedbackView(modifier: Modifier = Modifier) {
    var feedbackText by remember { mutableStateOf("") }
    val context = LocalContext.current
    HorizontalDivider()
    Column(
        modifier = modifier
            .padding(16.dp)
    ) {

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Tell us what you think or report an issue:",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        TextField(
            value = feedbackText,
            onValueChange = { feedbackText = it },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            placeholder = { Text("Enter your feedback here...") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (feedbackText.isNotBlank()) {
                    Toast.makeText(context, "Feedback submitted! Thank you.", Toast.LENGTH_SHORT).show()
                    feedbackText = ""
                } else {
                    Toast.makeText(context, "Please enter some feedback.", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Submit")
        }
    }
}
