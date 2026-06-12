package com.example.noted

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import kotlin.OptIn
import androidx.biometric.AuthenticationRequest
import androidx.biometric.AuthenticationResult
import androidx.biometric.compose.rememberAuthenticationLauncher
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.noted.ui.theme.NotedTheme
import dev.shreyaspatil.capturable.capturable
import dev.shreyaspatil.capturable.controller.rememberCaptureController
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.geometry.Offset
import com.example.noted.ui.screens.SettingsView
import com.example.noted.ui.screens.HelpFeedbackView
import com.example.noted.ui.components.NoteContent
import com.example.noted.utils.copyUriToInternalStorage
import com.example.noted.utils.saveAsPng
import com.example.noted.utils.saveAsPdf
import com.example.noted.utils.saveAsText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import com.example.noted.utils.NotificationHelper
import androidx.compose.runtime.LaunchedEffect


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current

            LaunchedEffect(Unit) {
                NotificationHelper.createChannels(context)
            }

            val captureController = rememberCaptureController()
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            val noteViewModel: NoteViewModel = viewModel()
            val editorViewModel: NoteEditorViewModel = viewModel()
            val notes by noteViewModel.allNotes.collectAsState()
            val trashNotes by noteViewModel.trashNotes.collectAsState()
            val favouriteNotes by noteViewModel.favouriteNotes.collectAsState()
            val archivedNotes by noteViewModel.archivedNotes.collectAsState()
            val secureNotes by noteViewModel.secureNotes.collectAsState()
            val reminderNotes by noteViewModel.reminderNotes.collectAsState()

            val currentViewState = remember { mutableStateOf("Notes") }
            var currentView by currentViewState

            val systemInDarkTheme = isSystemInDarkTheme()
            var isDarkMode by remember { mutableStateOf(systemInDarkTheme) }
            var showWordCount by remember { mutableStateOf(false) }
            var showCharCount by remember { mutableStateOf(false) }

            val biometricLauncher = rememberAuthenticationLauncher { result ->
                if (result is AuthenticationResult.Success) {
                    currentViewState.value = "Secure"
                    scope.launch { drawerState.close() }
                }
            }

            val filePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument(),
                onResult = { uri ->
                    uri?.let {
                        context.contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        val mimeType = context.contentResolver.getType(it) ?: ""

                        val internalUri = copyUriToInternalStorage(context, it) ?: it.toString()

                        val lastElement = editorViewModel.importedElements.lastOrNull()
                        val startX = -editorViewModel.panOffset.x + 100f
                        val startY = if (lastElement != null) {
                            lastElement.y + (if (lastElement.height > 0) lastElement.height else 200f) + 40f
                        } else {
                            -editorViewModel.panOffset.y + 100f
                        }

                        when {
                            mimeType.contains("pdf") -> {
                                editorViewModel.importedElements = editorViewModel.importedElements + NoteElement(
                                    uri = internalUri,
                                    x = startX,
                                    y = startY,
                                    type = "pdf"
                                )
                            }
                            mimeType.contains("image") -> {
                                editorViewModel.importedElements = editorViewModel.importedElements + NoteElement(
                                    uri = internalUri,
                                    x = startX,
                                    y = startY,
                                    type = "image"
                                )
                            }
                            mimeType.contains("text/plain") -> {
                                try {
                                    val content = context.contentResolver.openInputStream(it)?.use { stream ->
                                        stream.bufferedReader().readText()
                                    } ?: ""
                                    editorViewModel.importedElements = editorViewModel.importedElements + NoteElement(
                                        text = content,
                                        x = startX,
                                        y = startY,
                                        type = "text"
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "Failed to read text file", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            )

            var showSharePopup by remember { mutableStateOf(false) }
            var exportTarget by remember { mutableStateOf<Note?>(null) }

            NotedTheme(darkTheme = isDarkMode) {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = !editorViewModel.isAddingNote && editorViewModel.editingNote == null,
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
                                            scope.launch { drawerState.close() }
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
                                            currentView = "Reminders"
                                            scope.launch { drawerState.close() }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (currentView == "Reminders") MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                            contentColor = if (currentView == "Reminders") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground
                                        ),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Start,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Notifications, contentDescription = "Reminders")
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("Reminders")
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            currentView = "Favourites"
                                            scope.launch { drawerState.close() }
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
                                            currentView = "Archive"
                                            scope.launch { drawerState.close() }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (currentView == "Archive") MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                            contentColor = if (currentView == "Archive") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground
                                        ),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Start,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Archive, contentDescription = "Archive")
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("Archive")
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            val authRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                                AuthenticationRequest.biometricRequest(
                                                    title = "Secure Access",
                                                    AuthenticationRequest.Biometric.Fallback.DeviceCredential
                                                ) {
                                                    setSubtitle("Authenticate to view secure notes")
                                                    setMinStrength(AuthenticationRequest.Biometric.Strength.Class2)
                                                }
                                            } else {
                                                AuthenticationRequest.biometricRequest(
                                                    title = "Secure Access"
                                                ) {
                                                    setSubtitle("Authenticate to view secure notes")
                                                    setMinStrength(AuthenticationRequest.Biometric.Strength.Class2)
                                                }
                                            }
                                            biometricLauncher.launch(authRequest)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (currentView == "Secure") MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                            contentColor = if (currentView == "Secure") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground
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
                                            scope.launch { drawerState.close() }
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
                                            scope.launch { drawerState.close() }
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
                                            scope.launch { drawerState.close() }
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
                                            Icon(Icons.Default.Info, contentDescription = "Help")
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
                            if (!editorViewModel.isAddingNote && editorViewModel.editingNote == null) {
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
                                                scope.launch { drawerState.open() }
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
                                            val isNoteView = currentView == "Notes" || currentView == "Favourites" ||
                                                    currentView == "Archive" || currentView == "Secure"
                                            if (isNoteView) {
                                                IconButton(onClick = { /* search logic */ }) {
                                                    Icon(Icons.Default.Search, contentDescription = "Search")
                                                }
                                                IconButton(onClick = {
                                                    editorViewModel.startNewNote()
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
                                    "Archive" -> archivedNotes
                                    "Secure" -> secureNotes
                                    "Reminders" -> reminderNotes
                                    else -> trashNotes
                                }

                                when (currentView) {
                                    "Settings" -> {
                                        SettingsView(
                                            modifier = Modifier.fillMaxSize(),
                                            isDarkMode = isDarkMode,
                                            onDarkModeChange = { isDarkMode = it },
                                            showWordCount = showWordCount,
                                            onShowWordCountChange = { showWordCount = it },
                                            showCharCount = showCharCount,
                                            onShowCharCountChange = { showCharCount = it }
                                        )
                                    }
                                    "Help & Feedback" -> {
                                        HelpFeedbackView(modifier = Modifier.fillMaxSize())
                                    }
                                    else -> {
                                        HorizontalDivider()
                                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                                            items(displayNotes) { note ->
                                                Button(
                                                    onClick = { editorViewModel.loadNote(note) },
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
                                                        Column(modifier = Modifier.weight(1f)) {
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
                                                            note.reminderTime?.let { time ->
                                                                val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                    Icon(
                                                                        Icons.Default.Notifications,
                                                                        contentDescription = null,
                                                                        modifier = Modifier.height(12.dp),
                                                                        tint = MaterialTheme.colorScheme.primary
                                                                    )
                                                                    Spacer(modifier = Modifier.width(4.dp))
                                                                    Text(
                                                                        text = sdf.format(time),
                                                                        style = MaterialTheme.typography.bodySmall,
                                                                        color = MaterialTheme.colorScheme.primary
                                                                    )
                                                                }
                                                            }
                                                        }

                                                        var showMenu by remember { mutableStateOf(false) }

                                                        Box {
                                                            IconButton(onClick = { showMenu = true }) {
                                                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                                                            }
                                                            DropdownMenu(
                                                                expanded = showMenu,
                                                                onDismissRequest = { showMenu = false }
                                                            ) {
                                                                if (currentView == "Notes" || currentView == "Favourites") {
                                                                    DropdownMenuItem(
                                                                        text = { Text("Secure") },
                                                                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Secure") },
                                                                        onClick = {
                                                                            noteViewModel.updateNote(note.copy(secure = true))
                                                                            showMenu = false
                                                                        }
                                                                    )
                                                                    DropdownMenuItem(
                                                                        text = { Text("Archive") },
                                                                        leadingIcon = { Icon(Icons.Default.Archive, contentDescription = "Archive") },
                                                                        onClick = {
                                                                            noteViewModel.updateNote(note.copy(archived = true))
                                                                            showMenu = false
                                                                        }
                                                                    )
                                                                    DropdownMenuItem(
                                                                        text = { Text("Move to Trash") },
                                                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Trash") },
                                                                        onClick = {
                                                                            noteViewModel.updateNote(note.copy(trash = true))
                                                                            showMenu = false
                                                                        }
                                                                    )
                                                                } else if (currentView == "Archive") {
                                                                    DropdownMenuItem(
                                                                        text = { Text("Unarchive") },
                                                                        leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = "Unarchive") },
                                                                        onClick = {
                                                                            noteViewModel.updateNote(note.copy(archived = false))
                                                                            showMenu = false
                                                                        }
                                                                    )
                                                                    DropdownMenuItem(
                                                                        text = { Text("Move to Trash") },
                                                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Trash") },
                                                                        onClick = {
                                                                            noteViewModel.updateNote(note.copy(trash = true))
                                                                            showMenu = false
                                                                        }
                                                                    )
                                                                } else if (currentView == "Secure") {
                                                                    DropdownMenuItem(
                                                                        text = { Text("Unsecure") },
                                                                        leadingIcon = { Icon(Icons.Default.LockOpen, contentDescription = "Unsecure") },
                                                                        onClick = {
                                                                            noteViewModel.updateNote(note.copy(secure = false))
                                                                            showMenu = false
                                                                        }
                                                                    )
                                                                    DropdownMenuItem(
                                                                        text = { Text("Move to Trash") },
                                                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Trash") },
                                                                        onClick = {
                                                                            noteViewModel.updateNote(note.copy(trash = true))
                                                                            showMenu = false
                                                                        }
                                                                    )
                                                                } else {
                                                                    // Trash view
                                                                    DropdownMenuItem(
                                                                        text = { Text("Restore") },
                                                                        leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = "Restore") },
                                                                        onClick = {
                                                                            noteViewModel.updateNote(note.copy(trash = false))
                                                                            showMenu = false
                                                                        }
                                                                    )
                                                                    DropdownMenuItem(
                                                                        text = { Text("Delete Permanently") },
                                                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Delete") },
                                                                        onClick = {
                                                                            noteViewModel.deleteNote(note)
                                                                            showMenu = false
                                                                        }
                                                                    )
                                                                }

                                                                if (note.reminderTime != null) {
                                                                    DropdownMenuItem(
                                                                        text = { Text("Clear Reminder") },
                                                                        leadingIcon = { Icon(Icons.Default.Notifications, contentDescription = "Clear Reminder") },
                                                                        onClick = {
                                                                            noteViewModel.updateNote(note.copy(reminderTime = null))
                                                                            showMenu = false
                                                                        }
                                                                    )
                                                                }

                                                                DropdownMenuItem(
                                                                    text = { Text("Share") },
                                                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = "Share") },
                                                                    onClick = {
                                                                        exportTarget = note
                                                                        showSharePopup = true
                                                                        showMenu = false
                                                                    }
                                                                )

                                                                DropdownMenuItem(
                                                                    text = { Text(if (note.favourite) "Unstar" else "Star") },
                                                                    leadingIcon = {
                                                                        Icon(
                                                                            if (note.favourite) Icons.Default.Star else Icons.Outlined.StarBorder,
                                                                            contentDescription = if (note.favourite) "Unstar" else "Star",
                                                                            tint = if (note.favourite) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant
                                                                        )
                                                                    },
                                                                    onClick = {
                                                                        noteViewModel.updateNote(note.copy(favourite = !note.favourite))
                                                                        showMenu = false
                                                                    }
                                                                )

                                                                DropdownMenuItem(
                                                                    text = { Text("Export") },
                                                                    leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = "Export") },
                                                                    onClick = {
                                                                        exportTarget = note
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
                                }
                            } else {
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
                                        IconButton(onClick = { editorViewModel.reset() }) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                        }
                                        Box {
                                            IconButton(onClick = {
                                                if (editorViewModel.isDrawingMode) {
                                                    editorViewModel.showToolMenu = !editorViewModel.showToolMenu
                                                } else {
                                                    editorViewModel.isDrawingMode = true
                                                    editorViewModel.currentTool = "Pen"
                                                }
                                            }) {
                                                Icon(
                                                    Icons.Default.Build,
                                                    contentDescription = "Drawing Tools",
                                                    tint = if (editorViewModel.isDrawingMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = editorViewModel.showToolMenu,
                                                onDismissRequest = { editorViewModel.showToolMenu = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Pen") },
                                                    onClick = {
                                                        editorViewModel.currentTool = "Pen"
                                                        editorViewModel.showToolMenu = false
                                                    },
                                                    leadingIcon = {
                                                        if (editorViewModel.currentTool == "Pen") Icon(Icons.Default.Check, contentDescription = null)
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Eraser") },
                                                    onClick = {
                                                        editorViewModel.currentTool = "Eraser"
                                                        editorViewModel.showToolMenu = false
                                                    },
                                                    leadingIcon = {
                                                        if (editorViewModel.currentTool == "Eraser") Icon(Icons.Default.Check, contentDescription = null)
                                                    }
                                                )
                                                HorizontalDivider()
                                                DropdownMenuItem(
                                                    text = { Text("Disable Drawing") },
                                                    onClick = {
                                                        editorViewModel.isDrawingMode = false
                                                        editorViewModel.showToolMenu = false
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Clear All Drawings") },
                                                    onClick = {
                                                        editorViewModel.importedElements = editorViewModel.importedElements.filter { it.type != "path" && it.type != "eraser" }
                                                        editorViewModel.showToolMenu = false
                                                    },
                                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                                                )
                                            }
                                        }

                                        IconButton(onClick = {
                                            val calendar = Calendar.getInstance()
                                            editorViewModel.reminderTime?.let { calendar.timeInMillis = it }
                                            
                                            DatePickerDialog(
                                                context,
                                                { _, year, month, dayOfMonth ->
                                                    calendar.set(Calendar.YEAR, year)
                                                    calendar.set(Calendar.MONTH, month)
                                                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                                    
                                                    TimePickerDialog(
                                                        context,
                                                        { _, hourOfDay, minute ->
                                                            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                                            calendar.set(Calendar.MINUTE, minute)
                                                            calendar.set(Calendar.SECOND, 0)
                                                            editorViewModel.reminderTime = calendar.timeInMillis
                                                        },
                                                        calendar.get(Calendar.HOUR_OF_DAY),
                                                        calendar.get(Calendar.MINUTE),
                                                        false
                                                    ).show()
                                                },
                                                calendar.get(Calendar.YEAR),
                                                calendar.get(Calendar.MONTH),
                                                calendar.get(Calendar.DAY_OF_MONTH)
                                            ).show()
                                        }) {
                                            Icon(
                                                Icons.Default.Notifications,
                                                contentDescription = "Set Reminder",
                                                tint = if (editorViewModel.reminderTime != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        IconButton(onClick = {
                                            filePickerLauncher.launch(arrayOf("image/*", "application/pdf", "text/plain"))
                                        }) {
                                            Icon(Icons.Default.Add, contentDescription = "Import")
                                        }

                                        IconButton(onClick = {
                                            if (editorViewModel.noteTitle.isNotBlank() || editorViewModel.noteContent.isNotBlank() || editorViewModel.importedElements.isNotEmpty()) {
                                                val currentNote = editorViewModel.editingNote
                                                if (currentNote != null) {
                                                    noteViewModel.updateNote(
                                                        currentNote.copy(
                                                            title = editorViewModel.noteTitle,
                                                            content = editorViewModel.noteContent,
                                                            elements = editorViewModel.importedElements,
                                                            reminderTime = editorViewModel.reminderTime
                                                        )
                                                    )
                                                } else {
                                                    noteViewModel.addNote(
                                                        editorViewModel.noteTitle,
                                                        editorViewModel.noteContent,
                                                        editorViewModel.importedElements,
                                                        secure = currentView == "Secure",
                                                        reminderTime = editorViewModel.reminderTime
                                                    )
                                                }
                                            }
                                            editorViewModel.reset()
                                        }) {
                                            Icon(Icons.Default.Check, contentDescription = "Save")
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .clipToBounds()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
                                    ) {
                                        NoteContent(
                                            noteTitle = editorViewModel.noteTitle,
                                            onNoteTitleChange = { editorViewModel.noteTitle = it },
                                            noteContent = editorViewModel.noteContent,
                                            onNoteContentChange = { editorViewModel.noteContent = it },
                                            importedElements = editorViewModel.importedElements,
                                            onImportedElementsChange = { editorViewModel.importedElements = it },
                                            panOffset = editorViewModel.panOffset,
                                            onPanOffsetChange = { editorViewModel.panOffset = it },
                                            zoomScale = editorViewModel.zoomScale,
                                            onZoomScaleChange = { editorViewModel.zoomScale = it },
                                            isDrawingMode = editorViewModel.isDrawingMode,
                                            currentTool = editorViewModel.currentTool,
                                            currentPathPoints = editorViewModel.currentPathPoints,
                                            onCurrentPathPointsChange = { editorViewModel.currentPathPoints = it },
                                            strokeColor = editorViewModel.strokeColor,
                                            isExporting = false,
                                            showWordCount = showWordCount,
                                            showCharCount = showCharCount
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Share popup
            if (showSharePopup) {
                Popup(
                    alignment = Alignment.Center,
                    onDismissRequest = {
                        showSharePopup = false
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .width(240.dp)
                            .shadow(8.dp, MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                // FIX 3: Show the target note's title in the share header for clarity.
                                "Share \"${exportTarget?.title ?: "Note"}\"",
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

            if (exportTarget != null) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(-10000, -10000) }
                        .wrapContentSize(unbounded = true)
                        .background(Color.White)
                ) {
                    exportTarget?.let { note ->
                        MaterialTheme(colorScheme = lightColorScheme()) {
                            @OptIn(ExperimentalComposeUiApi::class)
                            Box(
                                modifier = Modifier
                                    .wrapContentSize(unbounded = true)
                                    .background(Color.White)
                                    .capturable(captureController)
                                    .padding(32.dp)
                            ) {
                                NoteContent(
                                    noteTitle = note.title,
                                    onNoteTitleChange = {},
                                    noteContent = note.content,
                                    onNoteContentChange = {},
                                    importedElements = note.elements,
                                    onImportedElementsChange = {},
                                    panOffset = Offset.Zero,
                                    onPanOffsetChange = {},
                                    zoomScale = 1f,
                                    onZoomScaleChange = {},
                                    isDrawingMode = false,
                                    currentTool = "Pen",
                                    currentPathPoints = emptyList(),
                                    onCurrentPathPointsChange = {},
                                    strokeColor = Color.Black,
                                    isExporting = true,
                                    showWordCount = false,
                                    showCharCount = false
                                )
                            }
                        }
                    }
                }

                Popup(
                    alignment = Alignment.Center,
                    onDismissRequest = { exportTarget = null }
                ) {
                    Box(
                        modifier = Modifier
                            .width(240.dp)
                            .shadow(8.dp, MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
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
                                        saveAsPdf(bitmap.asAndroidBitmap(), context, exportTarget?.title ?: "Note")
                                        exportTarget = null
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as PNG") },
                                onClick = {
                                    scope.launch {
                                        val bitmap = captureController.captureAsync().await()
                                        saveAsPng(bitmap.asAndroidBitmap(), context, exportTarget?.title ?: "Note")
                                        exportTarget = null
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as Text") },
                                onClick = {
                                    exportTarget?.let { note ->
                                        saveAsText(context, note.title, note.content)
                                    }
                                    exportTarget = null
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
