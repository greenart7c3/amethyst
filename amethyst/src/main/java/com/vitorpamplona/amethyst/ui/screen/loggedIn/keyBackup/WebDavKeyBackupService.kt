/*
 * Copyright (c) 2025 Vitor Pamplona
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.vitorpamplona.amethyst.ui.screen.loggedIn.keyBackup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URL
import java.util.concurrent.TimeUnit

object WebDavKeyBackupService {
    private val client =
        OkHttpClient
            .Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    private fun buildUrl(
        serverUrl: String,
        filename: String,
    ): String {
        val base = serverUrl.trimEnd('/')
        return "$base/$filename"
    }

    suspend fun save(
        serverUrl: String,
        username: String,
        password: String,
        filename: String,
        encryptedKey: String,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                URL(buildUrl(serverUrl, filename)) // validate URL
                val credential = Credentials.basic(username, password)
                val body = encryptedKey.toRequestBody("text/plain; charset=utf-8".toMediaType())
                val request =
                    Request
                        .Builder()
                        .url(buildUrl(serverUrl, filename))
                        .header("Authorization", credential)
                        .put(body)
                        .build()
                val response = client.newCall(request).execute()
                response.use {
                    if (!it.isSuccessful) {
                        throw IOException("Server returned ${it.code}: ${it.message}")
                    }
                }
            }
        }

    suspend fun load(
        serverUrl: String,
        username: String,
        password: String,
        filename: String,
    ): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                URL(buildUrl(serverUrl, filename)) // validate URL
                val credential = Credentials.basic(username, password)
                val request =
                    Request
                        .Builder()
                        .url(buildUrl(serverUrl, filename))
                        .header("Authorization", credential)
                        .get()
                        .build()
                val response = client.newCall(request).execute()
                response.use {
                    if (!it.isSuccessful) {
                        throw IOException("Server returned ${it.code}: ${it.message}")
                    }
                    it.body?.string() ?: throw IOException("Empty response body")
                }
            }
        }
}
