package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MetadataConfirmationTest {
    @Test
    fun safeFillsDoNotRequireConfirmation() {
        val preview = preview(
            change(
                field = MetadataRefreshField.Cover,
                currentValue = "Empty",
                newValue = "https://example.com/cover.jpg",
                overwrites = false,
            ),
        )

        assertFalse(preview.requiresMetadataConfirmation())
        assertEquals(setOf(MetadataRefreshField.Cover), preview.defaultSelectedMetadataFields())
    }

    @Test
    fun overwritesRequireConfirmation() {
        val preview = preview(
            change(
                field = MetadataRefreshField.Title,
                currentValue = "Local title",
                newValue = "Provider title",
                overwrites = true,
            ),
        )

        assertTrue(preview.requiresMetadataConfirmation())
        assertEquals(setOf(MetadataRefreshField.Title), preview.defaultSelectedMetadataFields())
    }

    @Test
    fun locallyEditedFieldsRequireConfirmationAndStartUnchecked() {
        val preview = preview(
            change(
                field = MetadataRefreshField.Synopsis,
                currentValue = "My synopsis",
                newValue = "Provider synopsis",
                overwrites = true,
                locallyOverridden = true,
            ),
            change(
                field = MetadataRefreshField.Genres,
                currentValue = "Empty",
                newValue = "Drama",
                overwrites = false,
            ),
        )

        assertTrue(preview.requiresMetadataConfirmation())
        assertEquals(setOf(MetadataRefreshField.Genres), preview.defaultSelectedMetadataFields())
    }

    private fun preview(vararg changes: MetadataRefreshChange) = MetadataRefreshPreview(
        mediaItemId = 1,
        refreshed = MetadataSuggestion(
            source = MetadataSource.AniList,
            externalId = "1",
            mediaType = MediaType.Anime,
            title = "Provider title",
        ),
        changes = changes.toList(),
    )

    private fun change(
        field: MetadataRefreshField,
        currentValue: String,
        newValue: String,
        overwrites: Boolean,
        locallyOverridden: Boolean = false,
    ) = MetadataRefreshChange(
        field = field,
        currentValue = currentValue,
        newValue = newValue,
        overwritesExistingValue = overwrites,
        isLocallyOverridden = locallyOverridden,
    )
}
