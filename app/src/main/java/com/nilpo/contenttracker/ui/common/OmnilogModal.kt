package com.nilpo.contenttracker.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.ui.theme.OmnilogTheme

@Composable
fun OmnilogModal(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .widthIn(max = 520.dp),
            shape = RoundedCornerShape(16.dp),
            color = OmnilogTheme.colors.appPanel,
            contentColor = OmnilogTheme.colors.appInk,
            border = BorderStroke(1.dp, OmnilogTheme.colors.appLine),
            tonalElevation = 0.dp,
            shadowElevation = 10.dp,
            content = content,
        )
    }
}

@Composable
fun OmnilogAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    text: @Composable (() -> Unit)? = null,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = title,
                color = OmnilogTheme.colors.appInk,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
            )
        },
        text = text,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        shape = RoundedCornerShape(16.dp),
        containerColor = OmnilogTheme.colors.appPanel,
        titleContentColor = OmnilogTheme.colors.appInk,
        textContentColor = OmnilogTheme.colors.appMuted,
        tonalElevation = 0.dp,
    )
}

@Composable
fun omnilogModalTextFieldColors(accent: androidx.compose.ui.graphics.Color) =
    androidx.compose.material3.OutlinedTextFieldDefaults.colors(
        focusedBorderColor = accent,
        cursorColor = accent,
        unfocusedBorderColor = OmnilogTheme.colors.appLine,
        focusedLabelColor = accent,
        focusedPlaceholderColor = OmnilogTheme.colors.appMuted,
        unfocusedPlaceholderColor = OmnilogTheme.colors.appMuted,
    )
