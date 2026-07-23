package com.nilpo.contenttracker.core.mal

import com.nilpo.contenttracker.core.model.TrackingStatus
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.ArrayDeque

class MalAccountImportLoaderTest {
    @Test
    fun `official API page maps into normalized MAL import rows`() {
        val firstPage = JSONObject(fixture("mal-api-page-1.json")).toMalAnimeListPage()

        assertEquals(2, firstPage.items.size)
        assertEquals(NextPageUrl, firstPage.nextPageUrl)

        val completed = firstPage.items[0]
        assertEquals(5114, completed.malId)
        assertEquals("Fullmetal Alchemist: Brotherhood", completed.title)
        assertEquals("TV", completed.seriesType)
        assertEquals(64, completed.episodeTotal)
        assertEquals(64, completed.watchedEpisodes)
        assertEquals(10, completed.rating)
        assertEquals(TrackingStatus.Completed, completed.status)
        assertEquals(LocalDate.of(2020, 1, 2), completed.startedAt)
        assertEquals(LocalDate.of(2020, 3, 4), completed.finishedAt)
        assertEquals("Imported from the account API", completed.notes)
        assertEquals(listOf("favorites", "rewatch"), completed.tags)

        val watching = firstPage.items[1]
        assertEquals(TrackingStatus.InProgress, watching.status)
        assertEquals(12, watching.watchedEpisodes)
        assertNull(watching.rating)
        assertNull(watching.finishedAt)

        val finalPage = JSONObject(fixture("mal-api-page-2.json")).toMalAnimeListPage()
        assertNull(finalPage.nextPageUrl)
        assertEquals("OVA", finalPage.items.single().seriesType)
        assertEquals(TrackingStatus.Paused, finalPage.items.single().status)
        assertNull(finalPage.items.single().startedAt)
        assertNull(finalPage.items.single().notes)
    }

