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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.Objective
import com.nilpo.contenttracker.core.objectives.ObjectiveCalculator
import com.nilpo.contenttracker.core.model.TrackedMedia
import com.nilpo.contenttracker.core.model.TrackingStatus
import com.nilpo.contenttracker.core.stats.StatsCalculator
import com.nilpo.contenttracker.core.stats.StatsFilters
import com.nilpo.contenttracker.core.stats.StatsPeriod
import com.nilpo.contenttracker.ui.common.MetadataCoverImage
import com.nilpo.contenttracker.ui.common.displayMediaTitle
import com.nilpo.contenttracker.ui.theme.OmnilogColors
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

    val snapshot = remember(items) {
        StatsCalculator().calculate(
            items = items,
            filters = StatsFilters(period = StatsPeriod.AllTime),
        )
    }
    val avatarColor = profileAccentColors[avatarAccentIndex.coerceIn(profileAccentColors.indices)]
    val activeItems = remember(items) {
        items
            .filter { it.currentSession?.status == TrackingStatus.InProgress }
            .sortedWith(
                compareByDescending<TrackedMedia> { it.currentSession?.updatedAtEpochMillis ?: 0L }
                    .thenBy { displayMediaTitle(it.item.title).lowercase() },
            )
            .take(4)
    }
    val plannedItems = remember(items) {
        items
            .filter { it.currentSession?.status == TrackingStatus.Planned }
            .sortedBy { displayMediaTitle(it.item.title).lowercase() }
            .take(4)
    }
    val mediaTypeCounts = remember(items) {
        MediaType.entries.map { type ->
            type to items.count { it.item.type == type }
        }.filter { (_, count) -> count > 0 }
    }

    val objectiveProgress = remember(items, objectives) { ObjectiveCalculator().calculate(items, objectives) }


    Surface(
        modifier = modifier,
        color = OmnilogColors.AppBackground,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ProfileHero(
                    displayName = displayName,
                    bio = bio,
                    avatarColor = avatarColor,
                    imagePath = profileImagePath,
                    onEdit = {
                        draftName = displayName
                        draftBio = bio
                        draftAvatarAccentIndex = avatarAccentIndex
                        isEditing = !isEditing
                    },
                )
            }

            if (isEditing) {
                item {
                    ProfileEditor(
                        displayName = draftName,
                        bio = draftBio,
                        avatarAccentIndex = draftAvatarAccentIndex,
                        hasProfileImage = profileImagePath != null,
                        imageUrl = profileImageUrl,
                        isImageLoading = isImageLoading,
                        imageError = imageError,
                        onDisplayNameChange = { draftName = it },
                        onBioChange = { draftBio = it },
                        onAvatarAccentChange = { draftAvatarAccentIndex = it },
                        onImageUrlChange = { profileImageUrl = it },
                        onChoosePhoto = chooseProfileImage,
                        onDownloadFromUrl = downloadProfileImage,
                        onRemovePhoto = removeProfileImage,
                        onCancel = { isEditing = false },
                        onSave = {
                            displayName = draftName.trim().ifBlank { ProfilePreferences.DEFAULT_DISPLAY_NAME }
                            bio = draftBio.trim().ifBlank { ProfilePreferences.DEFAULT_BIO }
                            avatarAccentIndex = draftAvatarAccentIndex.coerceIn(profileAccentColors.indices)
                            preferences.edit()
                                .putString(ProfilePreferences.DISPLAY_NAME_KEY, displayName)
                                .putString(ProfilePreferences.BIO_KEY, bio)
                                .putInt(ProfilePreferences.AVATAR_ACCENT_KEY, avatarAccentIndex)
                                .apply()
                            isEditing = false
                        },
                    )
                }
            }

            item {
                ProfileStatsCard(
                    totalTitles = snapshot.totalTitles,
                    completedTitles = snapshot.completedInPeriod,
                    activeTitles = snapshot.activeNow,
                    averageRating = snapshot.averageRating,
                )
            }

            item {
                ProfileObjectivesSection(
                    objectives = objectiveProgress,
                    onSave = onSaveObjective,
                    onDelete = onDeleteObjective,
                )
            }

            item {
                ProfileActivityCard(
                    activeItems = activeItems,
                    plannedItems = plannedItems,
                    activeCount = items.count { it.currentSession?.status == TrackingStatus.InProgress },
                    plannedCount = items.count { it.currentSession?.status == TrackingStatus.Planned },
                    onOpenMedia = onOpenMedia,
                )
            }

            item {
                ProfileLibraryMixCard(
                    mediaTypeCounts = mediaTypeCounts,
                    topGenres = snapshot.topGenres.take(3).map { it.label },
                )
            }

        }
    }
}

