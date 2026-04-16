import {open, type DB} from '@op-engineering/op-sqlite';
import {Phrase} from '../types';

let db: DB | null = null;

function getDB(): DB {
  if (!db) {
    db = open({name: 'quicktext.db'});

    // Create phrases table
    db.executeSync(`
      CREATE TABLE IF NOT EXISTS phrases (
        id TEXT PRIMARY KEY,
        shortcut TEXT NOT NULL,
        text TEXT NOT NULL,
        category TEXT NOT NULL DEFAULT 'Chung',
        createdAt INTEGER NOT NULL,
        updatedAt INTEGER NOT NULL
      )
    `);

    // Create index for fast prefix search
    db.executeSync(
      'CREATE INDEX IF NOT EXISTS idx_phrases_shortcut ON phrases(shortcut)',
    );
  }
  return db;
}

function rowToPhrase(row: Record<string, unknown>): Phrase {
  return {
    id: String(row.id),
    shortcut: String(row.shortcut),
    text: String(row.text),
    category: String(row.category),
    createdAt: Number(row.createdAt),
    updatedAt: Number(row.updatedAt),
  };
}

export async function initializePhrases(): Promise<Phrase[]> {
  // Just ensure the table exists. No seed data.
  getDB();
  return getAllPhrases();
}

export async function getAllPhrases(): Promise<Phrase[]> {
  const database = getDB();
  const result = database.executeSync(
    'SELECT * FROM phrases ORDER BY updatedAt DESC',
  );
  return (result.rows ?? []).map(rowToPhrase);
}

export async function savePhrase(phrase: Phrase): Promise<void> {
  const database = getDB();
  database.executeSync(
    `INSERT OR REPLACE INTO phrases (id, shortcut, text, category, createdAt, updatedAt)
     VALUES (?, ?, ?, ?, ?, ?)`,
    [
      phrase.id,
      phrase.shortcut,
      phrase.text,
      phrase.category,
      phrase.createdAt,
      phrase.updatedAt,
    ],
  );
}

export async function deletePhrase(id: string): Promise<void> {
  const database = getDB();
  database.executeSync('DELETE FROM phrases WHERE id = ?', [id]);
}

export async function searchPhrases(query: string): Promise<Phrase[]> {
  const database = getDB();
  const result = database.executeSync(
    `SELECT * FROM phrases
     WHERE shortcut LIKE ? OR text LIKE ? OR category LIKE ?
     ORDER BY
       CASE WHEN shortcut LIKE ? THEN 0 ELSE 1 END,
       updatedAt DESC`,
    [`${query}%`, `%${query}%`, `%${query}%`, `${query}%`],
  );
  return (result.rows ?? []).map(rowToPhrase);
}

export async function getCategories(): Promise<string[]> {
  const database = getDB();
  const result = database.executeSync(
    'SELECT DISTINCT category FROM phrases ORDER BY category',
  );
  return (result.rows ?? []).map(row => String(row.category));
}
