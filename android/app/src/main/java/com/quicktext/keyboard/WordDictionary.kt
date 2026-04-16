package com.quicktext.keyboard

import android.content.Context
import com.quicktext.R
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Word dictionary for autocomplete.
 * Loads words from res/raw/common_words.txt once, then answers prefix queries.
 *
 * Matching is diacritic-insensitive (so typing "toi" matches "tôi", "tối", "tồi",
 * etc.), but the original diacritic-preserving form is returned in suggestions.
 */
class WordDictionary(context: Context) {

    /** Original words as loaded from the dictionary file. */
    private val words: List<String>

    /** Parallel list: each word's diacritic-free lowercase form, used for prefix lookup. */
    private val normalized: List<String>

    init {
        val loaded = loadWords(context)
        words = loaded
        normalized = loaded.map { TelexProcessor.removeDiacritics(it).lowercase() }
    }

    private fun loadWords(context: Context): List<String> {
        val result = mutableListOf<String>()
        try {
            context.resources.openRawResource(R.raw.common_words).use { input ->
                BufferedReader(InputStreamReader(input, "UTF-8")).useLines { lines ->
                    for (line in lines) {
                        val trimmed = line.trim()
                        if (trimmed.isNotEmpty()) result.add(trimmed)
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback: empty dictionary.
        }
        return result.distinct()
    }

    /**
     * Return word candidates for a glide / swipe gesture. The path letters are
     * the keys the finger crossed (de-duplicated). Words must start with the
     * first key and end with the last key. Letters of the word should appear
     * as a subsequence of the path; the more letters that match, the higher
     * the score. Shorter words are slightly preferred when scores tie.
     */
    fun glideMatches(pathLetters: String, limit: Int = 6): List<String> {
        if (pathLetters.length < 2) return emptyList()
        val pathLower = pathLetters.lowercase()
        val first = pathLower.first()
        val last = pathLower.last()

        data class Scored(val word: String, val score: Double)
        val scored = mutableListOf<Scored>()
        for (i in words.indices) {
            val raw = words[i]
            // Glide matching is for single-token words only (skip phrases with spaces).
            if (raw.contains(' ')) continue
            val norm = normalized[i]
            if (norm.isBlank()) continue
            if (norm.first() != first) continue
            if (norm.last() != last) continue
            val matched = subsequenceMatchCount(norm, pathLower)
            // Score = fraction of word letters present, with mild bonus for shorter words.
            val score = matched.toDouble() / norm.length - norm.length * 0.005
            scored.add(Scored(raw, score))
        }
        return scored
            .sortedByDescending { it.score }
            .take(limit)
            .map { it.word }
    }

    private fun subsequenceMatchCount(word: String, path: String): Int {
        var count = 0
        var j = 0
        for (c in word) {
            while (j < path.length && path[j] != c) j++
            if (j < path.length) {
                count++
                j++
            } else {
                break
            }
        }
        return count
    }

    /**
     * Return up to [limit] suggestions whose diacritic-free lowercase prefix
     * starts with the diacritic-free form of [prefix]. Exact diacritic-free
     * matches come first, then shorter words, then dictionary order.
     */
    fun suggest(prefix: String, limit: Int = 6): List<String> {
        if (prefix.isBlank()) return emptyList()
        val normalizedPrefix = TelexProcessor.removeDiacritics(prefix).lowercase()
        if (normalizedPrefix.isEmpty()) return emptyList()

        val matches = mutableListOf<String>()
        for (i in words.indices) {
            if (normalized[i].startsWith(normalizedPrefix)) {
                matches.add(words[i])
            }
        }

        // Deduplicate while preserving insertion order
        val unique = matches.distinct().toMutableList()

        unique.sortWith(
            compareByDescending<String> {
                TelexProcessor.removeDiacritics(it).lowercase() == normalizedPrefix
            }
                .thenBy { it.length }
                .thenBy { it.lowercase() },
        )
        return unique.take(limit)
    }
}