@Composable
private fun ProfileHero(
    displayName: String,
    bio: String,
    avatarColor: Color,
    imagePath: String?,
    onEdit: () -> Unit,
) {

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = OmnilogColors.AppPanel,
        border = BorderStroke(1.dp, avatarColor.copy(alpha = 0.48f)),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileAvatar(
                displayName = displayName,
                color = avatarColor,
                imagePath = imagePath,
                modifier = Modifier.size(74.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = displayName,
                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = OmnilogColors.AppInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = bio,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = OmnilogColors.AppMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        
    Surface(
                modifier = Modifier
                    .size(34.dp)
                    .clickable(onClick = onEdit),
                shape = CircleShape,
                color = OmnilogColors.AppBackground,
                border = BorderStroke(1.dp, avatarColor.copy(alpha = 0.45f)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edita el perfil",
                        tint = avatarColor,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileEditor(
    displayName: String,
    bio: String,
    avatarAccentIndex: Int,
    hasProfileImage: Boolean,
    imageUrl: String,
    isImageLoading: Boolean,
    imageError: String?,
    onDisplayNameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onAvatarAccentChange: (Int) -> Unit,
    onImageUrlChange: (String) -> Unit,
    onChoosePhoto: () -> Unit,
    onDownloadFromUrl: () -> Unit,
    onRemovePhoto: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = OmnilogColors.AppPanel,
        border = BorderStroke(1.dp, OmnilogColors.AppLine),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Personalitza el perfil",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = OmnilogColors.AppInk,
            )
            OutlinedTextField(
                value = displayName,
                onValueChange = onDisplayNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nom") },
                singleLine = true,
            )
            OutlinedTextField(
                value = bio,
                onValueChange = onBioChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Descripció") },
                minLines = 2,
                maxLines = 3,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Foto de perfil",
                        style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = OmnilogColors.AppMuted,
                    )
                    Text(
                        text = if (hasProfileImage) "Foto personalitzada activa" else "Fes que el perfil sigui teu",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = OmnilogColors.AppMuted,
                    )
                }
                OutlinedButton(onClick = onChoosePhoto) {
                    Icon(Icons.Filled.Edit, contentDescription = null)
                    Text("Tria")
                }
                if (hasProfileImage) {
                    IconButtonSurface(
                        icon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = OmnilogColors.AppMuted) },
                        label = "Treu",
                        onClick = onRemovePhoto,
                    )
                }
            }
            OutlinedTextField(
                value = imageUrl,
                onValueChange = onImageUrlChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("URL de la foto") },
                placeholder = { Text("https://exemple.com/foto.jpg") },
                singleLine = true,
                enabled = !isImageLoading,
            )
            OutlinedButton(
                onClick = onDownloadFromUrl,
                enabled = imageUrl.isNotBlank() && !isImageLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isImageLoading) "Descarregant…" else "Descarrega la foto des de la URL")
            }
            imageError?.let { error ->
                Text(
                    text = error,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                )
            }
            Text(
                text = "Color de l'avatar",
                style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = OmnilogColors.AppMuted,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                profileAccentColors.forEachIndexed { index, color ->
                    AvatarColorOption(
                        color = color,
                        selected = avatarAccentIndex == index,
                        onClick = { onAvatarAccentChange(index) },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onCancel) { Text("Cancel·la") }
                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OmnilogColors.Dashboard,
                        contentColor = OmnilogColors.AppBackground,
                    ),
                ) { Text("Desa") }
            }
        }
    }
}

