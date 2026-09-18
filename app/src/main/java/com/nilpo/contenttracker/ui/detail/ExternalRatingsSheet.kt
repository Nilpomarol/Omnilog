package com.nilpo.contenttracker.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.core.model.ExternalRating
import com.nilpo.contenttracker.core.model.ExternalRatingOrigin
import com.nilpo.contenttracker.core.model.ExternalRatingSource
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.ui.common.OmnilogLocale
import com.nilpo.contenttracker.ui.common.OmnilogPrimaryButton
import com.nilpo.contenttracker.ui.common.OmnilogStatusPanel
import com.nilpo.contenttracker.ui.common.displayName
import com.nilpo.contenttracker.ui.common.sourceName
import com.nilpo.contenttracker.ui.common.formatCompactCount
import com.nilpo.contenttracker.ui.common.localizedSteamScoreDescriptor
import com.nilpo.contenttracker.ui.common.omnilogModalTextFieldColors
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import com.nilpo.contenttracker.ui.theme.SerifFontFamily
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import kotlin.math.roundToInt

private val SheetGutter = 24.dp

/** Id of the rating open in the form; [NewRating] for a new one, null for the list. */
private const val NewRating = -1L

/**
 * What other sites say about a title, and the form to add or correct one, in a single sheet.
 *
 * The list is the sheet's resting state; tapping a row or the add button swaps the same sheet to
 * the form, and back returns to the list rather than closing everything.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExternalRatingsSheet(
    title: String,
    mediaType: MediaType,
    ratings: List<ExternalRating>,
    primaryRatingId: Long?,
    accent: Color,
    onAdd: (ExternalRatingSource, Double, Double, Int?, Boolean, String?) -> Unit,
    onUpdate: (Long, ExternalRatingSource, Double, Double, Int?, Boolean, String?) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    // The repository keeps one rating per source, so only unused known sources are offered; any other
    // site can still be added by name.
    val usedSources = ratings.map { it.source }.toSet()
    val freeSources = mediaType.externalRatingSources().filterNot { it in usedSources }
    val sortedRatings = ratings.sortedWith(
        compareByDescending<ExternalRating> { it.id == primaryRatingId }.thenBy { it.source.ordinal },
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = OmnilogTheme.colors.appBackground,
    ) {
        // Inside the sheet: it has its own window, and back there would otherwise close it outright.
        BackHandler(enabled = editingId != null) { editingId = null }
        AnimatedContent(targetState = editingId, label = "externalRatingsSheet") { id ->
            if (id == null) {
                RatingsList(
                    title = title,
                    mediaType = mediaType,
                    ratings = sortedRatings,
                    primaryRatingId = primaryRatingId,
                    accent = accent,
                    onOpen = { editingId = it },
                )
            } else {
                val rating = ratings.firstOrNull { it.id == id }
                // Just deleted, and fading out: there is nothing left to show.
                if (id != NewRating && rating == null) return@AnimatedContent
                RatingForm(
                    rating = rating,
                    mediaType = mediaType,
                    sources = (listOfNotNull(rating?.source) + freeSources + ExternalRatingSource.Other).distinct(),
                    initialSource = rating?.source ?: freeSources.preferredFor(mediaType),
                    // Every other rating's name, so a site cannot be added twice under either spelling.
                    takenNames = ratings.filter { it.id != id }.map { it.sourceName().lowercase() }.toSet(),
                    isPrimary = rating != null && rating.id == primaryRatingId,
                    // The only rating is the primary one whatever the switch says, so it is not offered.
                    offerPrimary = ratings.any { it.id != id },
                    accent = accent,
                    onBack = { editingId = null },
                    onSave = { source, score, maxScore, votes, makePrimary, customName ->
                        if (rating == null) {
                            onAdd(source, score, maxScore, votes, makePrimary || ratings.isEmpty(), customName)
                        } else {
                            onUpdate(rating.id, source, score, maxScore, votes, makePrimary, customName)
                        }
                        editingId = null
                    },
                    onDelete = rating?.let { { onDelete(it.id); editingId = null } },
                )
            }
        }
    }
}

@Composable
private fun RatingsList(
    title: String,
    mediaType: MediaType,
    ratings: List<ExternalRating>,
    primaryRatingId: Long?,
    accent: Color,
    onOpen: (Long) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = SheetGutter),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SheetTitle(stringResource(R.string.detail_external_scores))
            Text(
                text = stringResource(R.string.external_ratings_note, title),
                style = MaterialTheme.typography.bodyMedium,
                color = OmnilogTheme.colors.appMuted,
            )
        }

        if (ratings.isEmpty()) {
            OmnilogStatusPanel(
                text = stringResource(R.string.external_ratings_empty),
                accent = accent,
                modifier = Modifier.padding(horizontal = SheetGutter, vertical = 16.dp),
            )
        } else {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 12.dp),
            ) {
                ratings.forEachIndexed { index, rating ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = SheetGutter),
                            color = OmnilogTheme.colors.appLine,
                        )
                    }
                    RatingRow(
                        rating = rating,
                        mediaType = mediaType,
                        isPrimary = rating.id == primaryRatingId,
                        accent = accent,
                        onClick = { onOpen(rating.id) },
                    )
                }
            }
        }

        OmnilogPrimaryButton(
            text = stringResource(R.string.add_external_rating),
            onClick = { onOpen(NewRating) },
            accent = accent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = SheetGutter, end = SheetGutter, top = 12.dp)
                .heightIn(min = 48.dp),
        )
    }
}

/**
 * `IMDb · Principal` over `1,2M vots`, with the score on the source's own scale at the end — the same
 * figure the form asks for, so what is read here is what gets typed there.
 */
