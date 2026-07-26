package com.nilpo.contenttracker.core.imports

import com.nilpo.contenttracker.core.model.MediaType
import com.nilpo.contenttracker.core.model.MetadataSource
import com.nilpo.contenttracker.core.model.MetadataSuggestion
import com.nilpo.contenttracker.core.repository.MetadataRefreshChange
import com.nilpo.contenttracker.core.repository.MetadataRefreshField
import com.nilpo.contenttracker.core.repository.MetadataRefreshPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportEnrichmentPolicyTest {
    @Test
    fun `exact enrichment replaces populated import values but preserves manual fields`() {
        val preview = preview(
            change(MetadataRefreshField.Cover, overwrites = false),
            change(MetadataRefreshField.Synopsis, overwrites = false),
            change(MetadataRefreshField.Title, overwrites = true),
            change(MetadataRefreshField.Genres, overwrites = true),
            change(MetadataRefreshField.Creators, overwrites = true),
            change(MetadataRefreshField.ProgressTotal, overwrites = true),
            change(MetadataRefreshField.ExternalRatings, overwrites = true),
            change(MetadataRefreshField.ReleaseYear, overwrites = true, locallyOverridden = true),
        )

        listOf(ImportSource.ImdbCsv, ImportSource.StoryGraphCsv).forEach { source ->
            val automatic = preview.autoApplicableImportedFields(
                source,
                AnimeTitlePreference.EnglishWithRomajiOriginal,
            )

            assertEquals(
                setOf(
                    MetadataRefreshField.Cover,
                    MetadataRefreshField.Synopsis,
                    MetadataRefreshField.Title,
                    MetadataRefreshField.Genres,
                    MetadataRefreshField.Creators,
                    MetadataRefreshField.ProgressTotal,
                    MetadataRefreshField.ExternalRatings,
                ),
                automatic,
            )
            assertTrue(preview.hasReviewChangesAfter(automatic))
        }
    }

    @Test
    fun `an exact result with only import-owned changes needs no review`() {
        val preview = preview(
            change(MetadataRefreshField.Cover, overwrites = false),
            change(MetadataRefreshField.ReleaseYear, overwrites = true),
            change(MetadataRefreshField.Genres, overwrites = true),
            change(MetadataRefreshField.Creators, overwrites = true),
            change(MetadataRefreshField.ProgressTotal, overwrites = true),
            change(MetadataRefreshField.ExternalRatings, overwrites = true),
        )
        val automatic = preview.autoApplicableImportedFields(
            ImportSource.ImdbCsv,
            AnimeTitlePreference.EnglishWithRomajiOriginal,
        )

        assertFalse(preview.hasReviewChangesAfter(automatic))
    }

    @Test
    fun `review selection is rejected after the selected local value changes`() {
        val shown = preview(
            MetadataRefreshChange(
                field = MetadataRefreshField.Title,
                currentValue = "Old title",
                newValue = "Provider title",
                overwritesExistingValue = true,
            ),
        )
        val fresh = preview(
            MetadataRefreshChange(
                field = MetadataRefreshField.Title,
                currentValue = "Edited title",
                newValue = "Provider title",
                overwritesExistingValue = true,
            ),
        )

        assertFalse(fresh.selectedChangesMatch(shown, setOf(MetadataRefreshField.Title)))
        assertTrue(fresh.selectedChangesMatch(shown, emptySet()))
    }

    @Test
    fun `MAL title preference applies provider titles while preserving manual title edits`() {
        val automatic = preview(
            change(MetadataRefreshField.Title, overwrites = true),
            change(MetadataRefreshField.OriginalTitle, overwrites = true),
        )
        val protected = preview(
            change(MetadataRefreshField.Title, overwrites = true, locallyOverridden = true),
            change(MetadataRefreshField.OriginalTitle, overwrites = true),
        )

        assertEquals(
            setOf(MetadataRefreshField.Title, MetadataRefreshField.OriginalTitle),
            automatic.autoApplicableImportedFields(
                ImportSource.MalApi,
                AnimeTitlePreference.EnglishWithRomajiOriginal,
            ),
        )
        assertEquals(
            setOf(MetadataRefreshField.OriginalTitle),
            protected.autoApplicableImportedFields(
                ImportSource.MalXml,
                AnimeTitlePreference.EnglishWithRomajiOriginal,
            ),
        )
        assertEquals(
            emptySet<MetadataRefreshField>(),
            automatic.autoApplicableImportedFields(
                ImportSource.MalApi,
                AnimeTitlePreference.KeepMalTitle,
            ),
        )
    }

    @Test
    fun `keeping MAL title still allows empty original title to be enriched`() {
        val preview = preview(
            change(MetadataRefreshField.Title, overwrites = true),
            change(MetadataRefreshField.OriginalTitle, overwrites = false),
            change(MetadataRefreshField.Genres, overwrites = true),
        )

        assertEquals(
            setOf(MetadataRefreshField.OriginalTitle, MetadataRefreshField.Genres),
            preview.autoApplicableImportedFields(
                ImportSource.MalApi,
                AnimeTitlePreference.KeepMalTitle,
            ),
        )
    }

    @Test
    fun `MAL short and slugged URLs with the same id do not require review`() {
        val urlPreview = preview(
            MetadataRefreshChange(
                field = MetadataRefreshField.SourceUrl,
                currentValue = "https://myanimelist.net/anime/5114",
                newValue = "https://myanimelist.net/anime/5114/Fullmetal_Alchemist_Brotherhood",
                overwritesExistingValue = true,
            ),
        )
        val reference = ProviderReference(
            source = MetadataSource.Jikan,
            externalId = "5114",
            mediaType = MediaType.Anime,
            evidence = "Exact MAL ID",
        )

        assertEquals(setOf(MetadataRefreshField.SourceUrl), urlPreview.equivalentMalUrlFields(reference))
        assertFalse(urlPreview.hasReviewChangesAfter(urlPreview.equivalentMalUrlFields(reference)))
    }

    private fun preview(vararg changes: MetadataRefreshChange) = MetadataRefreshPreview(
        mediaItemId = 1,
        refreshed = MetadataSuggestion(
            source = MetadataSource.Jikan,
            externalId = "5114",
            mediaType = MediaType.Anime,
            title = "Resolved title",
        ),
        changes = changes.toList(),
    )

    private fun change(
        field: MetadataRefreshField,
        overwrites: Boolean,
        locallyOverridden: Boolean = false,
    ) = MetadataRefreshChange(
        field = field,
        currentValue = if (overwrites) "Current" else "—",
        newValue = "Remote",
        overwritesExistingValue = overwrites,
        isLocallyOverridden = locallyOverridden,
    )
}
