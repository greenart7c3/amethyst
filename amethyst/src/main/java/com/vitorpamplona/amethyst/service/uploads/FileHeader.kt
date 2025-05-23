/**
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
package com.vitorpamplona.amethyst.service.uploads

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaDataSource
import android.media.MediaMetadataRetriever
import android.util.Log
import com.vitorpamplona.amethyst.commons.blurhash.toBlurhash
import com.vitorpamplona.amethyst.service.images.BlurhashWrapper
import com.vitorpamplona.quartz.nip01Core.core.toHexKey
import com.vitorpamplona.quartz.nip94FileMetadata.tags.DimensionTag
import com.vitorpamplona.quartz.utils.sha256.sha256
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import java.io.IOException

class FileHeader(
    val mimeType: String?,
    val hash: String,
    val size: Int,
    val dim: DimensionTag?,
    val blurHash: BlurhashWrapper?,
) {
    class UnableToDownload(
        val fileUrl: String,
    ) : Exception()

    companion object {
        suspend fun prepare(
            fileUrl: String,
            mimeType: String?,
            dimPrecomputed: DimensionTag?,
            okHttpClient: (String) -> OkHttpClient,
        ): Result<FileHeader> =
            try {
                val imageData: ImageDownloader.Blob? = ImageDownloader().waitAndGetImage(fileUrl, okHttpClient)

                if (imageData != null) {
                    prepare(imageData.bytes, mimeType ?: imageData.contentType, dimPrecomputed)
                } else {
                    Result.failure(UnableToDownload(fileUrl))
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("ImageDownload", "Couldn't download image from server: ${e.message}")
                Result.failure(e)
            }

        fun prepare(
            data: ByteArray,
            mimeType: String?,
            dimPrecomputed: DimensionTag?,
        ): Result<FileHeader> =
            try {
                val hash = sha256(data).toHexKey()
                val size = data.size

                val (blurHash, dim) =
                    if (mimeType?.startsWith("image/") == true) {
                        val opt = BitmapFactory.Options()
                        opt.inPreferredConfig = Bitmap.Config.ARGB_8888
                        val mBitmap = BitmapFactory.decodeByteArray(data, 0, data.size, opt)
                        Pair(BlurhashWrapper(mBitmap.toBlurhash()), DimensionTag(mBitmap.width, mBitmap.height))
                    } else if (mimeType?.startsWith("video/") == true) {
                        val mediaMetadataRetriever = MediaMetadataRetriever()
                        mediaMetadataRetriever.setDataSource(ByteArrayMediaDataSource(data))

                        val newDim = mediaMetadataRetriever.prepareDimFromVideo() ?: dimPrecomputed
                        val blurhash = mediaMetadataRetriever.getThumbnail()?.toBlurhash()?.let { BlurhashWrapper(it) }

                        if (newDim?.hasSize() == true) {
                            Pair(blurhash, newDim)
                        } else {
                            Pair(blurhash, null)
                        }
                    } else {
                        Pair(null, null)
                    }

                Result.success(FileHeader(mimeType, hash, size, dim, blurHash))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("ImageDownload", "Couldn't convert image in to File Header: ${e.message}")
                Result.failure(e)
            }
    }
}

class ByteArrayMediaDataSource(
    var imageData: ByteArray,
) : MediaDataSource() {
    override fun getSize(): Long = imageData.size.toLong()

    @Throws(IOException::class)
    override fun readAt(
        position: Long,
        buffer: ByteArray,
        offset: Int,
        size: Int,
    ): Int {
        if (position >= imageData.size) {
            return -1
        }
        val newSize =
            if (position + size > imageData.size) {
                size - ((position.toInt() + size) - imageData.size)
            } else {
                size
            }

        imageData.copyInto(buffer, offset, position.toInt(), position.toInt() + newSize)

        return newSize
    }

    @Throws(IOException::class)
    override fun close() {}
}
