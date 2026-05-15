package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.MetadataSearchRequest
import com.nilpo.contenttracker.core.model.MetadataSuggestion

class NoOpMetadataRepository : MetadataRepository {
    override suspend fun searchSuggestions(
        request: MetadataSearchRequest,
    ): List<MetadataSuggestion> {
        return emptyList()
    }
}
