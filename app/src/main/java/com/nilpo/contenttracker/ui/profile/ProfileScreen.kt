package com.nilpo.contenttracker.ui.profile

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.Objective
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.objectives.ObjectiveCalculator
import com.nilpo.contenttracker.core.stats.StatsCalculator
import com.nilpo.contenttracker.core.stats.StatsFilters
import com.nilpo.contenttracker.core.stats.StatsPeriod
import com.nilpo.contenttracker.core.stats.completionDates
import com.nilpo.contenttracker.ui.ProfileHeaderActions
import com.nilpo.contenttracker.ui.common.omnilogModalTextFieldColors
import com.nilpo.contenttracker.ui.detail.BarPaddingReclaim
import com.nilpo.contenttracker.ui.detail.RelatedMediaSection
import com.nilpo.contenttracker.ui.home.EditorialSheet
import com.nilpo.contenttracker.ui.home.TopBarOpacityEffect
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import com.nilpo.contenttracker.ui.theme.SerifFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

/** How many of the latest finished titles the profile shows as covers. */
private const val RecentCompletedCount = 12

/**
 * The profile as an editorial page: who you are in the header, what you have finished — counted by
 * format and shown as the latest covers — and the objectives you are working towards.
 *
 * Editing the profile happens in a bottom sheet opened from the app bar, like every other question
 * the editorial pages ask.
 */
