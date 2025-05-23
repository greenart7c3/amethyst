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
package com.vitorpamplona.amethyst.commons.blurhash

import kotlin.math.pow

class SRGB {
    companion object {
        fun linearToSrgb(value: Float): Int {
            val v = value.coerceIn(0.0f, 1.0f)
            return if (v <= 0.0031308f) {
                (v * 12.92f * 255f + 0.5f).toInt()
            } else {
                ((1.055f * v.pow(1 / 2.4f) - 0.055f) * 255 + 0.5f).toInt()
            }
        }

        fun linearToSrgb(value: Double): Int {
            val v = value.coerceIn(0.0, 1.0)
            return if (v <= 0.0031308f) {
                (v * 12.92f * 255f + 0.5f).toInt()
            } else {
                ((1.055f * v.pow(1 / 2.4) - 0.055f) * 255 + 0.5f).toInt()
            }
        }

        fun srgbToLinear(value: Int): Float {
            val valueCheck = value.coerceIn(0, 255)

            val v = valueCheck / 255f
            return if (v <= 0.04045f) {
                v / 12.92f
            } else {
                ((v + 0.055f) / 1.055f).pow(2.4f)
            }
        }
    }
}
