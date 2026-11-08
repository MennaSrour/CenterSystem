package center.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

fun createHttpClient(): HttpClient =
    HttpClient(CIO) {
        expectSuccess = true
    }