@Composable
fun ProfileScreen(
    items: List<TrackedMedia>,
    objectives: List<Objective> = emptyList(),
    onOpenMedia: (TrackedMedia) -> Unit,
    onSaveObjective: (Objective) -> Unit = {},
    onDeleteObjective: (Long) -> Unit = {},
    headerActions: ProfileHeaderActions = remember { ProfileHeaderActions() },
    modifier: Modifier = Modifier,
    /** The app bar's height; the page scrolls under the bar, which draws its own surface. */
    topInset: Dp = 0.dp,
    /** How solid the transparent app bar should be: clear over the header, solid once it scrolls away. */
    onTopBarOpacityChange: (Float) -> Unit = {},
) {
    val context = LocalContext.current
    val preferences = remember(context) { ProfilePreferences.from(context) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    TopBarOpacityEffect(listState = listState, solid = false, onOpacityChange = onTopBarOpacityChange)

    val legacyProfileImageUri = remember(context) {
        preferences.getString(ProfilePreferences.AVATAR_IMAGE_URI_KEY, null)
    }
    var profileImagePath by rememberSaveable {
        mutableStateOf(
            preferences.getString(ProfilePreferences.AVATAR_IMAGE_PATH_KEY, null)
                ?.takeIf { path -> File(path).isFile },
        )
    }
    var profileImageUrl by rememberSaveable { mutableStateOf("") }
    var isImageLoading by rememberSaveable { mutableStateOf(false) }
    var imageError by rememberSaveable { mutableStateOf<String?>(null) }
    var displayName by rememberSaveable {
        mutableStateOf(
            preferences.getString(
                ProfilePreferences.DISPLAY_NAME_KEY,
                ProfilePreferences.DEFAULT_DISPLAY_NAME,
            ).orEmpty(),
        )
    }
    var bio by rememberSaveable {
        mutableStateOf(
            preferences.getString(
                ProfilePreferences.BIO_KEY,
                ProfilePreferences.DEFAULT_BIO,
            ).orEmpty(),
        )
    }
    var avatarAccentIndex by rememberSaveable {
        mutableStateOf(
            preferences.getInt(
                ProfilePreferences.AVATAR_ACCENT_KEY,
                ProfilePreferences.DEFAULT_AVATAR_ACCENT,
            ),
        )
    }
    var isEditing by rememberSaveable { mutableStateOf(false) }
    var draftName by rememberSaveable { mutableStateOf(displayName) }
    var draftBio by rememberSaveable { mutableStateOf(bio) }
    var draftAvatarAccentIndex by rememberSaveable { mutableStateOf(avatarAccentIndex) }
    var isPickingPhoto by rememberSaveable { mutableStateOf(false) }
    var followsLastCompleted by rememberSaveable {
        mutableStateOf(preferences.getBoolean(ProfilePreferences.AVATAR_FOLLOWS_LAST_COMPLETED_KEY, false))
    }
    val setFollowsLastCompleted: (Boolean) -> Unit = { follows ->
        followsLastCompleted = follows
        preferences.edit().putBoolean(ProfilePreferences.AVATAR_FOLLOWS_LAST_COMPLETED_KEY, follows).apply()
    }
    val profileAccentColors = profileAccentColors()

    // The edit button lives in the top bar, which cannot see this state; hand it the action.
    headerActions.onEditRequested = {
        draftName = displayName
        draftBio = bio
        draftAvatarAccentIndex = avatarAccentIndex
        isEditing = true
    }
    val saveProfile = {
        displayName = draftName.trim().ifBlank { ProfilePreferences.DEFAULT_DISPLAY_NAME }
        bio = draftBio.trim()
        avatarAccentIndex = draftAvatarAccentIndex.coerceIn(profileAccentColors.indices)
        preferences.edit()
            .putString(ProfilePreferences.DISPLAY_NAME_KEY, displayName)
            .putString(ProfilePreferences.BIO_KEY, bio)
            .putInt(ProfilePreferences.AVATAR_ACCENT_KEY, avatarAccentIndex)
            .apply()
        isEditing = false
    }
    val persistProfileImage: (String) -> Unit = { localPath ->
        profileImagePath?.let { previousPath ->
            if (previousPath != localPath) File(previousPath).delete()
        }
        profileImagePath = localPath
        // Choosing a picture of your own ends following the latest finished title.
        setFollowsLastCompleted(false)
        preferences.edit()
            .putString(ProfilePreferences.AVATAR_IMAGE_PATH_KEY, localPath)
            .remove(ProfilePreferences.AVATAR_IMAGE_URI_KEY)
            .apply()
    }
    LaunchedEffect(legacyProfileImageUri, profileImagePath) {
        if (profileImagePath == null && !legacyProfileImageUri.isNullOrBlank()) {
            val migratedPath = runCatching {
                withContext(Dispatchers.IO) {
                    copyImageToProfileStorage(context, Uri.parse(legacyProfileImageUri))
                }
            }.getOrNull()
            if (migratedPath != null) {
                persistProfileImage(migratedPath)
            }
        }
    }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isImageLoading = true
                imageError = null
                val result = runCatching {
                    withContext(Dispatchers.IO) {
                        copyImageToProfileStorage(context, uri)
                    }
                }
                result.onSuccess {
                    persistProfileImage(it)
                    isPickingPhoto = false
                }
                result.onFailure { imageError = "No s'ha pogut desar aquesta imatge." }
                isImageLoading = false
            }
        }
    }
    val chooseProfileImage = {
        imagePickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }
    val removeProfileImage = {
        profileImagePath?.let { path -> File(path).delete() }
        profileImagePath = null
        imageError = null
        setFollowsLastCompleted(false)
        preferences.edit()
            .remove(ProfilePreferences.AVATAR_IMAGE_PATH_KEY)
            .remove(ProfilePreferences.AVATAR_IMAGE_URI_KEY)
            .apply()
    }
    // A library cover and a typed URL take the same path: fetch the image into profile storage rather
    // than holding a link that could rot.
    val downloadProfileImage: (String, String) -> Unit = { rawUrl, failureMessage ->
        val url = rawUrl.trim()
        if (url.isBlank()) {
            imageError = "Escriu una URL d'imatge abans de descarregar-la."
        } else {
            coroutineScope.launch {
                isImageLoading = true
                imageError = null
                val result = runCatching {
                    withContext(Dispatchers.IO) {
                        downloadImageToProfileStorage(context, url)
                    }
                }
                result.onSuccess {
                    persistProfileImage(it)
                    isPickingPhoto = false
                }
                result.onFailure { imageError = failureMessage }
                isImageLoading = false
            }
        }
    }

    val snapshot = remember(items) {
        StatsCalculator().calculate(
            items = items,
            filters = StatsFilters(period = StatsPeriod.AllTime),
        )
    }
    val accent = profileAccentColors[avatarAccentIndex.coerceIn(profileAccentColors.indices)].first
    val completedItems = remember(items) { items.completedNewestFirst() }
    val latestCompletedCoverUrl = completedItems.firstNotNullOfOrNull { it.item.coverUrl }
    val profileImage: Any? = if (followsLastCompleted) latestCompletedCoverUrl else profileImagePath?.let(::File)
    val completedByType = remember(completedItems) {
        MediaType.entries
            .map { type -> type to completedItems.count { it.item.type == type } }
    }
    // The photo picker offers the covers you have been busy with lately, whatever their status.
    val coverItems = remember(items) {
        items
            .filter { it.item.coverUrl != null }
            .sortedByDescending { it.currentSession?.updatedAtEpochMillis ?: 0L }
    }
    val objectiveProgress = remember(items, objectives) { ObjectiveCalculator().calculate(items, objectives) }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(OmnilogTheme.colors.appBackground),
        // Tucked up under the bar by as much as the item page, so every editorial page opens at the same height.
        contentPadding = PaddingValues(top = topInset - BarPaddingReclaim, bottom = 24.dp),
    ) {
        item(key = "header") {
            ProfileHeader(
                displayName = displayName,
                bio = bio,
                accent = accent,
                image = profileImage,
                totalTitles = snapshot.totalTitles,
                activeTitles = snapshot.activeNow,
                averageRating = snapshot.averageRating,
            )
        }
        item(key = "completed") {
            ProfileCompletedSummary(
                completedByType = completedByType,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
        if (completedItems.isNotEmpty()) {
            item(key = "recent_completed") {
                RelatedMediaSection(
                    title = "Darrers acabats",
                    relatedMedia = completedItems.take(RecentCompletedCount),
                    onMediaClick = onOpenMedia,
                    // Every one of these is finished; a tick on each would say nothing.
                    showStatus = false,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                )
            }
        }
        item(key = "objectives") {
            ProfileObjectivesSection(
                objectives = objectiveProgress,
                onSave = onSaveObjective,
                onDelete = onDeleteObjective,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }

    if (isEditing) {
        val draftAccent = profileAccentColors[draftAvatarAccentIndex.coerceIn(profileAccentColors.indices)].first
        EditorialSheet(
            title = "Edita el perfil",
            confirmText = "Desa",
            accent = draftAccent,
            onDismiss = { isEditing = false },
            onConfirm = saveProfile,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProfileAvatar(
                    displayName = draftName,
                    color = draftAccent,
                    image = profileImage,
                    shape = PortraitShape,
                    modifier = Modifier
                        .size(width = 64.dp, height = 96.dp)
                        .border(2.dp, draftAccent, PortraitShape)
                        .padding(3.dp)
                        .clip(PortraitShape)
                        .clickable { isPickingPhoto = true },
                )
                TextButton(onClick = { isPickingPhoto = true }) {
                    Text(
                        text = if (profileImage == null) "Afegeix una foto" else "Canvia la foto",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = draftAccent,
                    )
                }
            }
            OutlinedTextField(
                value = draftName,
                onValueChange = { draftName = it },
                label = { Text("Nom") },
                singleLine = true,
                colors = omnilogModalTextFieldColors(draftAccent),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = draftBio,
                onValueChange = { draftBio = it },
                label = { Text("Sobre tu") },
                placeholder = { Text("Una línia sobre el que llegeixes, mires o jugues") },
                maxLines = 3,
                colors = omnilogModalTextFieldColors(draftAccent),
                modifier = Modifier.fillMaxWidth(),
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "COLOR",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = OmnilogTheme.colors.appMuted,
                )
                AccentSwatches(
                    options = profileAccentColors,
                    selectedIndex = draftAvatarAccentIndex,
                    onSelect = { draftAvatarAccentIndex = it },
                )
            }
        }
    }

    if (isPickingPhoto) {
        ProfilePhotoSheet(
            coverItems = coverItems,
            imageUrl = profileImageUrl,
            isLoading = isImageLoading,
            errorMessage = imageError,
            hasPhoto = profileImagePath != null,
            followsLastCompleted = followsLastCompleted,
            lastCompletedCoverUrl = latestCompletedCoverUrl,
            onFollowLastCompletedChange = setFollowsLastCompleted,
            onCoverSelected = { downloadProfileImage(it, "No s'ha pogut fer servir aquesta portada.") },
            onImageUrlChange = { profileImageUrl = it },
            onDownloadFromUrl = {
                downloadProfileImage(profileImageUrl, "No s'ha pogut descarregar la imatge. Revisa la URL.")
            },
            onChooseFromGallery = chooseProfileImage,
            onRemovePhoto = {
                removeProfileImage()
                isPickingPhoto = false
            },
            onDismiss = {
                isPickingPhoto = false
                imageError = null
            },
        )
    }
}

@Composable
internal fun ProfileAvatar(
    displayName: String,
    color: Color,
    /** A stored photo as a [File], or a cover URL. */
    image: Any?,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (image != null) {
                AsyncImage(
                    model = image,
                    contentDescription = "Foto de perfil de $displayName",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = displayName.profileInitials(),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = SerifFontFamily,
                        fontWeight = FontWeight.Normal,
                    ),
                    color = OmnilogTheme.colors.appBackground,
                )
            }
        }
    }
}

