package com.nilpo.contenttracker.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.nilpo.contenttracker.core.model.SynopsisDocument
import com.nilpo.contenttracker.core.model.SynopsisStyle
import com.nilpo.contenttracker.core.model.parseSynopsis

@Composable
fun SynopsisText(
    body: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val document = remember(body) { parseSynopsis(body) }
    Text(
        text = document?.toAnnotatedString() ?: AnnotatedString(""),
        modifier = modifier,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
    )
}

private fun SynopsisDocument.toAnnotatedString(): AnnotatedString = buildAnnotatedString {
    runs.forEach { run ->
        val spanStyle = SpanStyle(
            fontWeight = FontWeight.Bold.takeIf { SynopsisStyle.Bold in run.styles },
            fontStyle = FontStyle.Italic.takeIf { SynopsisStyle.Italic in run.styles },
        )
        withStyle(spanStyle) { append(run.text) }
    }
}
