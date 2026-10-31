package com.example.curio.feature.settings

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.curio.R
import com.example.curio.domain.DailyMode
import com.example.curio.feature.onboarding.modeDescription
import com.example.curio.feature.onboarding.modeLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenSources: () -> Unit,
    shakeEnabled: Boolean,
    onShakeToggle: (Boolean) -> Unit,
    hapticsEnabled: Boolean,
    onHapticsToggle: (Boolean) -> Unit,
    tiltEnabled: Boolean = true,
    onTiltToggle: (Boolean) -> Unit = {},
    darkMode: String = "SYSTEM",
    onDarkModeChange: (String) -> Unit = {},
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val msgDone = stringResource(R.string.settings_reset_done)
    val msgFailed = stringResource(R.string.settings_reset_failed)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            val text = when (event) {
                SettingsEvent.ResetDone -> msgDone
                SettingsEvent.ResetFailed -> msgFailed
            }
            snackbar.showSnackbar(text)
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)

            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_shake_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.settings_shake_desc), style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = shakeEnabled, onCheckedChange = onShakeToggle)
            }

            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_tilt_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.settings_tilt_desc), style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = tiltEnabled, onCheckedChange = onTiltToggle)
            }

            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_haptics_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.settings_haptics_desc), style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = hapticsEnabled, onCheckedChange = onHapticsToggle)
            }

            val modes = listOf("SYSTEM", "LIGHT", "DARK")
            val labels = listOf<Int>(R.string.appearance_system, R.string.appearance_light, R.string.appearance_dark)
            Text(stringResource(R.string.settings_appearance), style = MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                modes.forEachIndexed { i, mode ->
                    SegmentedButton(
                        selected = darkMode == mode,
                        onClick = { onDarkModeChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                    ) { Text(stringResource(labels[i])) }
                }
            }

            val context = LocalContext.current
            val hasAccelerometer = remember {
                (context.getSystemService(Context.SENSOR_SERVICE) as SensorManager)
                    .getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
            }
            val hasGyroscope = remember {
                (context.getSystemService(Context.SENSOR_SERVICE) as SensorManager)
                    .getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
            }

            Text(stringResource(R.string.settings_sensors), style = MaterialTheme.typography.titleMedium)
            ListItem(
                headlineContent = { Text(stringResource(R.string.sensor_touchscreen)) },
                supportingContent = { Text(stringResource(R.string.sensor_touchscreen_use)) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.sensor_accelerometer)) },
                supportingContent = {
                    Text(stringResource(if (hasAccelerometer) R.string.sensor_accelerometer_use else R.string.sensor_missing))
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.sensor_gyroscope)) },
                supportingContent = {
                    Text(stringResource(if (hasGyroscope) R.string.sensor_gyroscope_use else R.string.sensor_missing))
                },
            )

            Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleMedium)
            ui.themes.forEach { theme ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .selectable(
                            selected = theme.id == ui.themeId,
                            enabled = theme.available,
                            role = Role.RadioButton,
                            onClick = { viewModel.onThemeSelected(theme.id) },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    RadioButton(selected = theme.id == ui.themeId, onClick = null, enabled = theme.available)
                    Text(theme.title)
                    if (!theme.available) Text(stringResource(R.string.theme_coming_soon), style = MaterialTheme.typography.bodySmall)
                }
            }

            Text(stringResource(R.string.settings_mode), style = MaterialTheme.typography.titleMedium)
            DailyMode.values().forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .selectable(
                            selected = mode == ui.mode,
                            role = Role.RadioButton,
                            onClick = { viewModel.onModeSelected(mode) },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    RadioButton(selected = mode == ui.mode, onClick = null)
                    Column {
                        Text(stringResource(modeLabel(mode)))
                        Text(stringResource(modeDescription(mode)), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Text(stringResource(R.string.settings_change_tomorrow_hint), style = MaterialTheme.typography.bodySmall)

            OutlinedButton(
                onClick = viewModel::onResetRequested,
                enabled = !ui.resetting,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.settings_reset))
            }

            Text(
                text = stringResource(R.string.settings_sources),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button, onClick = onOpenSources)
                    .padding(vertical = 12.dp),
            )
        }
    }

    if (ui.resetDialogVisible) {
        AlertDialog(
            onDismissRequest = viewModel::onResetCancelled,
            title = { Text(stringResource(R.string.settings_reset_title)) },
            text = { Text(stringResource(R.string.settings_reset_message)) },
            confirmButton = {
                TextButton(onClick = viewModel::onResetConfirmed) { Text(stringResource(R.string.settings_reset_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onResetCancelled) { Text(stringResource(R.string.settings_reset_cancel)) }
            },
        )
    }
}