/**
 * Titles finished at least once, newest finish first. A completion without a recorded day still
 * counts, but sorts after every dated one, ordered by when the title was last touched.
 */
internal fun List<TrackedMedia>.completedNewestFirst(): List<TrackedMedia> =
    mapNotNull { trackedMedia ->
        val dates = trackedMedia.sessions.flatMap { it.completionDates() }
        if (dates.isEmpty()) null else trackedMedia to (dates.filterNotNull().maxOrNull() ?: LocalDate.MIN)
    }
        .sortedWith(
            compareByDescending<Pair<TrackedMedia, LocalDate>> { it.second }
                .thenByDescending { it.first.currentSession?.updatedAtEpochMillis ?: 0L },
        )
        .map { it.first }

/** The cover of the latest finished title that has one: the profile picture while it follows them. */
internal fun List<TrackedMedia>.latestCompletedCoverUrl(): String? =
    completedNewestFirst().firstNotNullOfOrNull { it.item.coverUrl }

private const val MAX_PROFILE_IMAGE_BYTES = 10L * 1024L * 1024L

private fun copyImageToProfileStorage(context: Context, uri: Uri): String {
    val contentType = context.contentResolver.getType(uri)
    if (contentType != null && !contentType.startsWith("image/", ignoreCase = true)) {
        throw IOException("Selected file is not an image")
    }
    val inputStream = context.contentResolver.openInputStream(uri)
        ?: throw IOException("Could not open selected image")
    return inputStream.use { input ->
        writeProfileImage(context, input)
    }
}

