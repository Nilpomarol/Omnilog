package com.nilpo.contenttracker.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun TrackingStatusSelector(
    selectedStatus: TrackingStatus,
    accent: Color,
    onStatusSelected: (TrackingStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TrackingStatus.entries.forEach { status ->
            val isSelected = status == selectedStatus
            val statusColor = statusColor(status)
            Surface(
                onClick = { onStatusSelected(status) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) statusColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) statusColor.copy(alpha = 0.78f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                ),
            ) {
                Text(
                    text = stringResource(status.labelResId()),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = if (isSelected) statusColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun TrackingProgressField(
    value: String,
    progressTotal: Int?,
    mediaType: MediaType,
    label: String,
    accent: Color,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = value.toIntOrNull() ?: 0
    val maximum = progressTotal ?: Int.MAX_VALUE
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilledTonalIconButton(
                onClick = { onValueChange((current - 1).coerceAtLeast(0).toString()) },
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = accent.copy(alpha = 0.14f), contentColor = accent),
            ) { Text(text = "−", fontSize = 20.sp, fontWeight = FontWeight.Light) }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                OutlinedTextField(
                    value = value,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        onValueChange(digits.toIntOrNull()?.coerceIn(0, maximum)?.toString() ?: digits)
                    },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = accent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = trackingTextFieldColors(accent),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                progressTotal?.takeIf { it > 0 }?.let { total ->
                    Text(text = "de $total ${progressUnitLabel(mediaType, total)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f))
                }
            }
            FilledTonalIconButton(
                onClick = { onValueChange((current + 1).coerceAtMost(maximum).toString()) },
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = accent.copy(alpha = 0.14f), contentColor = accent),
            ) { Text(text = "+", fontSize = 20.sp, fontWeight = FontWeight.Light) }
        }
    }
}

@Composable
fun TrackingRatingSelector(currentRating: Int?, accent: Color, onRatingSelected: (Int?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            (1..10).forEach { rating ->
                val isSelected = rating == currentRating
                val isActive = currentRating != null && rating <= currentRating
                Surface(
                    onClick = { onRatingSelected(if (isSelected) null else rating) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isActive) accent.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
                    border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isActive) accent.copy(alpha = 0.74f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)),
                ) {
                    Text(text = rating.toString(), modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge, fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold, color = if (isActive) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f))
                }
            }
        }
        if (currentRating != null) {
            Text(text = stringResource(R.string.rating_clear), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error.copy(alpha = 0.78f), modifier = Modifier.clickable { onRatingSelected(null) }.padding(vertical = 4.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingDateField(label: String, value: String, accent: Color, onValueChange: (String) -> Unit) {
    var showPicker by remember(label) { mutableStateOf(false) }
    val selectedMillis = value.toLocalDateOrNull()?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    OutlinedTextField(
        value = value, onValueChange = { onValueChange(it.take(10)) }, modifier = Modifier.fillMaxWidth(), label = { Text(label) }, placeholder = { Text("YYYY-MM-DD") }, singleLine = true,
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value.isNotBlank()) IconButton(onClick = { onValueChange("") }) { Icon(Icons.Filled.Close, stringResource(R.string.clear_date), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.72f)) }
                TextButton(onClick = { showPicker = true }, colors = ButtonDefaults.textButtonColors(contentColor = accent)) { Text(stringResource(R.string.pick_date)) }
            }
        }, colors = trackingTextFieldColors(accent), shape = RoundedCornerShape(12.dp),
    )
    if (showPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedMillis)
        DatePickerDialog(onDismissRequest = { showPicker = false }, confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { onValueChange(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().toString()) }
                showPicker = false
            }, colors = ButtonDefaults.textButtonColors(contentColor = accent)) { Text(stringResource(R.string.save)) }
        }, dismissButton = { TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.cancel)) } }) { DatePicker(state = datePickerState) }
    }
}

@Composable
fun TrackingNotesField(value: String, accent: Color, onValueChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth(), placeholder = { Text(stringResource(R.string.notes_placeholder)) }, colors = trackingTextFieldColors(accent), shape = RoundedCornerShape(12.dp), textStyle = MaterialTheme.typography.bodyMedium, maxLines = 6)
}

@Composable
private fun trackingTextFieldColors(accent: Color) = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant, cursorColor = accent)
private fun TrackingStatus.labelResId(): Int = when (this) {
    TrackingStatus.Planned -> R.string.status_planned
    TrackingStatus.InProgress -> R.string.status_in_progress
    TrackingStatus.Completed -> R.string.status_completed
    TrackingStatus.Paused -> R.string.status_paused
    TrackingStatus.Dropped -> R.string.status_dropped
}
private fun statusColor(status: TrackingStatus): Color = when (status) {
    TrackingStatus.Planned -> OmnilogColors.Planned
    TrackingStatus.InProgress -> OmnilogColors.InProgress
    TrackingStatus.Completed -> OmnilogColors.Completed
    TrackingStatus.Paused -> OmnilogColors.Paused
    TrackingStatus.Dropped -> OmnilogColors.Dropped
}
@Composable
private fun progressUnitLabel(mediaType: MediaType, value: Int): String = when (mediaType) {
    MediaType.Anime,
    MediaType.TvShow -> if (value == 1) "episodi" else "episodis"
    MediaType.Book -> if (value == 1) "pàgina" else "pàgines"
    MediaType.Movie -> if (value == 1) "minut" else "minuts"
    MediaType.Game -> if (value == 1) "hora" else "hores"
}
private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()
