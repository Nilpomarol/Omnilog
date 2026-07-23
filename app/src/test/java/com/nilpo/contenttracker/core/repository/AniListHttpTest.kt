package com.nilpo.contenttracker.core.repository

import com.sun.net.httpserver.HttpServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetSocketAddress

class AniListHttpTest {
    @Test
    fun `Jikan HTTP failure retains status detail and retry delay`() {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        server.createContext("/limited") { exchange ->
            val body = "{\"status\":429,\"message\":\"Too Many Requests\"}".toByteArray()
            exchange.responseHeaders.add("Retry-After", "7")
            exchange.sendResponseHeaders(429, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()
        try {
            val error = runCatching {
                getJson("http://127.0.0.1:${server.address.port}/limited")
            }.exceptionOrNull() as MetadataProviderHttpException

            assertEquals(429, error.statusCode)
            assertEquals(7_000L, error.retryAfterMillis)
            assertTrue(error.message.orEmpty().contains("Too Many Requests"))
        } finally {
            server.stop(0)
        }
    }
}
