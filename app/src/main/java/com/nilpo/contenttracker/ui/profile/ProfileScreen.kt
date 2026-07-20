package com.nilpo.contenttracker.ui.profile

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nilpo.contenttracker.R
import com.nilpo.contenttracker.ui.ProfileHeaderActions
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.Objective
import com.nilpo.contenttracker.core.objectives.ObjectiveCalculator
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.stats.StatsCalculator
import com.nilpo.contenttracker.core.stats.StatsFilters
import com.nilpo.contenttracker.core.stats.StatsPeriod
import com.nilpo.contenttracker.ui.theme.OmnilogColors
import com.nilpo.contenttracker.ui.theme.OmnilogTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun ProfileScreen(
    items: List<TrackedMedia>,
    objectives: List<Objective> = emptyList(),
    onOpenMedia: (TrackedMedia) -> Unit,
    onSaveObjective: (Objective) -> Unit = {},
    onDeleteObjective: (Long) -> Unit = {},
    headerActions: ProfileHeaderActions = remember { ProfileHeaderActions() },
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val preferences = remember(context) { ProfilePreferences.from(context) }
    val coroutineScope = rememberCoroutineScope()
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
    var draftName by rememberSaveable(displayName) { mutableStateOf(displayName) }
    var draftBio by rememberSaveable(bio) { mutableStateOf(bio) }
    var draftAvatarAccentIndex by rememberSaveable(avatarAccentIndex) {
        mutableStateOf(avatarAccentIndex)
    }
    var isPickingPhoto by rememberSaveable { mutableStateOf(false) }
    val profileAccentColors = profileAccentColors()
    val draftAvatarColor =
        profileAccentColors[draftAvatarAccentIndex.coerceIn(profileAccentColors.indices)]

    // The edit controls live in the top bar, which cannot see this state; hand it the actions.
    headerActions.isEditing = isEditing
    headerActions.onEditRequested = {
        draftName = displayName
        draftBio = bio
        draftAvatarAccentIndex = avatarAccentIndex
        isEditing = true
    }
    headerActions.onCancelRequested = { isEditing = false }
    headerActions.onSaveRequested = {
        displayName = draftName.trim().ifBlank { ProfilePreferences.DEFAULT_DISPLAY_NAME }
        bio = draftBio.trim().ifBlank { ProfilePreferences.DEFAULT_BIO }
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
                result.onSuccess(persistProfileImage)
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
        preferences.edit()
            .remove(ProfilePreferences.AVATAR_IMAGE_PATH_KEY)
            .remove(ProfilePreferences.AVATAR_IMAGE_URI_KEY)
            .apply()
    }
    val downloadProfileImage: () -> Unit = {
        val url = profileImageUrl.trim()
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
                result.onSuccess(persistProfileImage)
                result.onFailure { imageError = "No s'ha pogut descarregar la imatge. Revisa la URL." }
                isImageLoading = false
            }
        }
    }

    // A library cover is a remote URL, so picking one takes the same path as the URL field: fetch
    // it into profile storage rather than holding a link that could rot.
    val useCoverAsPhoto: (String) -> Unit = { coverUrl ->
        coroutineScope.launch {
            isImageLoading = true
            imageError = null
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    downloadImageToProfileStorage(context, coverUrl)
                }
            }
            result.onSuccess {
                persistProfileImage(it)
                isPickingPhoto = false
            }
            result.onFailure { imageError = "No s'ha pogut fer servir aquesta portada." }
            isImageLoading = false
        }
    }

    val snapshot = remember(items) {
        StatsCalculator().calculate(
            items = items,
            filters = StatsFilters(period = StatsPeriod.AllTime),
        )
    }
    val avatarColor = profileAccentColors[avatarAccentIndex.coerceIn(profileAccentColors.indices)]
    val mediaTypeCounts = remember(items) {
        MediaType.entries.map { type ->
            type to items.count { it.item.type == type }
        }.filter { (_, count) -> count > 0 }
    }
    // Most recently touched first, so the banner reflects what you are actually reading and
    // watching rather than whatever happens to sit at the top of the library.
    val coverItems = remember(items) {
        items
            .filter { it.item.coverUrl != null }
            .sortedByDescending { it.currentSession?.updatedAtEpochMillis ?: 0L }
    }
    val bannerItems = remember(coverItems) { coverItems.take(12) }

    val objectiveProgress = remember(items, objectives) { ObjectiveCalculator().calculate(items, objectives) }


    Surface(
        modifier = modifier,
        color = OmnilogTheme.colors.appBackground,
    ) {
        // The page carries no horizontal padding of its own so the hero's banner can run to the
        // screen edges; every other section insets itself instead.
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ProfileHeroCard(
                    // While editing, the hero is bound to the draft rather than the saved values:
                    // it *is* the preview, so every keystroke and colour tap has to land here.
                    displayName = if (isEditing) draftName else displayName,
                    bio = if (isEditing) draftBio else bio,
                    avatarColor = if (isEditing) draftAvatarColor else avatarColor,
                    imagePath = profileImagePath,
                    bannerItems = bannerItems,
                    totalTitles = snapshot.totalTitles,
                    completedTitles = snapshot.uniqueTitlesCompleted,
                    activeTitles = snapshot.activeNow,
                    averageRating = snapshot.averageRating,
                    mediaTypeCounts = mediaTypeCounts,
                    editing = isEditing,
                    accentOptions = profileAccentColors,
                    selectedAccentIndex = draftAvatarAccentIndex,
                    onNameChange = { draftName = it },
                    onBioChange = { draftBio = it },
                    onAccentChange = { draftAvatarAccentIndex = it },
                    onPhotoClick = { isPickingPhoto = true },
                )
            }

            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ProfileObjectivesSection(
                        objectives = objectiveProgress,
                        onSave = onSaveObjective,
                        onDelete = onDeleteObjective,
                    )
                }
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
            onCoverSelected = useCoverAsPhoto,
            onImageUrlChange = { profileImageUrl = it },
            onDownloadFromUrl = downloadProfileImage,
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
    imagePath: String?,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
) {

    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (imagePath != null) {
                AsyncImage(
                    model = File(imagePath),
                    contentDescription = "Foto de perfil de $displayName",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = displayName.profileInitials(),
                    style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = OmnilogTheme.colors.appBackground,
                )
            }
        }
    }
}


@Composable
private fun IconButtonSurface(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
    
    Surface(
            modifier = Modifier
                .size(42.dp)
                .clickable(onClick = onClick),
            shape = CircleShape,
            color = OmnilogTheme.colors.appBackground,
        ) {
            Box(contentAlignment = Alignment.Center) { icon() }
        }
        Text(
            text = label,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = OmnilogTheme.colors.appMuted,
        )
    }
}

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
 * to capture a value rather than read a CompositionLocal.
 */
@Composable
@ReadOnlyComposable
private fun profileAccentColors(): List<Color> = listOf(
    OmnilogTheme.accents.Dashboard,
    OmnilogTheme.accents.Anime,
    OmnilogTheme.accents.Books,
    OmnilogTheme.accents.Tv,
    OmnilogTheme.accents.Games,
)

private fun String.profileInitials(): String {
    val parts = trim().split(Regex("\\s+")).filter(String::isNotBlank)
    return when {
        parts.isEmpty() -> "O"
        parts.size == 1 -> parts.first().take(2).uppercase()
        else -> "${parts.first().first()}${parts.last().first()}".uppercase()
    }
}