@Composable
private fun AvatarColorOption(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {

    Surface(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = color,
        border = if (selected) BorderStroke(3.dp, OmnilogColors.AppInk) else null,
    ) {}
}

@Composable
private fun ProfileAvatar(
    displayName: String,
    color: Color,
    imagePath: String?,
    modifier: Modifier = Modifier,
) {

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = color,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (imagePath != null) {
                AsyncImage(
                    model = File(imagePath),
                    contentDescription = "Foto de perfil de $displayName",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = displayName.profileInitials(),
                    style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = OmnilogColors.AppBackground,
                )
            }
        }
    }
}

@Composable
private fun ProfileStatsCard(
    totalTitles: Int,
    completedTitles: Int,
    activeTitles: Int,
    averageRating: Double?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "La teva biblioteca",
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = OmnilogColors.AppInk,
        )
    
    Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = OmnilogColors.AppPanel,
            border = BorderStroke(1.dp, OmnilogColors.AppLine),
        ) {
            Row(
                modifier = Modifier.padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ProfileMetric(value = totalTitles.toString(), label = "Títols")
                ProfileMetric(value = completedTitles.toString(), label = "Completats")
                ProfileMetric(value = activeTitles.toString(), label = "En curs")
                ProfileMetric(value = averageRating?.let { "%.1f".format(it) } ?: "—", label = "Nota")
            }
        }
    }
}

@Composable
private fun ProfileMetric(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = value,
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = OmnilogColors.Dashboard,
        )
        Text(
            text = label,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = OmnilogColors.AppMuted,
        )
    }
}

@Composable
private fun ProfileActivityCard(
    activeItems: List<TrackedMedia>,
    plannedItems: List<TrackedMedia>,
    activeCount: Int,
    plannedCount: Int,
    onOpenMedia: (TrackedMedia) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Ara mateix",
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = OmnilogColors.AppInk,
        )
    
    Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = OmnilogColors.AppPanel,
            border = BorderStroke(1.dp, OmnilogColors.AppLine),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ProfileActivityMetric(
                        value = activeCount.toString(),
                        label = "En curs",
                        accent = OmnilogColors.Tv,
                        modifier = Modifier.weight(1f),
                    )
                    ProfileActivityMetric(
                        value = plannedCount.toString(),
                        label = "A continuació",
                        accent = OmnilogColors.Dashboard,
                        modifier = Modifier.weight(1f),
                    )
                }

                if (activeItems.isNotEmpty()) {
                    ProfileActivityList(
                        title = "Continua on ho vas deixar",
                        items = activeItems,
                        onOpenMedia = onOpenMedia,
                    )
                }
                if (plannedItems.isNotEmpty()) {
                    ProfileActivityList(
                        title = "La teva cua",
                        items = plannedItems,
                        onOpenMedia = onOpenMedia,
                    )
                }
                if (activeItems.isEmpty() && plannedItems.isEmpty()) {
                    Text(
                        text = "Quan tinguis algun títol en curs o planificat, el veuràs aquí.",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = OmnilogColors.AppMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileActivityMetric(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.10f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = value,
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
            )
            Text(
                text = label,
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = OmnilogColors.AppMuted,
            )
        }
    }
}

@Composable
private fun ProfileActivityList(
    title: String,
    items: List<TrackedMedia>,
    onOpenMedia: (TrackedMedia) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = OmnilogColors.AppMuted,
        )
        items.forEach { trackedMedia ->
            ProfileMediaRow(
                trackedMedia = trackedMedia,
                onClick = { onOpenMedia(trackedMedia) },
            )
        }
    }
}

@Composable
private fun ProfileMediaRow(
    trackedMedia: TrackedMedia,
    onClick: () -> Unit,
) {

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = OmnilogColors.AppBackground,
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetadataCoverImage(
                coverUrl = trackedMedia.item.coverUrl,
                modifier = Modifier.size(width = 44.dp, height = 58.dp),
                shape = RoundedCornerShape(8.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = displayMediaTitle(trackedMedia.item.title),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = OmnilogColors.AppInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = trackedMedia.profileProgressLabel(),
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = OmnilogColors.AppMuted,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = OmnilogColors.AppMuted,
            )
        }
    }
}

