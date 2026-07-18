package com.nilpo.contenttracker.core.cover

import android.content.Context
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.Disposable
import coil3.request.ImageRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Central entry point for cover loading and storage.
 *
 * This class is intentionally not tied to the media database or UI. A future application-scoped
 * instance can be called by the UI, save flows, and a WorkManager synchronization worker without
 * duplicating cover policy in those layers.
 *
 * Permanent covers are stored in [Context.filesDir], so they survive process death, app closure,
 * reboots, and cache eviction. They are removed only by [removeOrphans], clearing app data, or
 * uninstalling the app. Coil remains responsible for the temporary memory and disk caches.
 */
class CoverRepository(
    private val context: Context,
    private val imageLoader: ImageLoader,
    private val storageDirectory: File = File(context.filesDir, DEFAULT_DIRECTORY_NAME),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val downloader: CoverDownloader = HttpCoverDownloader(),
) {
    private val persistenceLocks = ConcurrentHashMap<String, Mutex>()

    /** Returns a permanent local file when present, otherwise the original URL for Coil. */
    fun displayModel(coverUrl: String?): Any? {
        val normalizedUrl = coverUrl.normalizedCoverUrl() ?: return null
        return storedFile(normalizedUrl).takeIf(File::isUsableCover) ?: normalizedUrl
    }

    /** Returns the permanent local copy, or `null` when this cover has not been persisted. */
    fun localCover(coverUrl: String?): File? {
        val normalizedUrl = coverUrl.normalizedCoverUrl() ?: return null
        return storedFile(normalizedUrl).takeIf(File::isUsableCover)
    }

    /**
     * Warms Coil's temporary disk cache without retaining a full-size decoded image in RAM.
     * Permanent covers are skipped because [displayModel] will load them locally.
     */
    fun prefetch(coverUrls: Iterable<String?>): List<Disposable> {
        return coverUrls
            .mapNotNull(String?::normalizedCoverUrl)
            .distinct()
            .filter { localCover(it) == null }
            .map { coverUrl ->
                imageLoader.enqueue(
                    ImageRequest.Builder(context)
                        .data(coverUrl)
                        .size(PREFETCH_DECODE_SIZE_PX)
                        .memoryCachePolicy(CachePolicy.DISABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .networkCachePolicy(CachePolicy.ENABLED)
                        .build(),
                )
            }
    }

    /** Downloads one cover into permanent app storage. Existing copies are reused. */
    suspend fun persist(coverUrl: String): Result<File> {
        val normalizedUrl = coverUrl.normalizedCoverUrl()
            ?: return Result.failure(IllegalArgumentException("Cover URL must use HTTP or HTTPS"))
        val key = storageKey(normalizedUrl)
        val lock = persistenceLocks.getOrPut(key) { Mutex() }

        return try {
            Result.success(
                withContext(ioDispatcher) {
                    lock.withLock {
                        val destination = storedFileForKey(key)
                        if (destination.isUsableCover()) {
                            return@withLock destination
                        }

                        ensureStorageDirectory()
                        val temporaryFile = File(
                            storageDirectory,
                            "$TEMPORARY_FILE_PREFIX$key-${UUID.randomUUID()}",
                        )
                        try {
                            downloader.download(normalizedUrl, temporaryFile)
                            if (!temporaryFile.isFile || temporaryFile.length() == 0L) {
                                throw IOException("Downloaded cover is empty")
                            }
                            moveReplacing(temporaryFile, destination)
                            destination
                        } finally {
                            temporaryFile.delete()
                        }
                    }
                },
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }

    /** Persists a collection of covers sequentially to avoid flooding remote providers. */
    suspend fun persistAll(coverUrls: Iterable<String?>): CoverSyncResult {
        val urls = coverUrls.mapNotNull(String?::normalizedCoverUrl).distinct()
        var alreadyStored = 0
        var downloaded = 0
        val failures = mutableListOf<CoverFailure>()

        urls.forEach { coverUrl ->
            if (localCover(coverUrl) != null) {
                alreadyStored++
            } else {
                persist(coverUrl).fold(
                    onSuccess = { downloaded++ },
                    onFailure = { failures += CoverFailure(coverUrl, it) },
                )
            }
        }

        return CoverSyncResult(
            requested = urls.size,
            alreadyStored = alreadyStored,
            downloaded = downloaded,
            failures = failures,
        )
    }

    /**
     * Deletes permanent covers that are no longer referenced by any saved media item.
     * Callers must pass the complete current set of saved cover URLs.
     */
    suspend fun removeOrphans(referencedCoverUrls: Iterable<String?>): Int = withContext(ioDispatcher) {
        val referencedKeys = referencedCoverUrls
            .mapNotNull(String?::normalizedCoverUrl)
            .mapTo(mutableSetOf(), ::storageKey)
        val files = storageDirectory.listFiles().orEmpty()
        var removed = 0

        files.forEach { file ->
            when {
                file.name.startsWith(TEMPORARY_FILE_PREFIX) -> {
                    if (System.currentTimeMillis() - file.lastModified() >= STALE_TEMPORARY_FILE_AGE_MILLIS) {
                        file.delete()
                    }
                }
                file.extension == STORED_FILE_EXTENSION && file.nameWithoutExtension !in referencedKeys -> {
                    if (file.delete()) removed++
                }
            }
        }
        removed
    }

    /** Returns the bytes currently occupied by permanent covers, excluding temporary files. */
    suspend fun persistentSizeBytes(): Long = withContext(ioDispatcher) {
        storageDirectory.listFiles()
            .orEmpty()
            .asSequence()
            .filter { it.isFile && it.extension == STORED_FILE_EXTENSION }
            .sumOf(File::length)
    }

    private fun storedFile(normalizedUrl: String): File = storedFileForKey(storageKey(normalizedUrl))

    private fun storedFileForKey(key: String): File {
        return File(storageDirectory, "$key.$STORED_FILE_EXTENSION")
    }

    private fun ensureStorageDirectory() {
        if (!storageDirectory.isDirectory && !storageDirectory.mkdirs()) {
            throw IOException("Could not create permanent cover directory")
        }
    }

    private fun moveReplacing(source: File, destination: File) {
        try {
            Files.move(
                source.toPath(),
                destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                source.toPath(),
                destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
        }
    }
}

data class CoverSyncResult(
    val requested: Int,
    val alreadyStored: Int,
    val downloaded: Int,
    val failures: List<CoverFailure>,
) {
    val succeeded: Int get() = alreadyStored + downloaded
}

data class CoverFailure(
    val coverUrl: String,
    val error: Throwable,
)

/** Download boundary kept injectable so permanent storage can be tested without network access. */
fun interface CoverDownloader {
    @Throws(IOException::class)
    fun download(coverUrl: String, destination: File)
}

private class HttpCoverDownloader : CoverDownloader {
    override fun download(coverUrl: String, destination: File) {
        val connection = URL(coverUrl).openConnection() as? HttpURLConnection
            ?: throw IOException("Could not open cover URL")
        try {
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("Accept", "image/*")
            connection.setRequestProperty("User-Agent", "Omnilog/1.0")

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IOException("Cover URL returned HTTP $responseCode")
            }
            val contentType = connection.contentType?.substringBefore(';')?.trim()
            if (contentType != null && !contentType.startsWith("image/", ignoreCase = true)) {
                throw IOException("Cover URL did not return an image")
            }
            val contentLength = connection.contentLengthLong
            if (contentLength > MAX_COVER_BYTES) {
                throw IOException("Cover is too large")
            }

            connection.inputStream.use { input ->
                destination.outputStream().use { output ->
                    copyWithLimit(input, output, MAX_COVER_BYTES)
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}

private fun String?.normalizedCoverUrl(): String? {
    val value = this?.trim()?.takeIf(String::isNotEmpty) ?: return null
    val url = runCatching { URL(value) }.getOrNull() ?: return null
    return value.takeIf {
        url.protocol.equals("http", ignoreCase = true) ||
            url.protocol.equals("https", ignoreCase = true)
    }
}

private fun storageKey(normalizedUrl: String): String {
    return MessageDigest.getInstance("SHA-256")
        .digest(normalizedUrl.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte -> "%02x".format(byte) }
}

private fun File.isUsableCover(): Boolean = isFile && length() > 0L

private fun copyWithLimit(input: InputStream, output: OutputStream, maximumBytes: Long) {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var totalBytes = 0L
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        if (read == 0) continue
        totalBytes += read
        if (totalBytes > maximumBytes) throw IOException("Cover is too large")
        output.write(buffer, 0, read)
    }
    if (totalBytes == 0L) throw IOException("Cover is empty")
}

private const val DEFAULT_DIRECTORY_NAME = "covers"
private const val STORED_FILE_EXTENSION = "cover"
private const val TEMPORARY_FILE_PREFIX = ".cover-download-"
private const val PREFETCH_DECODE_SIZE_PX = 1
private const val CONNECT_TIMEOUT_MILLIS = 15_000
private const val READ_TIMEOUT_MILLIS = 30_000
private const val MAX_COVER_BYTES = 20L * 1024L * 1024L
private const val STALE_TEMPORARY_FILE_AGE_MILLIS = 24L * 60L * 60L * 1_000L
