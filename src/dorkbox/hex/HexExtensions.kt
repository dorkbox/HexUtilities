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

package dorkbox.hex

import dorkbox.bytes.BigEndian
import dorkbox.hex.Hex.tweakHex
import java.math.BigInteger

object Hex {
    /**
     * Gets the version number.
     */
    const val version = "1.0"

    init {
        // Add this project to the updates system, which verifies this class + UUID + version information
        dorkbox.updates.Updates.add(Hex::class.java, "e52265b05ab54361a011ca8d406b9db9", version)
    }

    /**
     * Represents all the chars used for nibble
     */
    private val HEX_CHARS = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')
    private val UPPER_HEX_CHARS = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')


    internal val HEX_REGEX = Regex("(0[xX])?[0-9a-fA-F]+")
    private val prefix = "0x".toCharArray()

    /**
     * Encodes the given byte value as a hexadecimal character.
     */
    fun encode(value: Byte): String {
        val hexString = CharArray(2)
        val toInt = value.toInt()

        hexString[0] = HEX_CHARS[toInt and 0xF0 shr 4]
        hexString[1] = HEX_CHARS[toInt and 0x0F]

        return String(hexString)
    }

    /**
     * Encodes the given byte value as a hexadecimal character.
     */
    fun encodeUpper(value: Byte): String {
        val hexString = CharArray(2)
        val toInt = value.toInt()
        hexString[0] = UPPER_HEX_CHARS[toInt and 0xF0 shr 4]
        hexString[1] = UPPER_HEX_CHARS[toInt and 0x0F]

        return String(hexString)
    }

    /**
     * Encodes the given byte array value to its hexadecimal representations, and optionally prepends the prefix `0x` to it.
     */
    fun encode(bytes: ByteArray, usePrefix: Boolean = true, start: Int = 0, length: Int = bytes.size, toUpperCase: Boolean = false): String {
        require(start >= 0) { "Start ($start) must be >= 0" }
        require(length >= 0) { "Limit ($length) must be >= 0" }
        require(bytes.isEmpty() || start < bytes.size) { "Start ($start) position must be smaller than the size of the byte array" }

        @Suppress("NAME_SHADOWING")
        val length = if (length < bytes.size) {
            length
        } else {
            bytes.size
        }

        var j: Int
        val hexString = if (usePrefix) {
            j = 2
            val array = CharArray(j + ((2 * (length - start) )))
            prefix.copyInto(array)
            array
        } else {
            j = 0
            CharArray(((2 * (length - start) )))
        }


        if (toUpperCase) {
            for (i in start until length) {
                val toInt = bytes[i].toInt()
                hexString[j++] = UPPER_HEX_CHARS[toInt and 0xF0 shr 4]
                hexString[j++] = UPPER_HEX_CHARS[toInt and 0x0F]
            }
        } else {
            for (i in start until length) {
                val toInt = bytes[i].toInt()
                hexString[j++] = HEX_CHARS[toInt and 0xF0 shr 4]
                hexString[j++] = HEX_CHARS[toInt and 0x0F]
            }
        }

        return String(hexString)
    }

    /**
     * Converts the given ch into its integer representation considering it as a hexadecimal character.
     */
    private fun hexToDecimal(char: Char): Int = when (char) {
        in '0'..'9' -> char - '0'
        in 'A'..'F' -> char - 'A' + 10
        in 'a'..'f' -> char - 'a' + 10
        else -> throw(IllegalArgumentException("'$char' is not a valid hexadecimal character"))
    }

    /**
     * Parses the given value reading it as an hexadecimal string, and returns a hex bytearray
     *
     * Note that either `0x`, `0X`, and no-prefixed strings are supported.
     *
     * @throws IllegalArgumentException if the value is not a hexadecimal string.
     */
    fun decode(value: CharSequence): ByteArray {
        // Remove the 0x or 0X prefix if it is set
        val cleanInput = value.removeHexPrefix()

        if (cleanInput.isEmpty()) {
            return ByteArray(0)
        }

        // A hex string must always have length multiple of 2
        if (cleanInput.length % 2 != 0) {
            throw IllegalArgumentException("hex-string '$value' must have an even number of digits")
        }

        return ByteArray(cleanInput.length / 2).apply {
            var i = 0
            while (i < cleanInput.length) {
                this[i / 2] = ((hexToDecimal(cleanInput[i]) shl 4) + hexToDecimal(cleanInput[i + 1])).toByte()
                i += 2
            }
        }
    }