@Composable
private fun RatingRow(
    rating: ExternalRating,
    mediaType: MediaType,
    isPrimary: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    val details = listOfNotNull(
        localizedSteamScoreDescriptor(mediaType, rating.source, rating.scoreDescriptor),
        rating.voteCount?.let {
            pluralStringResource(R.plurals.external_rating_votes, it, formatCompactCount(it.toDouble()))
        },
        stringResource(R.string.external_rating_manual).takeIf { rating.origin == ExternalRatingOrigin.Manual },
    ).joinToString(" · ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = stringResource(R.string.edit), onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(horizontal = SheetGutter, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = rating.sourceName(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OmnilogTheme.colors.appInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isPrimary) {
                    Text(
                        text = " · " + stringResource(R.string.primary_external_rating),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                    )
                }
            }
            if (details.isNotEmpty()) {
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = OmnilogTheme.colors.appMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = rating.score.nativeScore(rating.maxScore, mediaType, rating.source),
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = SerifFontFamily),
                color = OmnilogTheme.colors.appInk,
            )
            if (!isSteamPercentage(mediaType, rating.source)) {
                Text(
                    text = " / ${rating.maxScore.plain()}",
                    modifier = Modifier.padding(bottom = 3.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = OmnilogTheme.colors.appMuted,
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = OmnilogTheme.colors.appMuted,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RatingForm(
    rating: ExternalRating?,
    mediaType: MediaType,
    sources: List<ExternalRatingSource>,
    initialSource: ExternalRatingSource,
    takenNames: Set<String>,
    isPrimary: Boolean,
    offerPrimary: Boolean,
    accent: Color,
    onBack: () -> Unit,
    onSave: (ExternalRatingSource, Double, Double, Int?, Boolean, String?) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var source by rememberSaveable { mutableStateOf(initialSource) }
    var customName by rememberSaveable { mutableStateOf(rating?.customSourceName.orEmpty()) }
    var customMaxText by rememberSaveable {
        mutableStateOf(rating?.takeIf { it.source == ExternalRatingSource.Other }?.maxScore?.plain() ?: "10")
    }
    var scoreText by rememberSaveable {
        mutableStateOf(rating?.let { it.score.nativeScore(it.maxScore, mediaType, it.source) }.orEmpty())
    }
    var votesText by rememberSaveable { mutableStateOf(rating?.voteCount?.toString().orEmpty()) }
    var makePrimary by rememberSaveable {
        mutableStateOf(isPrimary || (rating == null && mediaType.prefersPrimarySource(initialSource)))
    }
    val isCustom = source == ExternalRatingSource.Other
    // A typed name that turns out to be a known site is saved as that site, so it reads the same everywhere.
    val typedKnownSource = customName.trim().let { typed ->
        ExternalRatingSource.entries.firstOrNull {
            it != ExternalRatingSource.Other && it.displayName().equals(typed, ignoreCase = true)
        }
    }
    val nameTaken = isCustom && customName.trim().lowercase() in takenNames
    val customMax = customMaxText.toDecimalOrNull()?.takeIf { it > 0.0 }
    // A rating keeps its own scale; a different known source brings that source's scale with it, and
    // a site the app does not know brings whatever scale the user gives it.
    val maxScore = when {
        isCustom -> customMax ?: 10.0
        else -> rating?.takeIf { it.source == source }?.maxScore ?: source.defaultMaxScore()
    }
    val isPercentage = isSteamPercentage(mediaType, source)
    val score = scoreText.toDecimalOrNull()
    val votes = votesText.trim().toIntOrNull()
    val scoreTooHigh = score != null && score > (if (isPercentage) 100.0 else maxScore)
    val scoreInvalid = scoreText.isNotBlank() && score == null
    val votesInvalid = votesText.isNotBlank() && (votes == null || votes < 0)
    val customInvalid = isCustom && (customName.isBlank() || nameTaken || customMax == null)
    val canSave = score != null && !scoreTooHigh && !votesInvalid && !customInvalid
    val scoreFocus = remember { FocusRequester() }
    val nameFocus = remember { FocusRequester() }
    // A new rating is mostly its figure, so the keyboard comes up ready for it; or for the site's
    // name when no known site is left to pick.
    LaunchedEffect(Unit) {
        if (rating == null) (if (isCustom) nameFocus else scoreFocus).requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SheetGutter)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.padding(end = 4.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = OmnilogTheme.colors.appInk,
                )
            }
            SheetTitle(
                stringResource(if (rating == null) R.string.external_rating_add_title else R.string.external_rating_edit_title),
            )
        }

        FormField(stringResource(R.string.field_external_rating_source)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                sources.forEach { option ->
                    SourcePill(
                        label = if (option == ExternalRatingSource.Other) {
                            stringResource(R.string.external_rating_other_source)
                        } else {
                            option.displayName()
                        },
                        selected = option == source,
                        accent = accent,
                        onClick = {
                            source = option
                            if (rating == null) makePrimary = mediaType.prefersPrimarySource(option)
                        },
                    )
                }
            }
        }

        if (isCustom) {
            OutlinedTextField(
                value = customName,
                onValueChange = { customName = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(nameFocus),
                label = { Text(stringResource(R.string.external_rating_custom_name)) },
                placeholder = { Text(stringResource(R.string.external_rating_custom_name_placeholder)) },
                supportingText = if (nameTaken) {
                    { Text(stringResource(R.string.external_rating_custom_name_taken, customName.trim())) }
                } else {
                    null
                },
                isError = nameTaken,
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                colors = omnilogModalTextFieldColors(accent),
                shape = RoundedCornerShape(12.dp),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = scoreText,
            onValueChange = { scoreText = it },
            modifier = Modifier
                .weight(1f)
                .focusRequester(scoreFocus),
            label = { Text(stringResource(R.string.field_external_rating_score)) },
            // A custom site's scale has its own field beside this one.
            suffix = if (isCustom) null else {
                { Text(if (isPercentage) "%" else "/ ${maxScore.plain()}") }
            },
            supportingText = {
                Text(
                    when {
                        scoreInvalid -> stringResource(R.string.external_rating_score_invalid)
                        scoreTooHigh -> stringResource(
                            R.string.external_rating_score_too_high,
                            if (isPercentage) "100%" else maxScore.plain(),
                        )
                        isPercentage -> stringResource(R.string.external_rating_score_hint_steam)
                        isCustom -> stringResource(R.string.external_rating_score_hint_custom)
                        else -> stringResource(R.string.external_rating_score_hint, source.displayName(), maxScore.plain())
                    },
                )
            },
            isError = scoreInvalid || scoreTooHigh,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = MaterialTheme.typography.titleLarge,
            colors = omnilogModalTextFieldColors(accent),
            shape = RoundedCornerShape(12.dp),
        )
        if (isCustom) {
            OutlinedTextField(
                value = customMaxText,
                onValueChange = { customMaxText = it },
                modifier = Modifier.weight(0.6f),
                label = { Text(stringResource(R.string.external_rating_custom_max)) },
                isError = customMax == null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.titleLarge,
                colors = omnilogModalTextFieldColors(accent),
                shape = RoundedCornerShape(12.dp),
            )
        }
        }

        OutlinedTextField(
            value = votesText,
            onValueChange = { votesText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.field_external_rating_users)) },
            supportingText = { Text(stringResource(R.string.external_rating_votes_hint)) },
            isError = votesInvalid,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = omnilogModalTextFieldColors(accent),
            shape = RoundedCornerShape(12.dp),
        )

        if (offerPrimary) {
            // The primary one cannot be switched off here: another rating has to take its place.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isPrimary, role = Role.Switch) { makePrimary = !makePrimary },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.make_primary_external_rating),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = OmnilogTheme.colors.appInk,
                    )
                    Text(
                        text = stringResource(
                            if (isPrimary) R.string.external_rating_primary_locked else R.string.external_rating_primary_note,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = OmnilogTheme.colors.appMuted,
                    )
                }
                Switch(
                    checked = makePrimary,
                    onCheckedChange = null,
                    enabled = !isPrimary,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = accent,
                        checkedTrackColor = accent.copy(alpha = 0.4f),
                        disabledCheckedThumbColor = accent.copy(alpha = 0.6f),
                        disabledCheckedTrackColor = accent.copy(alpha = 0.2f),
                    ),
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            OmnilogPrimaryButton(
                text = stringResource(if (rating == null) R.string.add else R.string.save),
                onClick = {
                    // Steam is typed as a percentage; stored on the rating's own scale.
                    val stored = if (isPercentage) (score ?: return@OmnilogPrimaryButton) / 100.0 * maxScore else score
                    stored ?: return@OmnilogPrimaryButton
                    when {
                        !isCustom -> onSave(source, stored, maxScore, votes, makePrimary, null)
                        // A known site's name typed by hand: saved as that site, on the scale given here.
                        typedKnownSource != null -> onSave(typedKnownSource, stored, maxScore, votes, makePrimary, null)
                        else -> onSave(source, stored, maxScore, votes, makePrimary, customName.trim())
                    }
                },
                enabled = canSave,
                accent = accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            )
            onDelete?.let {
                TextButton(
                    onClick = it,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.delete_external_rating))
                }
            }
        }
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.headlineSmall.copy(
            fontFamily = SerifFontFamily,
            fontWeight = FontWeight.Normal,
        ),
        color = OmnilogTheme.colors.appInk,
    )
}