    @Test
    fun `untrusted pagination URL is rejected before a bearer token could be sent`() {
        val json = JSONObject()
            .put("data", org.json.JSONArray())
            .put("paging", JSONObject().put("next", "https://example.com/steal-token"))

        val error = runCatching { json.toMalAnimeListPage() }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun `loader follows every page and reports cumulative progress`() = runBlocking {
        val api = FakeMalApiService().apply {
            enqueuePage(null, page("mal-api-page-1.json"))
            enqueuePage(NextPageUrl, page("mal-api-page-2.json"))
        }
        val store = FakeMalCredentialStore(validTokens())
        val loader = loader(api, store)
        val progress = mutableListOf<Pair<Int, Int>>()

        val rows = loader.fetchAll { itemCount, pageCount -> progress += itemCount to pageCount }

        assertEquals(listOf(5114, 52991, 9253), rows.map { it.malId })
        assertEquals(listOf(2 to 1, 3 to 2), progress)
        assertEquals(listOf(null, NextPageUrl), api.pageCalls.map { it.second })
        assertFalse(store.wasCleared)
    }

    @Test
    fun `expired token refreshes before the first list page`() = runBlocking {
        val api = FakeMalApiService().apply {
            refreshedTokens = validTokens(accessToken = "fresh-access")
            enqueuePage(null, page("mal-api-page-2.json"))
        }
        val store = FakeMalCredentialStore(validTokens(accessToken = "expired", expiresAt = 10))
        val loader = loader(api, store, now = 1_000_000)

        loader.fetchAll()

        assertEquals(1, api.refreshCalls)
        assertEquals("fresh-access", api.pageCalls.single().first)
        assertEquals("fresh-access", store.tokens?.accessToken)
    }

    @Test
    fun `HTTP 401 refreshes once and retries the same page`() = runBlocking {
        val api = FakeMalApiService().apply {
            refreshedTokens = validTokens(accessToken = "renewed-access")
            enqueuePage(null, Result.failure(MalApiException(401, "expired")))
            enqueuePage(null, page("mal-api-page-2.json"))
        }
        val store = FakeMalCredentialStore(validTokens(accessToken = "old-access"))

        val rows = loader(api, store).fetchAll()

        assertEquals(listOf(9253), rows.map { it.malId })
        assertEquals(1, api.refreshCalls)
        assertEquals(listOf("old-access", "renewed-access"), api.pageCalls.map { it.first })
    }

    @Test
    fun `second authorization rejection clears credentials instead of looping`() = runBlocking {
        val api = FakeMalApiService().apply {
            refreshedTokens = validTokens(accessToken = "renewed-access")
            enqueuePage(null, Result.failure(MalApiException(401, "expired")))
            enqueuePage(null, Result.failure(MalApiException(401, "still rejected")))
        }
        val store = FakeMalCredentialStore(validTokens(accessToken = "old-access"))

        val interrupted = runCatching { loader(api, store).fetchAll() }.exceptionOrNull()
            as MalAccountImportInterruptedException

        assertTrue(interrupted.cause is MalAuthorizationRequiredException)
        assertEquals(1, api.refreshCalls)
        assertEquals(2, api.pageCalls.size)
        assertTrue(store.wasCleared)
        assertNull(store.tokens)
    }

    @Test
    fun `partial page failure resumes without requesting completed pages again`() = runBlocking {
        val api = FakeMalApiService().apply {
            enqueuePage(null, page("mal-api-page-1.json"))
            enqueuePage(NextPageUrl, Result.failure(MalApiException(503, "temporary")))
        }
        val store = FakeMalCredentialStore(validTokens())
        val loader = loader(api, store)

        val interrupted = runCatching { loader.fetchAll() }.exceptionOrNull()
            as MalAccountImportInterruptedException
        assertEquals(listOf(5114, 52991), interrupted.continuation.items.map { it.malId })
        assertEquals(NextPageUrl, interrupted.continuation.nextPageUrl)
        assertEquals(1, interrupted.continuation.completedPages)

        api.enqueuePage(NextPageUrl, page("mal-api-page-2.json"))
        val resumed = loader.fetchAll(interrupted.continuation)

        assertEquals(listOf(5114, 52991, 9253), resumed.map { it.malId })
        assertEquals(1, api.pageCalls.count { it.second == null })
        assertEquals(2, api.pageCalls.count { it.second == NextPageUrl })
    }

    @Test
    fun `permanent refresh failure clears credentials and requires reconnection`() = runBlocking {
        val api = FakeMalApiService().apply {
            refreshFailure = MalApiException(401, "refresh revoked")
            enqueuePage(null, page("mal-api-page-2.json"))
        }
        val store = FakeMalCredentialStore(validTokens(expiresAt = 10))

        val interrupted = runCatching {
            loader(api, store, now = 1_000_000).fetchAll()
        }.exceptionOrNull() as MalAccountImportInterruptedException

        assertTrue(interrupted.cause is MalAuthorizationRequiredException)
        assertTrue(store.wasCleared)
        assertNull(store.tokens)
        assertTrue(api.pageCalls.isEmpty())
    }

    private fun loader(
        api: FakeMalApiService,
        store: FakeMalCredentialStore,
        now: Long = 1_000,
    ): MalAccountImportLoader {
        return MalAccountImportLoader(
            apiClient = api,
            authenticatedSession = MalAuthenticatedSession(api, store) { now },
        )
    }

    private fun page(name: String): Result<MalAnimeListPage> =
        Result.success(JSONObject(fixture(name)).toMalAnimeListPage())

    private fun fixture(name: String): String {
        val resource = requireNotNull(javaClass.classLoader?.getResource("imports/$name"))
        return resource.readText(Charsets.UTF_8)
    }

    private fun validTokens(
        accessToken: String = "valid-access",
        expiresAt: Long = 10_000_000,
    ) = MalTokens(
        accessToken = accessToken,
        refreshToken = "refresh-token",
        expiresAtEpochMillis = expiresAt,
        accountName = "fixture-user",
    )

    private class FakeMalCredentialStore(initialTokens: MalTokens?) : MalCredentialStore {
        var tokens: MalTokens? = initialTokens
        var wasCleared: Boolean = false

        override fun readTokens(): MalTokens? = tokens

        override fun saveTokens(tokens: MalTokens) {
            this.tokens = tokens
        }

        override fun clear() {
            wasCleared = true
            tokens = null
        }
    }

    private class FakeMalApiService : MalApiService {
        private val pages = mutableMapOf<String?, ArrayDeque<Result<MalAnimeListPage>>>()
        val pageCalls = mutableListOf<Pair<String, String?>>()
        var refreshCalls: Int = 0
        var refreshedTokens: MalTokens? = null
        var refreshFailure: MalApiException? = null

        fun enqueuePage(url: String?, result: Result<MalAnimeListPage>) {
            pages.getOrPut(url) { ArrayDeque() }.addLast(result)
        }

        override suspend fun getAnimeListPage(
            accessToken: String,
            nextPageUrl: String?,
        ): MalAnimeListPage {
            pageCalls += accessToken to nextPageUrl
            return requireNotNull(pages[nextPageUrl]) { "No fake page for $nextPageUrl" }
                .removeFirst()
                .getOrThrow()
        }

        override suspend fun refreshTokens(tokens: MalTokens): MalTokens {
            refreshCalls++
            refreshFailure?.let { throw it }
            return requireNotNull(refreshedTokens)
        }

        override suspend fun exchangeAuthorizationCode(code: String, verifier: String): MalTokens =
            error("Not used")

        override suspend fun getCurrentAccount(accessToken: String): MalAccount = error("Not used")

        override suspend fun updateAnimeList(
            accessToken: String,
            malId: Int,
            payload: MalSyncPayload,
        ): Int = error("Not used")
    }

    private companion object {
        const val NextPageUrl =
            "https://api.myanimelist.net/v2/users/@me/animelist?offset=2&limit=2"
    }
}
