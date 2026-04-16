package com.quicktext.keyboard

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.File

data class Phrase(
    val id: String,
    val shortcut: String,
    val text: String,
    val category: String,
)

/**
 * Reads phrases from the SQLite database created by op-sqlite (used by the React Native app).
 * op-sqlite stores databases at context.getDatabasePath("<name>") which is the Android
 * default: /data/data/<package>/databases/<name>.
 */
class PhrasesDatabase(private val context: Context) {

    companion object {
        private const val TAG = "PhrasesDatabase"
        private const val DB_NAME = "quicktext.db"
    }

    private fun openDatabase(): SQLiteDatabase? {
        return try {
            val dbFile: File = context.getDatabasePath(DB_NAME)
            if (!dbFile.exists()) {
                Log.w(TAG, "Database not found at ${dbFile.absolutePath}. Open the main app first.")
                return null
            }
            SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open database", e)
            null
        }
    }

    /**
     * Returns phrases whose shortcut starts with the query (prefix match), or whose
     * text/category contains the query. Ordered so that shortcut-prefix matches come first.
     */
    fun searchPhrases(query: String, limit: Int = 10): List<Phrase> {
        if (query.isBlank()) return emptyList()
        val db = openDatabase() ?: return emptyList()
        val results = mutableListOf<Phrase>()
        try {
            val cursor = db.rawQuery(
                """
                SELECT id, shortcut, text, category FROM phrases
                WHERE shortcut LIKE ? OR text LIKE ?
                ORDER BY
                    CASE WHEN shortcut LIKE ? THEN 0 ELSE 1 END,
                    updatedAt DESC
                LIMIT ?
                """.trimIndent(),
                arrayOf(
                    "$query%",
                    "%$query%",
                    "$query%",
                    limit.toString(),
                ),
            )
            cursor.use { c ->
                while (c.moveToNext()) {
                    results.add(
                        Phrase(
                            id = c.getString(0),
                            shortcut = c.getString(1),
                            text = c.getString(2),
                            category = c.getString(3) ?: "",
                        ),
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Query failed", e)
        } finally {
            db.close()
        }
        return results
    }
}
