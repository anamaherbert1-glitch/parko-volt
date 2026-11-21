package com.example.util

/**
 * Pure Kotlin ISO/IEC 18004 QR Code encoder (Version 2 & 3, Error Correction Level M, Byte mode).
 * Generates genuine, scannable QR code boolean matrices without external binary dependencies.
 */
object QrCodeGenerator {

    fun encode(text: String): Array<BooleanArray> {
        val bytes = text.toByteArray(Charsets.UTF_8)
        // Version 2-M holds up to 26 bytes; Version 3-M holds up to 42 bytes
        val version = if (bytes.size <= 26) 2 else 3
        val size = 17 + version * 4
        val dataCodewordsCount = if (version == 2) 28 else 44
        val eccCount = if (version == 2) 16 else 26

        val bits = MutableBitList()
        // Mode: Byte (0100)
        bits.append(0b0100, 4)
        // Char count (8 bits for versions 1..9)
        bits.append(bytes.size.coerceAtMost(42), 8)
        for (i in 0 until bytes.size.coerceAtMost(42)) {
            bits.append(bytes[i].toInt() and 0xFF, 8)
        }
        // Terminator
        val maxDataBits = dataCodewordsCount * 8
        val termLen = (maxDataBits - bits.size).coerceAtMost(4)
        bits.append(0, termLen)
        while (bits.size % 8 != 0) bits.append(0, 1)
        var padByte = 0xEC
        while (bits.size < maxDataBits) {
            bits.append(padByte, 8)
            padByte = if (padByte == 0xEC) 0x11 else 0xEC
        }

        val dataBytes = IntArray(dataCodewordsCount) { idx ->
            var v = 0
            for (b in 0 until 8) {
                v = (v shl 1) or bits.get(idx * 8 + b)
            }
            v
        }
        val eccBytes = computeReedSolomon(dataBytes, eccCount)
        val allCodewords = dataBytes + eccBytes

        val modules = Array(size) { BooleanArray(size) }
        val isFunction = Array(size) { BooleanArray(size) }

        drawFunctionPatterns(version, size, modules, isFunction)
        drawCodewords(size, allCodewords, modules, isFunction)

        // Apply Mask 2 ((col % 3) == 0) for crisp clean contrast and write Format Info (ECC M = 00, Mask 2 = 010)
        val mask = 2
        applyMask(size, mask, modules, isFunction)
        drawFormatBits(size, 0, mask, modules, isFunction) // ECC M -> 0 in QR spec table (L=1, M=0, Q=3, H=2)

        return modules
    }

    private fun drawFunctionPatterns(
        version: Int,
        size: Int,
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>
    ) {
        // Timing patterns
        for (i in 0 until size) {
            setFunc(6, i, i % 2 == 0, modules, isFunction)
            setFunc(i, 6, i % 2 == 0, modules, isFunction)
        }
        // Finder patterns
        drawFinder(0, 0, size, modules, isFunction)
        drawFinder(size - 7, 0, size, modules, isFunction)
        drawFinder(0, size - 7, size, modules, isFunction)
        // Alignment pattern (version 2 -> 18, version 3 -> 22)
        val alignPos = if (version == 2) 18 else 22
        drawAlignment(alignPos, alignPos, modules, isFunction)
        // Reserve format info areas
        for (i in 0..8) {
            if (i != 6) {
                setFunc(8, i, false, modules, isFunction)
                setFunc(i, 8, false, modules, isFunction)
            }
        }
        for (i in size - 8 until size) {
            setFunc(8, i, false, modules, isFunction)
            setFunc(i, 8, false, modules, isFunction)
        }
        // Dark module
        setFunc(8, size - 8, true, modules, isFunction)
    }

    private fun drawFinder(
        x: Int,
        y: Int,
        size: Int,
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>
    ) {
        for (dy in -1..7) {
            for (dx in -1..7) {
                val xx = x + dx
                val yy = y + dy
                if (xx in 0 until size && yy in 0 until size) {
                    val dist = maxOf(kotlin.math.abs(dx - 3), kotlin.math.abs(dy - 3))
                    val dark = dist == 3 || dist <= 1
                    setFunc(xx, yy, dx in 0..6 && dy in 0..6 && dark, modules, isFunction)
                }
            }
        }
    }

