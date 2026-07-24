package com.nilpo.contenttracker.core.mal

import com.nilpo.contenttracker.core.model.MyAnimeListImportItem
import kotlinx.coroutines.CancellationException

internal class MalAuthorizationRequiredException(
    val statusCode: Int? = null,
    message: String? = null,
) : Exception(message)

/** Executes authenticated MAL requests with one token refresh and one retry after HTTP 401. */
internal class MalAuthenticatedSession(
    private val apiClient: MalApiService,
    private val credentialStore: MalCredentialStore,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
) {
    suspend fun <T> execute(request: suspend (accessToken: String) -> T): T {
        var tokens = credentialStore.readTokens()
            ?: throw MalAuthorizationRequiredException(message = "Connecta el compte de MAL primer.")
        if (tokens.expiresAtEpochMillis <= nowEpochMillis() + RefreshLeewayMillis) {
            tokens = refreshOrDisconnect(tokens)
        }

        return try {
            request(tokens.accessToken)
        } catch (error: MalApiException) {
            if (error.statusCode != 401) throw error
            tokens = refreshOrDisconnect(tokens)
            try {
                request(tokens.accessToken)
            } catch (retryError: MalApiException) {
                if (retryError.statusCode in PermanentAuthorizationFailureCodes) {
                    credentialStore.clear()
                    throw MalAuthorizationRequiredException(retryError.statusCode, retryError.message)
                }
                throw retryError
            }
        }
    }

    private suspend fun refreshOrDisconnect(tokens: MalTokens): MalTokens = try {
        apiClient.refreshTokens(tokens)
            .copy(accountName = tokens.accountName)
            .also(credentialStore::saveTokens)
    } catch (error: MalApiException) {
        if (error.statusCode in PermanentAuthorizationFailureCodes) {
            credentialStore.clear()
            throw MalAuthorizationRequiredException(error.statusCode, error.message)
        }
        throw error
    }
}

internal data class MalAccountImportContinuation(
    val items: List<MyAnimeListImportItem> = emptyList(),
    val nextPageUrl: String? = null,
    val completedPages: Int = 0,
    val totalRows: Int = items.size,
)

data class MalAccountImportLoadResult(
    val items: List<MyAnimeListImportItem>,
    val totalRows: Int,
) {
    val invalidRows: Int
        get() = (totalRows - items.size).coerceAtLeast(0)
}

internal class MalAccountImportInterruptedException(
    val continuation: MalAccountImportContinuation,
    cause: Throwable,
) : Exception(cause.message, cause)

/** Fetches every MAL list page and retains a safe in-memory continuation after partial failure. */
internal class MalAccountImportLoader(
    private val apiClient: MalApiService,
    private val authenticatedSession: MalAuthenticatedSession,
) {
    suspend fun fetchAll(
        continuation: MalAccountImportContinuation = MalAccountImportContinuation(),
        onPageLoaded: suspend (
            page: MalAnimeListPage,
            continuation: MalAccountImportContinuation,
        ) -> Unit = { _, _ -> },
        onProgress: (itemCount: Int, completedPages: Int) -> Unit = { _, _ -> },
    ): MalAccountImportLoadResult {
        val items = continuation.items.toMutableList()
        var nextPageUrl = continuation.nextPageUrl
        var completedPages = continuation.completedPages
        var totalRows = continuation.totalRows
        val requestedPages = mutableSetOf<String>()

        while (true) {
            val pageKey = nextPageUrl ?: InitialPageKey
            check(requestedPages.add(pageKey)) { "MyAnimeList pagination repeated the same page" }
            val page = try {
                authenticatedSession.execute { accessToken ->
                    apiClient.getAnimeListPage(accessToken, nextPageUrl)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                throw MalAccountImportInterruptedException(
                    continuation = MalAccountImportContinuation(
                        items = items.toList(),
                        nextPageUrl = nextPageUrl,
                        completedPages = completedPages,
                        totalRows = totalRows,
                    ),
                    cause = error,
                )
            }

            items += page.items
            totalRows += page.totalRows
            completedPages++
            nextPageUrl = page.nextPageUrl
            val checkpoint = MalAccountImportContinuation(
                items = items.toList(),
                nextPageUrl = nextPageUrl,
                completedPages = completedPages,
                totalRows = totalRows,
            )
            onPageLoaded(page, checkpoint)
            onProgress(items.size, completedPages)
            if (nextPageUrl == null) {
                return MalAccountImportLoadResult(items = items, totalRows = totalRows)
            }
        }
    }
}

private const val InitialPageKey = "<initial>"
private const val RefreshLeewayMillis = 60_000L
private val PermanentAuthorizationFailureCodes = setOf(400, 401, 403)