private fun downloadImageToProfileStorage(context: Context, rawUrl: String): String {
    val url = URL(rawUrl)
    require(url.protocol.equals("http", ignoreCase = true) || url.protocol.equals("https", ignoreCase = true)) {
        "Only HTTP and HTTPS image URLs are supported"
    }
    val connection = (url.openConnection() as? HttpURLConnection)
        ?: throw IOException("Could not open image URL")
    return try {
        connection.connectTimeout = 15_000
        connection.readTimeout = 20_000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("Accept", "image/*")
        connection.setRequestProperty("User-Agent", "Omnilog/1.0")
        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            throw IOException("Image URL returned HTTP $responseCode")
        }
        val contentType = connection.contentType?.substringBefore(';')?.trim()
        if (contentType != null && !contentType.startsWith("image/", ignoreCase = true)) {
            throw IOException("URL did not return an image")
        }
        connection.inputStream.use { input ->
            writeProfileImage(context, input)
        }
    } finally {
        connection.disconnect()
    }
}

private fun writeProfileImage(context: Context, input: InputStream): String {
    val profileDirectory = File(context.filesDir, "profile").apply {
        if (!exists() && !mkdirs()) {
            throw IOException("Could not create profile image directory")
        }
    }
    val temporaryFile = File.createTempFile("avatar-", ".tmp", profileDirectory)
    return try {
        temporaryFile.outputStream().use { output ->
            copyWithLimit(input, output)
        }
        // A unique name per image, not a fixed "avatar". Writing every image to one path meant the
        // stored path never changed, so Compose saw no state change and Coil served the previous
        // bitmap from cache keyed on that path — you picked a new photo and nothing happened.
        val storedFile = File(profileDirectory, "avatar-${System.currentTimeMillis()}")
        if (!temporaryFile.renameTo(storedFile)) {
            throw IOException("Could not store profile image")
        }
        // The caller only knows the path it is replacing; anything older is orphaned here.
        profileDirectory.listFiles()
            ?.filter { it.name.startsWith("avatar") && it.name != storedFile.name }
            ?.forEach { it.delete() }
        storedFile.absolutePath
    } catch (error: Throwable) {
        temporaryFile.delete()
        throw error
    }
}

private fun copyWithLimit(input: InputStream, output: OutputStream) {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var totalBytes = 0L
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        if (read == 0) continue
        totalBytes += read
        if (totalBytes > MAX_PROFILE_IMAGE_BYTES) {
            throw IOException("Profile image is too large")
        }
        output.write(buffer, 0, read)
    }
    if (totalBytes == 0L) {
        throw IOException("Profile image is empty")
    }
}

/**
 * Resolved once per composition rather than held as a top-level constant: the swatches are theme
 * accents now, and the save lambda that indexes into this list runs outside composition, so it has
 * to capture a value rather than read a CompositionLocal. Each carries the name the picker shows.
 */
@Composable
@ReadOnlyComposable
private fun profileAccentColors(): List<Pair<Color, String>> = listOf(
    OmnilogTheme.accents.Dashboard to "Sàlvia",
    OmnilogTheme.accents.Anime to "Sakura",
    OmnilogTheme.accents.Books to "Ambre",
    OmnilogTheme.accents.Tv to "Turquesa",
    OmnilogTheme.accents.Games to "Mostassa",
)

private fun String.profileInitials(): String {
    val parts = trim().split(Regex("\\s+")).filter(String::isNotBlank)
    return when {
        parts.isEmpty() -> "O"
        parts.size == 1 -> parts.first().take(2).uppercase()
        else -> "${parts.first().first()}${parts.last().first()}".uppercase()
    }
}