@Composable
private fun FormField(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = OmnilogTheme.colors.appMuted,
        )
        content()
    }
}

/** A soft paper pill, tinted with the accent once chosen — the add flow's edition filter. */
@Composable
private fun SourcePill(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Surface(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) accent.copy(alpha = 0.16f) else OmnilogTheme.colors.appPanel,
        border = if (selected) BorderStroke(1.dp, accent) else null,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) accent else OmnilogTheme.colors.appInk,
        )
    }
}

private fun isSteamPercentage(mediaType: MediaType, source: ExternalRatingSource) =
    mediaType == MediaType.Game && source == ExternalRatingSource.Steam

/** The score as the source itself prints it: `4,21` on Goodreads, `87` for Steam's percentage. */
private fun Double.nativeScore(maxScore: Double, mediaType: MediaType, source: ExternalRatingSource): String =
    if (isSteamPercentage(mediaType, source) && maxScore > 0.0) {
        (this / maxScore * 100.0).roundToInt().toString()
    } else {
        plain()
    }

private fun Double.plain(): String = DecimalFormat("0.##", DecimalFormatSymbols(OmnilogLocale)).format(this)

internal fun String.toDecimalOrNull(): Double? = trim().replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0.0 }

private fun List<ExternalRatingSource>.preferredFor(mediaType: MediaType): ExternalRatingSource =
    firstOrNull { mediaType.prefersPrimarySource(it) || it == mediaType.defaultExternalRatingSource() }
        ?: firstOrNull()
        ?: ExternalRatingSource.Other

