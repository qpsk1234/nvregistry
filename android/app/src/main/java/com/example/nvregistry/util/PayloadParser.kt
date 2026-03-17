package com.example.nvregistry.util

object PayloadParser {

    data class ParsedElement(
        val index: Int,
        val decValue: Long,
        val hexDisplay: String, // "0x0001"
        val byteStr: String     // "01,00"  ← SETコマンドで使う形式
    )

    /**
     * ペイロード文字列をSize/Count/TypeNameに基づいてパースし、
     * 各インデックスの値リストを返す。
     */
    fun parse(payload: String, typeName: String, size: Int, count: Int): List<ParsedElement> {
        val tokens = payload.split(",").map { it.trim() }
        val isSigned = typeName.startsWith("s")
        val hexPadLen = size * 2
        val result = mutableListOf<ParsedElement>()

        for (i in 0 until count) {
            val startByte = i * size
            if (startByte >= tokens.size) break

            val chunk = tokens.subList(startByte, minOf(startByte + size, tokens.size))

            // リトルエンディアンでデコード
            var rawValue = 0L
            chunk.forEachIndexed { j, hexStr ->
                val b = hexStr.toLongOrNull(16) ?: 0L
                rawValue = rawValue or (b shl (j * 8))
            }

            // 符号付き型の場合は符号拡張
            val decValue = if (isSigned && size < 8) {
                val bitSize = size * 8
                val signBit = 1L shl (bitSize - 1)
                if (rawValue and signBit != 0L) rawValue - (1L shl bitSize) else rawValue
            } else {
                rawValue
            }

            // HEX表示 (ビッグエンディアン表示, ゼロパディング)
            val hexDisplay = "0x" + chunk.reversed()
                .joinToString("") { it.padStart(2, '0').uppercase() }
                .padStart(hexPadLen, '0')

            val byteStr = chunk.joinToString(",") { it.lowercase().padStart(2, '0') }

            result.add(ParsedElement(i, decValue, hexDisplay, byteStr))
        }
        return result
    }

    /**
     * ユーザー入力のHEX文字列（"001A"等）→ リトルエンディアンのバイト列文字列（"1a,00"等）に変換。
     * SETコマンドのvalue部分に使う。
     */
    fun hexInputToByteStr(hexInput: String, size: Int): String {
        val clean = hexInput.removePrefix("0x").removePrefix("0X")
            .padStart(size * 2, '0').lowercase()
        return (0 until size).map { byteIdx ->
            val pos = clean.length - (byteIdx + 1) * 2
            if (pos >= 0) clean.substring(pos, pos + 2) else "00"
        }.joinToString(",")
    }

    /**
     * リスト表示用の短縮HEX文字列を生成する（最大5個+省略）
     */
    fun summarize(elements: List<ParsedElement>, typeName: String, count: Int): String {
        if (elements.isEmpty()) return "(empty)"
        val maxShow = 5
        val shown = elements.take(maxShow).joinToString(", ") { it.hexDisplay }
        val suffix = if (elements.size < count) " ... +${count - elements.size}more" else ""
        return "[$shown$suffix]"
    }

    /**
     * Decimal用の短縮文字列
     */
    fun summarizeDec(elements: List<ParsedElement>, count: Int): String {
        if (elements.isEmpty()) return "(empty)"
        val maxShow = 5
        val shown = elements.take(maxShow).joinToString(", ") { it.decValue.toString() }
        val suffix = if (elements.size < count) " ... +${count - elements.size}more" else ""
        return "[$shown$suffix]"
    }
}