    internal fun String.tweakHex(value: Int, usePrefix: Boolean = true, toUpperCase: Boolean = false): String {
        val prefix = if (usePrefix) {
            if (-10 < value && value < 10) {
                "0x0"
            } else {
                "0x"
            }
        } else {
            if (-10 < value && value < 10) {
                "0"
            } else {
                ""
            }
        }

        return if (toUpperCase) {
            prefix + this.uppercase()
        } else {
            "$prefix$this"
        }
    }

    internal fun String.tweakHex(value: Long, usePrefix: Boolean = true, toUpperCase: Boolean = false): String {
        val prefix = if (usePrefix) {
            if (-10 < value && value < 10) {
                "0x0"
            } else {
                "0x"
            }
        } else {
            if (-10 < value && value < 10) {
                "0"

            } else {
                ""
            }
        }

        return if (toUpperCase) {
            prefix + this.uppercase()
        } else {
            "$prefix$this"
        }
    }

    private val bigInt10 = BigInteger.valueOf(10)
    private val bigIntN10 = BigInteger.valueOf(-10)

    internal fun BigInteger.tweakHex(usePrefix: Boolean = true, toUpperCase: Boolean = false): String {
        val asHex = this.toString(16)

        val prefix = if (usePrefix) {
            if (bigIntN10 < this && this < bigInt10) {
                "0x0"
            } else {
                "0x"
            }
        } else {
            if (bigIntN10 > this && this < bigInt10) {
                "0"

            } else {
                ""
            }
        }

        return if (toUpperCase) {
            prefix + asHex.uppercase()
        } else {
            "$prefix$asHex"
        }
    }
}

/**
 * Converts a [ByteArray] into its hexadecimal string representation, optionally with the `0x` prefix.
 */
fun ByteArray.toHexString(usePrefix: Boolean = true, start: Int = 0, length: Int = this.size, toUpperCase: Boolean = false): String =
    Hex.encode(this, usePrefix, start, length, toUpperCase)

/**
 * Converts a [Collection] into its hexadecimal string representation, optionally with the `0x` prefix.
 */
fun Collection<Byte>.toHexString(usePrefix: Boolean = true, length: Int = this.size): String = Hex.encode(this.toByteArray(), usePrefix, length)


/**
 * Parses the [String] as an hexadecimal value and returns its [ByteArray] representation.
 *
 * Note that both `0x`, `0X`, and no-prefix hex strings are supported.
 *
 * @throws IllegalArgumentException if [this] is not a hexadecimal string.
 */
fun CharSequence.hexToByteArray(): ByteArray = Hex.decode(this)



/**
 * Parses the [String] as an hexadecimal value and returns its [Byte] representation.
 *
 * Note that both `0x`, `0X`, and no-prefix hex strings are supported.
 *
 * @throws IllegalArgumentException if [this] is not a hexadecimal string.
 */
fun CharSequence.hexToByte(): Byte = Hex.decode(this).first()

/**
 * Parses the [String] as an hexadecimal value and returns its [UByte] representation.
 *
 * Note that both `0x`, `0X`, and no-prefix hex strings are supported.
 *
 * @throws IllegalArgumentException if [this] is not a hexadecimal string.
 */
fun CharSequence.hexToUByte(): UByte = Hex.decode(this).first().toUByte()

/**
 * Parses the [String] as an hexadecimal value and returns its [Short] representation.
 *
 * Note that both `0x`, `0X`, and no-prefix hex strings are supported.
 *
 * @throws IllegalArgumentException if [this] is not a hexadecimal string.
 */
fun CharSequence.hexToShort(): Short = BigEndian.Int_.from(Hex.decode(this)).toShort()

/**
 * Parses the [String] as an hexadecimal value and returns its [UShort] representation.
 *
 * Note that both `0x`, `0X`, and no-prefix hex strings are supported.
 *
 * @throws IllegalArgumentException if [this] is not a hexadecimal string.
 */
fun CharSequence.hexToUShort(): UShort = BigEndian.UShort_.from(Hex.decode(this))

/**
 * Parses the [String] as an hexadecimal value and returns its [Int] representation.
 *
 * Note that both `0x`, `0X`, and no-prefix hex strings are supported.
 *
 * @throws IllegalArgumentException if [this] is not a hexadecimal string.
 */
fun CharSequence.hexToInt(): Int = BigEndian.Int_.from(Hex.decode(this))

/**
 * Parses the [String] as an hexadecimal value and returns its [UShort] representation.
 *
 * Note that both `0x`, `0X`, and no-prefix hex strings are supported.
 *
 * @throws IllegalArgumentException if [this] is not a hexadecimal string.
 */
