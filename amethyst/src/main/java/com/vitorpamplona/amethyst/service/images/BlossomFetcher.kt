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
package com.vitorpamplona.amethyst.service.images

import androidx.compose.runtime.Stable
import coil3.ImageLoader
import coil3.Uri
import coil3.annotation.ExperimentalCoilApi
import coil3.disk.DiskCache
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.network.CacheStrategy
import coil3.network.ConnectivityChecker
import coil3.network.DeDupeConcurrentRequestStrategy
import coil3.network.NetworkFetcher
import coil3.network.okhttp.asNetworkClient
import coil3.request.Options
import com.vitorpamplona.amethyst.service.uploads.blossom.bud10.BlossomServerResolver
import com.vitorpamplona.quartz.nip01Core.core.toHexKey
import com.vitorpamplona.quartz.utils.Log
import com.vitorpamplona.quartz.utils.sha256.sha256StreamWithCount
import com.vitorpamplona.quartz.utils.startsWithIgnoreCase
import okhttp3.Call
import kotlin.coroutines.cancellation.CancellationException

@Stable
class BlossomFetcher(
    private val options: Options,
    private val data: Uri,
    private val blossomServerResolver: () -> BlossomServerResolver,
    private val networkFetcher: (url: String) -> Fetcher,
    private val diskCache: () -> DiskCache?,
) : Fetcher {
    override suspend fun fetch(): FetchResult? =
        try {
            val urlResult = blossomServerResolver().findServers(data.toString())
            val fetchUrl = urlResult?.serverUrl ?: data.toString()
            val result = networkFetcher(fetchUrl).fetch()

            // Some CDNs (e.g. nostr.build) return HTTP 200 with a "file not
            // found" placeholder PNG when the blob has expired. Coil happily
            // caches those bytes under the requested key, so every subsequent
            // load — including ones that never reach Morganite — keeps showing
            // the placeholder. Verify the cached bytes against the sha256
            // declared in the `blossom:` URI, and evict on mismatch so the
            // next request retries (and Coil shows an error placeholder for
            // this one instead of the wrong image).
            val expectedSha = urlResult?.uri?.sha256
            if (result != null && expectedSha != null && !diskCacheBytesMatch(fetchUrl, expectedSha)) {
                diskCache()?.remove(fetchUrl)
                return@fetch null
            }

            result
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            null
        }

    private fun diskCacheBytesMatch(
        url: String,
        expectedSha256: String,
    ): Boolean {
        val cache = diskCache() ?: return true
        val snapshot = cache.openSnapshot(url) ?: return true
        return snapshot.use {
            try {
                val (actual, _) = sha256StreamWithCount(it.data.toFile().inputStream())
                val matches = actual.toHexKey().equals(expectedSha256, ignoreCase = true)
                if (!matches) {
                    Log.w(TAG) {
                        "Hash mismatch for $url: expected $expectedSha256, got ${actual.toHexKey()}. Evicting cache entry."
                    }
                }
                matches
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // Can't read the cache file — don't penalise the load.
                true
            }
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    class Factory(
        val blossomServerResolver: () -> BlossomServerResolver,
        val networkClient: (url: String) -> Call.Factory,
    ) : Fetcher.Factory<Uri> {
        private val connectivityCheckerLazy = singleParameterLazy(::ConnectivityChecker)

        override fun create(
            data: Uri,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher? {
            if (!isApplicable(data)) return null
            return BlossomFetcher(
                options = options,
                data = data,
                blossomServerResolver = blossomServerResolver,
                networkFetcher = { url ->
                    NetworkFetcher(
                        url = url,
                        options = options,
                        networkClient = lazy { networkClient(url).asNetworkClient() },
                        diskCache = lazy { imageLoader.diskCache },
                        cacheStrategy = lazy { CacheStrategy.DEFAULT },
                        connectivityChecker = lazy { connectivityCheckerLazy.get(options.context) },
                        concurrentRequestStrategy = lazy { DeDupeConcurrentRequestStrategy() },
                    )
                },
                diskCache = { imageLoader.diskCache },
            )
        }

        private fun isApplicable(data: Uri): Boolean = data.scheme?.startsWithIgnoreCase("blossom", "BLOSSOM") == true
    }

    companion object {
        private const val TAG = "BlossomFetcher"
    }
}
