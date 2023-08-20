/*
 * Copyright 2023 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * MIT License
 *
 * Copyright (c) 2017 ligi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package dorkbox.hex

import dorkbox.bytes.sha512
import org.junit.Assert.*
import org.junit.Test

class TestHex {
    @Test
    fun weCanProduceSingleDigitHex() {
        assertEquals("00", Hex.encode(0.toByte()))
        assertEquals("01", Hex.encode(1.toByte()))
        assertEquals("0f", Hex.encode(15.toByte()))
    }

    @Test
    fun weCanProduceDoubleDigitHex() {
        assertEquals("10", Hex.encode(16.toByte()))
        assertEquals("2a", Hex.encode(42.toByte()))
        assertEquals("ff", Hex.encode(255.toByte()))
    }

    @Test
    fun prefixIsIgnored() {
        assertTrue(Hex.decode("0xab").contentEquals(Hex.decode("ab")))
    }

    @Test
    fun testPrimitives() {
        assertEquals("00", 0.toHexString(false))
        assertEquals("0x00", 0.toHexString())
        assertEquals("0x01", 1.toHexString())
        assertEquals("0xa", 10.toHexString())
        assertEquals("0xA", 10.toHexString(upperCase = true))
        assertEquals("0xf", 15.toHexString())
        assertEquals("0x10", 16.toHexString())
        assertEquals("0x11", 17.toHexString())
        assertEquals("0xff", 255.toHexString())
        assertEquals("0x100", 256.toHexString())
        assertEquals("0x4e9", 1257.toHexString())
        assertEquals("0x4E9", 1257.toHexString(upperCase = true))
        assertEquals("4E9", 1257.toHexString(usePrefix = false, upperCase = true))
    }

    @Test
    fun testPrimitivesRoundTrip() {
        assertEquals(1.toByte(), 1.toByte().toHexString().hexToByte())
        assertEquals(127.toByte(), 127.toByte().toHexString().hexToByte())
        assertEquals(255.toUByte(), 255.toUByte().toHexString().hexToUByte())

        assertEquals(1.toShort(), 1.toShort().toHexString().hexToShort())
        assertEquals(Short.MIN_VALUE, Short.MIN_VALUE.toHexString().hexToShort())
        assertEquals(Short.MAX_VALUE, Short.MAX_VALUE.toHexString().hexToShort())

        assertEquals(1.toUShort(), 1.toUShort().toHexString().hexToUShort())
        assertEquals(UShort.MIN_VALUE, UShort.MIN_VALUE.toHexString().hexToUShort())
        assertEquals(UShort.MAX_VALUE, UShort.MAX_VALUE.toHexString().hexToUShort())

        assertEquals(1, 1.toHexString().hexToInt())
        assertEquals(Int.MIN_VALUE, Int.MIN_VALUE.toHexString().hexToInt())
        assertEquals(Int.MAX_VALUE, Int.MAX_VALUE.toHexString().hexToInt())

        assertEquals(1.toUInt(), 1.toUInt().toHexString().hexToUInt())
        assertEquals(UInt.MIN_VALUE, UInt.MIN_VALUE.toHexString().hexToUInt())
        assertEquals(UInt.MAX_VALUE, UInt.MAX_VALUE.toHexString().hexToUInt())

        assertEquals(1L, 1L.toHexString().hexToLong())
        assertEquals(Long.MIN_VALUE, Long.MIN_VALUE.toHexString().hexToLong())
        assertEquals(Long.MAX_VALUE, Long.MAX_VALUE.toHexString().hexToLong())

        assertEquals(1.toULong(), 1.toULong().toHexString().hexToULong())
        assertEquals(ULong.MIN_VALUE, ULong.MIN_VALUE.toHexString().hexToULong())
        assertEquals(ULong.MAX_VALUE, ULong.MAX_VALUE.toHexString().hexToULong())
    }

    @Test
    fun sizesAreOk() {
        assertEquals(0, Hex.decode("0x").size)
        assertEquals(1, Hex.decode("ff").size)
        assertEquals(2, Hex.decode("ffaa").size)
        assertEquals(3, Hex.decode("ffaabb").size)
        assertEquals(4, Hex.decode("ffaabb44").size)
        assertEquals(5, Hex.decode("0xffaabb4455").size)
        assertEquals(6, Hex.decode("0xffaabb445566").size)
        assertEquals(7, Hex.decode("ffaabb44556677").size)
    }

    @Test
    fun byteArrayLimitWorks() {
        assertEquals("0x", Hex.encode(Hex.decode("00"), length = 0))
        assertEquals("0x00", Hex.encode(Hex.decode("00"), length = 1))
        assertEquals("0x", Hex.encode(Hex.decode("ff"), length = 0))
        assertEquals("0xff", Hex.encode(Hex.decode("ff"), length = 1))
        assertEquals("0x", Hex.encode(Hex.decode("abcdef"), length = 0))
        assertEquals("0xab", Hex.encode(Hex.decode("abcdef"), length = 1))
        assertEquals("0xabcd", Hex.encode(Hex.decode("abcdef"), length = 2))
        assertEquals("0xabcdef", Hex.encode(Hex.decode("abcdef"), length = 3))
        assertEquals("0xabcdef", Hex.encode(Hex.decode("abcdef"), length = 32))
        assertEquals("0xaa12456789bb", Hex.encode(Hex.decode("0xaa12456789bb"), length = 6))
        assertEquals("0xaa12456789bb", Hex.encode(Hex.decode("0xaa12456789bb"), length = 9))
    }

    @Test
    fun byteArrayStartWorks() {
        assertEquals("0x", Hex.encode(Hex.decode("abcdef"), start = 1, length = 1))
        assertEquals("0xcd", Hex.encode(Hex.decode("abcdef"), start = 1, length = 2))
        assertEquals("0xcdef", Hex.encode(Hex.decode("abcdef"), start = 1, length = 3))
        assertEquals("0xcdef", Hex.encode(Hex.decode("abcdef"), start = 1, length = 32))
        assertEquals("0x6789bb", Hex.encode(Hex.decode("0xaa12456789bb"), start = 3, length = 6))
        assertEquals("0x456789bb", Hex.encode(Hex.decode("0xaa12456789bb"), start = 2, length = 9))
    }

    @Test
    fun exceptionOnOddInput() {
        var exception: Exception? = null
        try {
            Hex.decode("0xa")
        } catch (e: Exception) {
            exception = e
        }
        assertTrue("Exception must be IllegalArgumentException", exception is IllegalArgumentException)
    }

    @Test
    fun testRoundTrip() {
        assertEquals("0x00", Hex.encode(Hex.decode("00")))
        assertEquals("0xff", Hex.encode(Hex.decode("ff")))
        assertEquals("0xabcdef", Hex.encode(Hex.decode("abcdef")))
        assertEquals("0xaa12456789bb", Hex.encode(Hex.decode("0xaa12456789bb")))
    }

    @Test
    fun regexMatchesForHEX() {
        assertTrue(Hex.HEX_REGEX.matches("0x00"))
        assertTrue(Hex.HEX_REGEX.matches("0xabcdef123456"))
    }

    @Test
    fun regexFailsForNonHEX() {
        assertFalse(Hex.HEX_REGEX.matches("q"))
        assertFalse(Hex.HEX_REGEX.matches(""))
        assertFalse(Hex.HEX_REGEX.matches("0x+"))
        assertFalse(Hex.HEX_REGEX.matches("0xgg"))
    }

    @Test
    fun detectsInvalidHex() {
        var exception: Exception? = null
        try {
            Hex.decode("0xxx")
        } catch (e: Exception) {
            exception = e
        }

        assertTrue("Exception must be IllegalArgumentException", exception is IllegalArgumentException)
    }

    @Test
    fun testStringAsHex() {
        assertEquals("0x123", "123".addHexPrefix())
        assertEquals("0x123", "123".addHexPrefix().addHexPrefix())

        assertEquals("123", "123".removeHexPrefix())
        assertEquals("123", "0x123".removeHexPrefix())
        assertEquals("0x123", "0x0x123".removeHexPrefix())
    }
}