@Composable
private fun ProfileLibraryMixCard(
    mediaTypeCounts: List<Pair<MediaType, Int>>,
    topGenres: List<String>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "La teva col·lecció",
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = OmnilogColors.AppInk,
        )
    
    Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = OmnilogColors.AppPanel,
            border = BorderStroke(1.dp, OmnilogColors.AppLine),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (mediaTypeCounts.isEmpty()) {
                    Text(
                        text = "Encara no hi ha cap títol a la biblioteca.",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = OmnilogColors.AppMuted,
                    )
                } else {
                    mediaTypeCounts.forEach { (type, count) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                        
    Surface(
                                modifier = Modifier.size(10.dp),
                                shape = CircleShape,
                                color = type.profileAccent(),
                            ) {}
                            Text(
                                text = type.profileLabel(),
                                modifier = Modifier.weight(1f),
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                color = OmnilogColors.AppInk,
                            )
                            Text(
                                text = count.toString(),
                                style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = type.profileAccent(),
                            )
                        }
                    }
                }
                if (topGenres.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(OmnilogColors.AppLine),
                    )
                    Text(
                        text = "Gèneres que més repeteixes",
                        style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = OmnilogColors.AppMuted,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        topGenres.forEach { genre ->
                        
    Surface(
                                shape = RoundedCornerShape(50),
                                color = OmnilogColors.Dashboard.copy(alpha = 0.12f),
                            ) {
                                Text(
                                    text = genre,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                    color = OmnilogColors.Dashboard,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun TrackedMedia.profileProgressLabel(): String {
    val session = currentSession ?: return item.type.profileLabel()
    return when (session.status) {
        TrackingStatus.InProgress -> {
            val total = item.progressTotal
            if (total != null && total > 0) {
                "${session.progressCurrent}/$total ${item.type.profileProgressUnit()}"
            } else {
                "En curs"
            }
        }
        TrackingStatus.Planned -> "A continuació"
        TrackingStatus.Completed -> "Completat"
        TrackingStatus.Paused -> "En pausa"
        TrackingStatus.Dropped -> "Abandonat"
    }
}

private fun MediaType.profileLabel(): String = when (this) {
    MediaType.Anime -> "Anime"
    MediaType.Book -> "Llibres"
    MediaType.Movie -> "Pel·lícules"
    MediaType.TvShow -> "Sèries"
    MediaType.Game -> "Jocs"
}

private fun MediaType.profileProgressUnit(): String = when (this) {
    MediaType.Anime, MediaType.TvShow -> "ep."
    MediaType.Book -> "pàg."
    MediaType.Movie -> "min"
    MediaType.Game -> "h"
}

private fun MediaType.profileAccent(): Color = when (this) {
    MediaType.Anime -> OmnilogColors.Anime
    MediaType.Book -> OmnilogColors.Books
    MediaType.Movie -> OmnilogColors.Dashboard
    MediaType.TvShow -> OmnilogColors.Tv
    MediaType.Game -> OmnilogColors.Games
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
            color = OmnilogColors.AppBackground,
        ) {
            Box(contentAlignment = Alignment.Center) { icon() }
        }
        Text(
            text = label,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = OmnilogColors.AppMuted,
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
        val storedFile = File(profileDirectory, "avatar")
        if (storedFile.exists() && !storedFile.delete()) {
            throw IOException("Could not replace previous profile image")
        }
        if (!temporaryFile.renameTo(storedFile)) {
            throw IOException("Could not store profile image")
        }
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

private val profileAccentColors = listOf(
    OmnilogColors.Dashboard,
    OmnilogColors.Anime,
    OmnilogColors.Books,
    OmnilogColors.Tv,
    OmnilogColors.Games,
)

private fun String.profileInitials(): String {
    val parts = trim().split(Regex("\\s+")).filter(String::isNotBlank)
    return when {
        parts.isEmpty() -> "O"
        parts.size == 1 -> parts.first().take(2).uppercase()
        else -> "${parts.first().first()}${parts.last().first()}".uppercase()
    }
}