fun CharSequence.hexToUInt(): UInt = BigEndian.UInt_.from(Hex.decode(this))

/**
 * Parses the [String] as an hexadecimal value and returns its [Long] representation.
 *
 * Note that both `0x`, `0X`, and no-prefix hex strings are supported.
 *
 * @throws IllegalArgumentException if [this] is not a hexadecimal string.
 */
fun CharSequence.hexToLong(): Long = BigEndian.Long_.from(Hex.decode(this))

/**
 * Parses the [String] as an hexadecimal value and returns its [ULong] representation.
 *
 * Note that both `0x`, `0X`, and no-prefix hex strings are supported.
 *
 * @throws IllegalArgumentException if [this] is not a hexadecimal string.
 */
fun CharSequence.hexToULong(): ULong = BigEndian.ULong_.from(Hex.decode(this))





/**
 * Returns `true` if and only if [this] value starts with the `0x` or `0X` prefix.
 */
fun CharSequence.hasPrefix(): Boolean = this.startsWith("0x") || this.startsWith("0X")

/**
 * Returns a new [String] obtained by prepends the `0x` prefix to the string, but only if it not already present.
 *
 * Examples:
 * ```kotlin
 * assertEquals("0x123", "123".addHexPrefix())
 * assertEquals("0x123", "123".addHexPrefix().addHexPrefix())
 * ```
 */
fun String.addHexPrefix(): String = if (hasPrefix()) this else "0x$this"
fun CharSequence.addHexPrefix(): CharSequence = if (hasPrefix()) this else "0x$this"

/**
 * Returns a new [String] obtained by removing the first occurrence of the `0x` prefix from the string, if it has it.
 *
 * Examples:
 * ```kotlin
 * assertEquals("123", "123".removeHexPrefix())
 * assertEquals("123", "0x123".removeHexPrefix())
 * assertEquals("0x123", "0x0x123".removeHexPrefix())
 * ```
 */
fun String.removeHexPrefix(): String = if (hasPrefix()) this.substring(2) else this
fun CharSequence.removeHexPrefix(): CharSequence = if (hasPrefix()) this.substring(2) else this

/**
 * Returns if a given string is a valid hex-string - either with or without 0x prefix
 */
fun CharSequence.isValidHex(): Boolean = Hex.HEX_REGEX.matches(this)

fun Byte.toHexString(usePrefix: Boolean = true, toUpperCase: Boolean = false): String {
    return if (toUpperCase) {
        if (usePrefix) {
            "0x" + Hex.encodeUpper(this)
        } else {
            Hex.encodeUpper(this)
        }

    } else {
        if (usePrefix) {
            "0x" + Hex.encode(this)
        } else {
            Hex.encode(this)
        }
    }
}

fun UByte.toHexString(usePrefix: Boolean = true, toUpperCase: Boolean = false): String = Integer.toHexString(this.toInt()).tweakHex(this.toInt(), usePrefix, toUpperCase)
fun Short.toHexString(usePrefix: Boolean = true, toUpperCase: Boolean = false): String = Integer.toHexString(this.toInt()).tweakHex(this.toInt(), usePrefix, toUpperCase)
fun UShort.toHexString(usePrefix: Boolean = true, toUpperCase: Boolean = false): String = Integer.toHexString(this.toInt()).tweakHex(this.toInt(), usePrefix, toUpperCase)
fun Int.toHexString(usePrefix: Boolean = true, upperCase: Boolean = false): String = Integer.toHexString(this).tweakHex(this, usePrefix, upperCase)
fun UInt.toHexString(usePrefix: Boolean = true, toUpperCase: Boolean = false): String = java.lang.Long.toHexString(this.toLong()).tweakHex(this.toLong(), usePrefix, toUpperCase)
fun Long.toHexString(usePrefix: Boolean = true, toUpperCase: Boolean = false): String = java.lang.Long.toHexString(this).tweakHex(this, usePrefix, toUpperCase)
fun ULong.toHexString(usePrefix: Boolean = true, toUpperCase: Boolean = false): String = BigInteger(this.toString()).tweakHex(usePrefix, toUpperCase)


fun ByteArray.hexDump(fromIndex: Int = 0, length: Int = this.size): String = HexUtil.hexDump(this, fromIndex, length)
fun ByteArray.prettyHexDump(fromIndex: Int = 0, length: Int = this.size): String = HexUtil.prettyHexDump(this, fromIndex, length)
