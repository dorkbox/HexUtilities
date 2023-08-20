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
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package dorkbox.hex

internal object HexUtil {
    private var NEWLINE = System.getProperty("line.separator", "\n")
    private var EMPTY_STRING = ""

    private val BYTE2CHAR = CharArray(256)
    private val HEXDUMP_TABLE = CharArray(256 * 4)
    private val HEXPADDING = arrayOfNulls<String>(16)
    private val HEXDUMP_ROWPREFIXES = arrayOfNulls<String>(65536 ushr 4)
    private val BYTE2HEX = arrayOfNulls<String>(256)
    private val BYTEPADDING = arrayOfNulls<String>(16)
    private val BYTE2HEX_PAD = arrayOfNulls<String>(256)

    init {
        // Generate the lookup table that converts a byte into a 2-digit hexadecimal integer.
        for (i in BYTE2HEX_PAD.indices) {
            val str = Integer.toHexString(i)
            BYTE2HEX_PAD[i] = if (i > 0xf) str else "0$str"
        }

        val DIGITS = "0123456789abcdef".toCharArray()
        for (i in 0..255) {
            HEXDUMP_TABLE[i shl 1] = DIGITS[i ushr 4 and 0x0F]
            HEXDUMP_TABLE[(i shl 1) + 1] = DIGITS[i and 0x0F]
        }
        var i: Int

        // Generate the lookup table for hex dump paddings
        i = 0
        while (i < HEXPADDING.size) {
            val padding = HEXPADDING.size - i
            val buf = StringBuilder(padding * 3)
            for (j in 0 until padding) {
                buf.append("   ")
            }
            HEXPADDING[i] = buf.toString()
            i++
        }

        // Generate the lookup table for the start-offset header in each row (up to 64KiB).
        i = 0
        while (i < HEXDUMP_ROWPREFIXES.size) {
            val buf = StringBuilder(12)
            buf.append(NEWLINE)
            buf.append(java.lang.Long.toHexString((i shl 4).toLong() and 0xFFFFFFFFL or 0x100000000L))
            buf.setCharAt(buf.length - 9, '|')
            buf.append('|')
            HEXDUMP_ROWPREFIXES[i] = buf.toString()
            i++
        }

        // Generate the lookup table for byte-to-hex-dump conversion
        i = 0
        while (i < BYTE2HEX.size) {
            BYTE2HEX[i] = ' '.toString() + BYTE2HEX_PAD[i and 0xff]
            i++
        }



        // Generate the lookup table for byte dump paddings
        i = 0
        while (i < BYTEPADDING.size) {
            val padding = BYTEPADDING.size - i
            val buf = StringBuilder(padding)
            for (j in 0 until padding) {
                buf.append(' ')
            }
            BYTEPADDING[i] = buf.toString()
            i++
        }

        // Generate the lookup table for byte-to-char conversion
        i = 0
        while (i < BYTE2CHAR.size) {
            if (i <= 0x1f || i >= 0x7f) {
                BYTE2CHAR[i] = '.'
            }
            else {
                BYTE2CHAR[i] = i.toChar()
            }
            i++
        }
    }

    fun hexDump(array: ByteArray, fromIndex: Int, length: Int): String {
        // checkPositiveOrZero(length, "length");
        if (length == 0) {
            return ""
        }
        val endIndex = fromIndex + length
        val buf = CharArray(length shl 1)
        var srcIdx = fromIndex
        var dstIdx = 0
        while (srcIdx < endIndex) {
            System.arraycopy(
                HEXDUMP_TABLE, array[srcIdx].toInt() and 0xFF shl 1, buf, dstIdx, 2
            )
            srcIdx++
            dstIdx += 2
        }
        return String(buf)
    }

    fun prettyHexDump(array: ByteArray, fromIndex: Int, length: Int): String {
        return if (length == 0) {
            EMPTY_STRING
        }
        else {
            val rows = length / 16 + (if (length and 15 == 0) 0 else 1) + 4
            val buf = StringBuilder(rows * 80)
            appendPrettyHexDump(buf, array, fromIndex, length)
            buf.toString()
        }
    }

    private fun appendPrettyHexDump(dump: StringBuilder, array: ByteArray, fromIndex: Int, length: Int) {
        require(fromIndex in 0..length) { "expected: 0 <= fromIndex($fromIndex) <= length($length)" }

        if (length == 0) {
            return
        }
        dump.append(
                  "         +-------------------------------------------------+" + NEWLINE +
                  "         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |" + NEWLINE +
                  "+--------+-------------------------------------------------+----------------+"
        )

        val fullRows = length ushr 4
        val remainder = length and 0xF

        // Dump the rows which have 16 bytes.
        for (row in 0 until fullRows) {
            val rowStartIndex = (row shl 4) + fromIndex

            // Per-row prefix.
            appendHexDumpRowPrefix(dump, row, rowStartIndex)

            // Hex dump
            val rowEndIndex = rowStartIndex + 16
            for (j in rowStartIndex until rowEndIndex) {
                dump.append(BYTE2HEX[(array[j].toInt() and 0xFF).toShort().toInt()])
            }
            dump.append(" |")

            // ASCII dump
            for (j in rowStartIndex until rowEndIndex) {
                dump.append(BYTE2CHAR[(array[j].toInt() and 0xFF).toShort().toInt()])
            }
            dump.append('|')
        }

        // Dump the last row which has less than 16 bytes.
        if (remainder != 0) {
            val rowStartIndex = (fullRows shl 4) + fromIndex
            appendHexDumpRowPrefix(dump, fullRows, rowStartIndex)

            // Hex dump
            val rowEndIndex = rowStartIndex + remainder
            for (j in rowStartIndex until rowEndIndex) {
                dump.append(BYTE2HEX[(array[j].toInt() and 0xFF).toShort().toInt()])
            }
            dump.append(HEXPADDING[remainder])
            dump.append(" |")

            // Ascii dump
            for (j in rowStartIndex until rowEndIndex) {
                dump.append(BYTE2CHAR[(array[j].toInt() and 0xFF).toShort().toInt()])
            }
            dump.append(BYTEPADDING[remainder])
            dump.append('|')
        }
        dump.append(
            NEWLINE + "+--------+-------------------------------------------------+----------------+"
        )
    }

    private fun appendHexDumpRowPrefix(dump: StringBuilder, row: Int, rowStartIndex: Int) {
        if (row < HEXDUMP_ROWPREFIXES.size) {
            dump.append(HEXDUMP_ROWPREFIXES[row])
        }
        else {
            dump.append(NEWLINE)
            dump.append(java.lang.Long.toHexString(rowStartIndex.toLong() and 0xFFFFFFFFL or 0x100000000L))
            dump.setCharAt(dump.length - 9, '|')
            dump.append('|')
        }
    }
}
