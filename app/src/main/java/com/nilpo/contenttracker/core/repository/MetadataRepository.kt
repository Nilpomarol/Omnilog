package com.nilpo.contenttracker.core.repository

import com.nilpo.contenttracker.core.model.MetadataSearchRequest
import com.nilpo.contenttracker.core.model.MetadataSuggestion

interface MetadataRepository {
    suspend fun searchSuggestions(request: MetadataSearchRequest): List<MetadataSuggestion>
}
