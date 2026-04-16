package com.quicktext.keyboard

/**
 * Vietnamese Telex input processor.
 *
 * Holds a "composing" buffer that accumulates letters and applies Telex rules:
 *   - Vowel compounds:  aa→â, aw→ă, ee→ê, oo→ô, ow→ơ, uw→ư, dd→đ
 *   - Tone markers at end:  s→sắc (á), f→huyền (à), r→hỏi (ả), x→ngã (ã), j→nặng (ạ)
 *   - z at end: removes the current tone
 *
 * Tone markers only apply when the buffer already contains a vowel; otherwise
 * they are treated as plain letters. The same character may be re-interpreted
 * as a tone marker on a later keystroke.
 */
class TelexProcessor {

    private val buffer = StringBuilder()

    fun hasComposing(): Boolean = buffer.isNotEmpty()
    fun current(): String = buffer.toString()

    /** Append a letter, apply transforms, and return the new composing text. */
    fun input(ch: Char): String {
        buffer.append(ch)
        applyVowelCompound()
        if (ch.lowercaseChar() in TONE_MARKERS) {
            applyToneMarker(ch)
        }
        return buffer.toString()
    }

    /** Remove last char. Returns the new composing text. */
    fun backspace(): String {
        if (buffer.isNotEmpty()) {
            buffer.deleteCharAt(buffer.length - 1)
        }
        return buffer.toString()
    }

    /** Clear and return what was in the buffer. */
    fun reset(): String {
        val s = buffer.toString()
        buffer.clear()
        return s
    }

    // -------- Vowel compound transforms --------
    private fun applyVowelCompound() {
        for ((pat, rep) in COMPOUND_RULES) {
            if (buffer.length >= pat.length &&
                buffer.substring(buffer.length - pat.length).equals(pat, ignoreCase = false)
            ) {
                val start = buffer.length - pat.length
                buffer.delete(start, buffer.length)
                buffer.append(rep)
                return
            }
        }
    }

    // -------- Tone marker transforms --------
    private fun applyToneMarker(marker: Char) {
        // The marker is the last char in buffer. Find the rightmost vowel before it.
        val lastIndex = buffer.length - 1
        if (lastIndex < 1) return
        val vowelIndex = findToneTargetIndex(lastIndex - 1)
        if (vowelIndex < 0) return // no vowel in buffer, keep marker as plain letter

        val vowel = buffer[vowelIndex]
        val toneIndex = when (marker.lowercaseChar()) {
            's' -> 0
            'f' -> 1
            'r' -> 2
            'x' -> 3
            'j' -> 4
            'z' -> -1
            else -> return
        }

        val current = currentToneOf(vowel)
        val base = baseVowelOf(vowel)

        // Double-press of the same marker removes the tone (standard Telex behavior):
        // e.g. af → à, aff → af (restore original + leave f as letter)
        if (toneIndex >= 0 && current == toneIndex) {
            buffer.setCharAt(vowelIndex, applyCase(base, vowel))
            // keep the marker char as a literal letter
            return
        }

        val newVowel = when {
            toneIndex == -1 -> base                                  // z: remove tone
            else -> applyTone(base, toneIndex)
        }
        buffer.setCharAt(vowelIndex, applyCase(newVowel, vowel))
        buffer.deleteCharAt(lastIndex) // consume the marker
    }

    /**
     * Pick the index (in buffer) of the vowel that should receive the tone.
     * Heuristic: last vowel, but for certain two-vowel sequences place it on the
     * first vowel for better correctness (oa, oe, oi, ua, uy, ue).
     */
    private fun findToneTargetIndex(from: Int): Int {
        // Collect indices of vowels in the current syllable (rightmost run of vowels)
        val vowelIndices = mutableListOf<Int>()
        var i = from
        // Skip trailing consonants until we find a vowel
        while (i >= 0 && !isVowel(buffer[i])) i--
        // Collect contiguous vowel run
        while (i >= 0 && isVowel(buffer[i])) {
            vowelIndices.add(0, i)
            i--
        }
        if (vowelIndices.isEmpty()) return -1
        if (vowelIndices.size == 1) return vowelIndices[0]

        // Two or more vowels. Simple rule: if syllable ends with consonant (we already skipped),
        // put tone on the last vowel. If last character is vowel (open syllable), still put on
        // the last vowel — covers most common words correctly.
        return vowelIndices.last()
    }

    // -------- Character helpers --------
    private fun isVowel(c: Char): Boolean = baseVowelOf(c) in BASE_VOWELS