private fun MediaType.defaultExternalRatingSource(): ExternalRatingSource = when (this) {
    MediaType.Book -> ExternalRatingSource.Goodreads
    MediaType.Anime -> ExternalRatingSource.Mal
    MediaType.Movie, MediaType.TvShow -> ExternalRatingSource.Imdb
    MediaType.Game -> ExternalRatingSource.Steam
}

private fun ExternalRatingSource.defaultMaxScore(): Double = when (this) {
    ExternalRatingSource.Goodreads, ExternalRatingSource.StoryGraph -> 5.0
    ExternalRatingSource.RottenTomatoes, ExternalRatingSource.Metacritic, ExternalRatingSource.Steam -> 100.0
    else -> 10.0
}

private fun MediaType.prefersPrimarySource(source: ExternalRatingSource): Boolean = when (this) {
    MediaType.Book -> source == ExternalRatingSource.Goodreads
    MediaType.Game -> source == ExternalRatingSource.Steam
    else -> false
}

private fun MediaType.externalRatingSources(): List<ExternalRatingSource> = when (this) {
    MediaType.Anime -> listOf(ExternalRatingSource.AniList, ExternalRatingSource.Mal)
    MediaType.Book -> listOf(
        ExternalRatingSource.Goodreads,
        ExternalRatingSource.StoryGraph,
        ExternalRatingSource.GoogleBooks,
        ExternalRatingSource.OpenLibrary,
    )
    MediaType.Movie, MediaType.TvShow -> listOf(
        ExternalRatingSource.Imdb,
        ExternalRatingSource.Tmdb,
        ExternalRatingSource.RottenTomatoes,
        ExternalRatingSource.Metacritic,
        ExternalRatingSource.FilmAffinity,
    )
    MediaType.Game -> listOf(
        ExternalRatingSource.Rawg,
        ExternalRatingSource.Metacritic,
        ExternalRatingSource.Steam,
    )
}

