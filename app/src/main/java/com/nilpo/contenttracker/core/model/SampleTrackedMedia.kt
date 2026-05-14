package com.nilpo.contenttracker.core.model

import java.time.LocalDate

object SampleTrackedMedia {
    val items = listOf(
        TrackedMedia(
            item = MediaItem(
                id = 1,
                type = MediaType.Anime,
                title = "Fullmetal Alchemist: Brotherhood",
                progressTotal = 64,
            ),
            sessions = listOf(
                TrackingSession(
                    id = 1,
                    mediaItemId = 1,
                    sessionNumber = 1,
                    status = TrackingStatus.Completed,
                    progressCurrent = 64,
                    rating = 10,
                    platform = ConsumptionPlatform("Crunchyroll", ConsumptionPlatformType.Streaming),
                    finishedAt = LocalDate.of(2024, 8, 12),
                ),
                TrackingSession(
                    id = 2,
                    mediaItemId = 1,
                    sessionNumber = 2,
                    status = TrackingStatus.InProgress,
                    progressCurrent = 18,
                    platform = ConsumptionPlatform("Crunchyroll", ConsumptionPlatformType.Streaming),
                    startedAt = LocalDate.of(2026, 5, 1),
                ),
            ),
            externalRatings = listOf(
                ExternalRating(
                    id = 1,
                    mediaItemId = 1,
                    source = ExternalRatingSource.Mal,
                    score = 9.1,
                    maxScore = 10.0,
                ),
            ),
            externalTracking = listOf(
                ExternalTracking(
                    id = 1,
                    mediaItemId = 1,
                    source = ExternalTrackingSource.Mal,
                    externalItemId = "5114",
                    url = "https://myanimelist.net/anime/5114",
                    isSynced = true,
                ),
            ),
        ),
        TrackedMedia(
            item = MediaItem(
                id = 3,
                type = MediaType.Book,
                title = "Dune",
                progressTotal = 688,
                ownership = Ownership(
                    isOwned = true,
                    type = OwnershipType.Physical,
                ),
            ),
            sessions = listOf(
                TrackingSession(
                    id = 3,
                    mediaItemId = 3,
                    sessionNumber = 1,
                    status = TrackingStatus.Completed,
                    progressCurrent = 688,
                    rating = 9,
                    platform = ConsumptionPlatform("Physical", ConsumptionPlatformType.Physical),
                ),
                TrackingSession(
                    id = 4,
                    mediaItemId = 3,
                    sessionNumber = 2,
                    status = TrackingStatus.InProgress,
                    progressCurrent = 210,
                    platform = ConsumptionPlatform("Kindle", ConsumptionPlatformType.Ebook),
                ),
            ),
            externalRatings = listOf(
                ExternalRating(
                    id = 2,
                    mediaItemId = 3,
                    source = ExternalRatingSource.Goodreads,
                    score = 4.3,
                    maxScore = 5.0,
                ),
            ),
            externalTracking = listOf(
                ExternalTracking(
                    id = 2,
                    mediaItemId = 3,
                    source = ExternalTrackingSource.StoryGraph,
                    isSynced = false,
                ),
                ExternalTracking(
                    id = 3,
                    mediaItemId = 3,
                    source = ExternalTrackingSource.Goodreads,
                    isSynced = true,
                ),
            ),
        ),
        TrackedMedia(
            item = MediaItem(
                id = 5,
                type = MediaType.Movie,
                title = "The Matrix",
                progressTotal = 1,
            ),
            sessions = listOf(
                TrackingSession(
                    id = 5,
                    mediaItemId = 5,
                    sessionNumber = 1,
                    status = TrackingStatus.Completed,
                    progressCurrent = 1,
                    rating = 8,
                    platform = ConsumptionPlatform("Netflix", ConsumptionPlatformType.Streaming),
                ),
                TrackingSession(
                    id = 6,
                    mediaItemId = 5,
                    sessionNumber = 2,
                    status = TrackingStatus.Completed,
                    progressCurrent = 1,
                    rating = 9,
                    platform = ConsumptionPlatform("Blu-ray", ConsumptionPlatformType.Physical),
                ),
            ),
            externalRatings = listOf(
                ExternalRating(
                    id = 3,
                    mediaItemId = 5,
                    source = ExternalRatingSource.Imdb,
                    score = 8.7,
                    maxScore = 10.0,
                ),
            ),
            externalTracking = listOf(
                ExternalTracking(
                    id = 4,
                    mediaItemId = 5,
                    source = ExternalTrackingSource.Imdb,
                    externalItemId = "tt0133093",
                    url = "https://www.imdb.com/title/tt0133093/",
                    isSynced = true,
                ),
            ),
        ),
        TrackedMedia(
            item = MediaItem(
                id = 7,
                type = MediaType.TvShow,
                title = "Severance - Season 1",
                progressTotal = 9,
            ),
            sessions = listOf(
                TrackingSession(
                    id = 7,
                    mediaItemId = 7,
                    sessionNumber = 1,
                    status = TrackingStatus.InProgress,
                    progressCurrent = 9,
                    rating = 9,
                    platform = ConsumptionPlatform("Apple TV+", ConsumptionPlatformType.Streaming),
                ),
            ),
            externalRatings = listOf(
                ExternalRating(
                    id = 4,
                    mediaItemId = 7,
                    source = ExternalRatingSource.Imdb,
                    score = 8.7,
                    maxScore = 10.0,
                ),
                ExternalRating(
                    id = 5,
                    mediaItemId = 7,
                    source = ExternalRatingSource.Tmdb,
                    score = 8.4,
                    maxScore = 10.0,
                ),
            ),
            externalTracking = listOf(
                ExternalTracking(
                    id = 5,
                    mediaItemId = 7,
                    source = ExternalTrackingSource.Imdb,
                    externalItemId = "tt11280740",
                    isSynced = false,
                ),
            ),
        ),
        TrackedMedia(
            item = MediaItem(
                id = 8,
                type = MediaType.Game,
                title = "Elden Ring",
                progressTotal = 95,
                ownership = Ownership(
                    isOwned = true,
                    type = OwnershipType.Digital,
                ),
            ),
            sessions = listOf(
                TrackingSession(
                    id = 8,
                    mediaItemId = 8,
                    sessionNumber = 1,
                    status = TrackingStatus.Completed,
                    progressCurrent = 95,
                    rating = 10,
                    platform = ConsumptionPlatform("Steam", ConsumptionPlatformType.DigitalStore),
                ),
                TrackingSession(
                    id = 9,
                    mediaItemId = 8,
                    sessionNumber = 2,
                    status = TrackingStatus.Planned,
                    progressCurrent = 0,
                    platform = ConsumptionPlatform("Steam", ConsumptionPlatformType.DigitalStore),
                ),
            ),
            externalRatings = listOf(
                ExternalRating(
                    id = 6,
                    mediaItemId = 8,
                    source = ExternalRatingSource.Metacritic,
                    score = 96.0,
                    maxScore = 100.0,
                ),
                ExternalRating(
                    id = 7,
                    mediaItemId = 8,
                    source = ExternalRatingSource.Rawg,
                    score = 4.6,
                    maxScore = 5.0,
                ),
            ),
            externalTracking = listOf(
                ExternalTracking(
                    id = 6,
                    mediaItemId = 8,
                    source = ExternalTrackingSource.Backloggd,
                    isSynced = false,
                ),
            ),
        ),
    )
}
