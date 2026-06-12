package com.example.noted.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsView(
    modifier: Modifier = Modifier,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    showWordCount: Boolean,
    onShowWordCountChange: (Boolean) -> Unit,
    showCharCount: Boolean,
    onShowCharCountChange: (Boolean) -> Unit
) {
    var cloudSync by remember { mutableStateOf(false) }
    var wifiOnly by remember { mutableStateOf(false) }
    var autoBackup by remember { mutableStateOf(true) }
    var appLock by remember { mutableStateOf(false) }
    var hidePreviews by remember { mutableStateOf(false) }
    var encryptNotes by remember { mutableStateOf(false) }
    var reminderNotifs by remember { mutableStateOf(true) }
    var vibration by remember { mutableStateOf(true) }
    var highContrast by remember { mutableStateOf(false) }
    var autosave by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        HorizontalDivider()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item { SettingHeader("General") }
            item { SettingSwitchRow("Dark Mode", isDarkMode) { onDarkModeChange(it) } }
            item { SettingClickableRow("Activate AI Assistant") }
            item { SettingClickableRow("Theme") }
            item { SettingClickableRow("Accent Color Selection") }
            item { SettingClickableRow("Font Size") }
            item { SettingClickableRow("Font Family") }
            item { SettingSwitchRow("AutoSave", checked = autosave) { autosave = it } }
            item { SettingSwitchRow("Show Word Count", showWordCount) { onShowWordCountChange(it) } }
            item { SettingSwitchRow("Show Character Count", checked = showCharCount) { onShowCharCountChange(it) } }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item { SettingHeader("Backup & Sync") }
            item { SettingSwitchRow("Enable cloud sync", cloudSync) { cloudSync = it } }
            item { SettingSwitchRow("Sync over Wi-Fi only", wifiOnly) { wifiOnly = it } }
            item { SettingSwitchRow("Auto backup", autoBackup) { autoBackup = it } }
            item { SettingClickableRow("Backup frequency") }
            item { SettingClickableRow("Export notes") }
            item { SettingClickableRow("Import notes") }
            item { SettingClickableRow("Restore from backup") }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item { SettingHeader("Security & Privacy") }
            item { SettingSwitchRow("App lock (PIN, password, fingerprint)", appLock) { appLock = it } }
            item { SettingClickableRow("Lock timeout duration") }
            item { SettingSwitchRow("Hide note previews in recent apps", hidePreviews) { hidePreviews = it } }
            item { SettingSwitchRow("Encrypt notes", encryptNotes) { encryptNotes = it } }
            item { SettingClickableRow("Secure backup encryption") }
            item { SettingClickableRow("Biometric authentication") }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item { SettingHeader("Notifications") }
            item { SettingSwitchRow("Reminder notifications", reminderNotifs) { reminderNotifs = it } }
            item { SettingClickableRow("Daily note reminder") }
            item { SettingClickableRow("Upcoming reminders alert") }
            item { SettingClickableRow("Notification sound") }
            item { SettingSwitchRow("Vibration toggle", vibration) { vibration = it } }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item { SettingHeader("Storage") }
            item { SettingClickableRow("Cache size information") }
            item { SettingClickableRow("Clear cache") }
            item { SettingClickableRow("Storage usage breakdown") }
            item { SettingClickableRow("Download attachments on demand") }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item { SettingHeader("Accessibility") }
            item { SettingClickableRow("Larger text mode") }
            item { SettingSwitchRow("High contrast mode", highContrast) { highContrast = it } }
            item { SettingClickableRow("Reduce animations") }
            item { SettingClickableRow("Screen reader optimizations") }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item { SettingHeader("Advanced") }
            item { SettingClickableRow("Developer mode") }
            item { SettingClickableRow("Debug logging") }
            item { SettingClickableRow("Export diagnostic logs") }
            item { SettingClickableRow("Experimental features toggle") }
        }
    }
}

@Composable
fun SettingHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SettingSwitchRow(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingClickableRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