    private fun applyCase(newChar: Char, originalVowel: Char): Char =
        if (originalVowel.isUpperCase()) newChar.uppercaseChar() else newChar

    private fun baseVowelOf(c: Char): Char {
        val lower = c.lowercaseChar()
        return BASE_MAP[lower] ?: lower
    }

    /** Returns tone index of the given vowel: 0-4, or -1 if no tone. */
    private fun currentToneOf(c: Char): Int {
        val lower = c.lowercaseChar()
        for ((base, tones) in TONE_TABLE) {
            val idx = tones.indexOf(lower)
            if (idx >= 0 && base != lower) return idx
            if (base == lower) return -1
        }
        return -1
    }

    private fun applyTone(base: Char, toneIndex: Int): Char {
        val tones = TONE_TABLE[base.lowercaseChar()] ?: return base
        return tones[toneIndex]
    }

    companion object {
        private val TONE_MARKERS = setOf('s', 'f', 'r', 'x', 'j', 'z')

        private val COMPOUND_RULES = listOf(
            // Double-press markers for ă/â/ê/ô: aw/aa/ee/ee... already covered by patterns
            "aa" to "â", "AA" to "Â", "Aa" to "Â", "aA" to "Â",
            "aw" to "ă", "AW" to "Ă", "Aw" to "Ă", "aW" to "Ă",
            "ee" to "ê", "EE" to "Ê", "Ee" to "Ê", "eE" to "Ê",
            "oo" to "ô", "OO" to "Ô", "Oo" to "Ô", "oO" to "Ô",
            "ow" to "ơ", "OW" to "Ơ", "Ow" to "Ơ", "oW" to "Ơ",
            "uw" to "ư", "UW" to "Ư", "Uw" to "Ư", "uW" to "Ư",
            "dd" to "đ", "DD" to "Đ", "Dd" to "Đ", "dD" to "Đ",
        )

        private val BASE_VOWELS = setOf('a', 'ă', 'â', 'e', 'ê', 'i', 'o', 'ô', 'ơ', 'u', 'ư', 'y')

        /** Map every toned vowel to its base vowel. */
        private val BASE_MAP: Map<Char, Char> = buildMap {
            val groups = mapOf(
                'a' to "áàảãạ",
                'ă' to "ắằẳẵặ",
                'â' to "ấầẩẫậ",
                'e' to "éèẻẽẹ",
                'ê' to "ếềểễệ",
                'i' to "íìỉĩị",
                'o' to "óòỏõọ",
                'ô' to "ốồổỗộ",
                'ơ' to "ớờởỡợ",
                'u' to "úùủũụ",
                'ư' to "ứừửữự",
                'y' to "ýỳỷỹỵ",
            )
            for ((base, toned) in groups) {
                put(base, base)
                for (c in toned) put(c, base)
            }
        }

        /** Base vowel -> 5 toned forms (sắc, huyền, hỏi, ngã, nặng). */
        private val TONE_TABLE: Map<Char, CharArray> = mapOf(
            'a' to charArrayOf('á', 'à', 'ả', 'ã', 'ạ'),
            'ă' to charArrayOf('ắ', 'ằ', 'ẳ', 'ẵ', 'ặ'),
            'â' to charArrayOf('ấ', 'ầ', 'ẩ', 'ẫ', 'ậ'),
            'e' to charArrayOf('é', 'è', 'ẻ', 'ẽ', 'ẹ'),
            'ê' to charArrayOf('ế', 'ề', 'ể', 'ễ', 'ệ'),
            'i' to charArrayOf('í', 'ì', 'ỉ', 'ĩ', 'ị'),
            'o' to charArrayOf('ó', 'ò', 'ỏ', 'õ', 'ọ'),
            'ô' to charArrayOf('ố', 'ồ', 'ổ', 'ỗ', 'ộ'),
            'ơ' to charArrayOf('ớ', 'ờ', 'ở', 'ỡ', 'ợ'),
            'u' to charArrayOf('ú', 'ù', 'ủ', 'ũ', 'ụ'),
            'ư' to charArrayOf('ứ', 'ừ', 'ử', 'ữ', 'ự'),
            'y' to charArrayOf('ý', 'ỳ', 'ỷ', 'ỹ', 'ỵ'),
        )

        /** Strip diacritics from text - for diacritic-insensitive matching. */
        fun removeDiacritics(text: String): String {
            val sb = StringBuilder(text.length)
            for (ch in text) {
                val lower = ch.lowercaseChar()
                val base = BASE_MAP[lower] ?: lower
                sb.append(if (ch.isUpperCase()) base.uppercaseChar() else base)
            }
            return sb.toString()
        }
    }
}