/**
 * Offered on opening a book with no Goodreads score, the one score most readers look for. It stays
 * small and asks for nothing first: no keyboard until the user decides to fill it in.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GoodreadsRatingSheet(
    title: String,
    accent: Color,
    onSave: (Double, Int?) -> Unit,
    onSkipBook: () -> Unit,
    onDismiss: () -> Unit,
) {
    var scoreText by rememberSaveable { mutableStateOf("") }
    var votesText by rememberSaveable { mutableStateOf("") }
    val score = scoreText.toDecimalOrNull()
    val votes = votesText.trim().toIntOrNull()
    val scoreInvalid = scoreText.isNotBlank() && (score == null || score > GoodreadsMax)
    val votesInvalid = votesText.isNotBlank() && (votes == null || votes < 0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = OmnilogTheme.colors.appBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SheetGutter)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SheetTitle(stringResource(R.string.goodreads_prompt_title))
                Text(
                    text = stringResource(R.string.goodreads_prompt_note, title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OmnilogTheme.colors.appMuted,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = scoreText,
                    onValueChange = { scoreText = it },
                    modifier = Modifier.weight(1f),
                    // The scale sits in the label: a suffix only shows once the field has focus.
                    label = { Text(stringResource(R.string.goodreads_prompt_score, GoodreadsMax.plain())) },
                    supportingText = if (scoreInvalid) {
                        { Text(stringResource(R.string.external_rating_score_too_high, GoodreadsMax.plain())) }
                    } else {
                        null
                    },
                    isError = scoreInvalid,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = MaterialTheme.typography.titleLarge,
                    colors = omnilogModalTextFieldColors(accent),
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = votesText,
                    onValueChange = { votesText = it },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.field_external_rating_users)) },
                    isError = votesInvalid,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.titleLarge,
                    colors = omnilogModalTextFieldColors(accent),
                    shape = RoundedCornerShape(12.dp),
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OmnilogPrimaryButton(
                    text = stringResource(R.string.save),
                    onClick = { onSave(score ?: return@OmnilogPrimaryButton, votes) },
                    enabled = score != null && !scoreInvalid && !votesInvalid,
                    accent = accent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.goodreads_prompt_later), color = OmnilogTheme.colors.appMuted)
                    }
                    TextButton(onClick = onSkipBook) {
                        Text(stringResource(R.string.goodreads_prompt_skip_book), color = OmnilogTheme.colors.appMuted)
                    }
                }
            }
        }
    }
}

private const val GoodreadsMax = 5.0