    private fun drawAlignment(
        cx: Int,
        cy: Int,
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>
    ) {
        for (dy in -2..2) {
            for (dx in -2..2) {
                val dist = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
                setFunc(cx + dx, cy + dy, dist != 1, modules, isFunction)
            }
        }
    }

    private fun setFunc(
        x: Int,
        y: Int,
        dark: Boolean,
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>
    ) {
        modules[y][x] = dark
        isFunction[y][x] = true
    }

    private fun drawCodewords(
        size: Int,
        data: IntArray,
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>
    ) {
        var i = 0
        val totalBits = data.size * 8
        var right = size - 1
        while (right >= 1) {
            if (right == 6) right = 5
            for (vert in 0 until size) {
                val upward = ((right + 1) and 2) == 0
                val y = if (upward) size - 1 - vert else vert
                for (j in 0..1) {
                    val x = right - j
                    if (!isFunction[y][x] && i < totalBits) {
                        val bit = ((data[i ushr 3] ushr (7 - (i and 7))) and 1) != 0
                        modules[y][x] = bit
                        i++
                    }
                }
            }
            right -= 2
        }
    }

    private fun applyMask(
        size: Int,
        mask: Int,
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>
    ) {
        for (y in 0 until size) {
            for (x in 0 until size) {
                if (!isFunction[y][x]) {
                    val invert = when (mask) {
                        0 -> (x + y) % 2 == 0
                        1 -> y % 2 == 0
                        2 -> x % 3 == 0
                        else -> false
                    }
                    if (invert) modules[y][x] = !modules[y][x]
                }
            }
        }
    }

    private fun drawFormatBits(
        size: Int,
        eccBits: Int,
        mask: Int,
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>
    ) {
        val data = (eccBits shl 3) or mask
        var rem = data
        for (i in 0 until 10) {
            rem = (rem shl 1) xor (((rem ushr 9) and 1) * 0x537)
        }
        val bits = ((data shl 10) or rem) xor 0x5412

        for (i in 0..5) setFunc(8, i, ((bits ushr i) and 1) != 0, modules, isFunction)
        setFunc(8, 7, ((bits ushr 6) and 1) != 0, modules, isFunction)
        setFunc(8, 8, ((bits ushr 7) and 1) != 0, modules, isFunction)
        setFunc(7, 8, ((bits ushr 8) and 1) != 0, modules, isFunction)
        for (i in 9 until 15) setFunc(14 - i, 8, ((bits ushr i) and 1) != 0, modules, isFunction)

        for (i in 0..7) setFunc(size - 1 - i, 8, ((bits ushr i) and 1) != 0, modules, isFunction)
        for (i in 8 until 15) setFunc(8, size - 15 + i, ((bits ushr i) and 1) != 0, modules, isFunction)
        setFunc(8, size - 8, true, modules, isFunction)
    }

    private fun computeReedSolomon(data: IntArray, degree: Int): IntArray {
        val gen = IntArray(degree)
        gen[degree - 1] = 1
        var root = 1
        for (i in 0 until degree) {
            for (j in 0 until degree) {
                gen[j] = gfMultiply(gen[j], root)
                if (j + 1 < degree) gen[j] = gen[j] xor gen[j + 1]
            }
            root = gfMultiply(root, 0x02)
        }

        val result = IntArray(degree)
        for (b in data) {
            val factor = b xor result[0]
            System.arraycopy(result, 1, result, 0, degree - 1)
            result[degree - 1] = 0
            for (j in 0 until degree) {
                result[j] = result[j] xor gfMultiply(gen[j], factor)
            }
        }
        return result
    }

    private fun gfMultiply(x: Int, y: Int): Int {
        var z = 0
        for (i in 7 downTo 0) {
            z = (z shl 1) xor (((z ushr 7) and 1) * 0x11D)
            z = z xor (((y ushr i) and 1) * x)
        }
        return z
    }

    private class MutableBitList {
        private val bits = ArrayList<Int>(512)
        val size: Int get() = bits.size
        fun append(value: Int, len: Int) {
            for (i in len - 1 downTo 0) {
                bits.add((value ushr i) and 1)
            }
        }
        fun get(index: Int): Int = bits[index]
    }
}
