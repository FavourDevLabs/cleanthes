package dev.favourdevlabs.cleanthes.data.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import dev.favourdevlabs.cleanthes.data.entities.VaultEntry;

import java.util.ArrayList;
import java.util.List;

public class VaultDao {

    private final DatabaseHelper dbHelper;

    public VaultDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public long insert(VaultEntry entry) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.insert(DatabaseHelper.TABLE_VAULT_ENTRIES, null, entryToContentValues(entry));
    }

    public int update(VaultEntry entry) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String[] whereArgs = { String.valueOf(entry.getId()) };
        return db.update(DatabaseHelper.TABLE_VAULT_ENTRIES,
                entryToContentValues(entry),
                DatabaseHelper.COLUMN_ID + " = ?",
                whereArgs);
    }

    public int deleteById(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DatabaseHelper.TABLE_VAULT_ENTRIES,
                DatabaseHelper.COLUMN_ID + " = ?",
                new String[]{ String.valueOf(id) });
    }

    public int deleteAll() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DatabaseHelper.TABLE_VAULT_ENTRIES, null, null);
    }

    public List<VaultEntry> getAllEntries() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String orderBy = DatabaseHelper.COLUMN_IS_FAVORITE + " DESC, "
                + DatabaseHelper.COLUMN_TITLE + " ASC";
        Cursor cursor = db.query(DatabaseHelper.TABLE_VAULT_ENTRIES,
                null, null, null, null, null, orderBy);
        return cursorToList(cursor);
    }

    public VaultEntry getEntryById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_VAULT_ENTRIES,
                null,
                DatabaseHelper.COLUMN_ID + " = ?",
                new String[]{ String.valueOf(id) },
                null, null, null);
        if (cursor == null) return null;
        try {
            return cursor.moveToFirst() ? cursorToEntry(cursor) : null;
        } finally {
            cursor.close();
        }
    }

    public List<VaultEntry> searchEntries(String query) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String likeQuery = "%" + query + "%";
        String selection = DatabaseHelper.COLUMN_TITLE    + " LIKE ? OR "
                         + DatabaseHelper.COLUMN_USERNAME + " LIKE ?";
        String orderBy   = DatabaseHelper.COLUMN_IS_FAVORITE + " DESC, "
                         + DatabaseHelper.COLUMN_TITLE + " ASC";
        Cursor cursor = db.query(DatabaseHelper.TABLE_VAULT_ENTRIES,
                null, selection, new String[]{ likeQuery, likeQuery },
                null, null, orderBy);
        return cursorToList(cursor);
    }

    public List<VaultEntry> getEntriesByCategory(String category) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String orderBy = DatabaseHelper.COLUMN_IS_FAVORITE + " DESC, "
                       + DatabaseHelper.COLUMN_TITLE + " ASC";
        Cursor cursor = db.query(DatabaseHelper.TABLE_VAULT_ENTRIES,
                null,
                DatabaseHelper.COLUMN_CATEGORY + " = ?",
                new String[]{ category },
                null, null, orderBy);
        return cursorToList(cursor);
    }

    public List<VaultEntry> getFavoriteEntries() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_VAULT_ENTRIES,
                null,
                DatabaseHelper.COLUMN_IS_FAVORITE + " = ?",
                new String[]{ "1" },
                null, null,
                DatabaseHelper.COLUMN_TITLE + " ASC");
        return cursorToList(cursor);
    }

    public int getEntryCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_VAULT_ENTRIES, null);
        try {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            cursor.close();
        }
    }

    public List<String> getAllCategories() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<String> categories = new ArrayList<>();
        Cursor cursor = db.rawQuery(
                "SELECT DISTINCT " + DatabaseHelper.COLUMN_CATEGORY
                + " FROM " + DatabaseHelper.TABLE_VAULT_ENTRIES
                + " ORDER BY " + DatabaseHelper.COLUMN_CATEGORY + " ASC", null);
        try {
            if (cursor.moveToFirst()) {
                do { categories.add(cursor.getString(0)); }
                while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        return categories;
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private ContentValues entryToContentValues(VaultEntry entry) {
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COLUMN_TITLE,              entry.getTitle());
        cv.put(DatabaseHelper.COLUMN_USERNAME,           entry.getUsername());
        cv.put(DatabaseHelper.COLUMN_ENCRYPTED_PASSWORD, entry.getEncryptedPassword());
        cv.put(DatabaseHelper.COLUMN_WEBSITE,            entry.getWebsite());
        cv.put(DatabaseHelper.COLUMN_CATEGORY,           entry.getCategory());
        cv.put(DatabaseHelper.COLUMN_NOTES,              entry.getNotes());
        cv.put(DatabaseHelper.COLUMN_CREATED_AT,         entry.getCreatedAt());
        cv.put(DatabaseHelper.COLUMN_UPDATED_AT,         entry.getUpdatedAt());
        cv.put(DatabaseHelper.COLUMN_IS_FAVORITE,        entry.isFavorite() ? 1 : 0);
        // TOTP — null is intentional for entries without TOTP configured
        cv.put(DatabaseHelper.COLUMN_TOTP_SECRET, entry.getTotpSecret());
        cv.put(DatabaseHelper.COLUMN_TOTP_ISSUER, entry.getTotpIssuer());
        cv.put(DatabaseHelper.COLUMN_TOTP_DIGITS, entry.getTotpDigits());
        cv.put(DatabaseHelper.COLUMN_TOTP_PERIOD, entry.getTotpPeriod());
        return cv;
    }

    private VaultEntry cursorToEntry(Cursor cursor) {
        VaultEntry entry = new VaultEntry();
        entry.setId(cursor.getLong(DatabaseHelper.IDX_ID));
        entry.setTitle(cursor.getString(DatabaseHelper.IDX_TITLE));
        entry.setUsername(cursor.getString(DatabaseHelper.IDX_USERNAME));
        entry.setEncryptedPassword(cursor.getString(DatabaseHelper.IDX_ENCRYPTED_PASSWORD));
        entry.setWebsite(cursor.getString(DatabaseHelper.IDX_WEBSITE));
        entry.setCategory(cursor.getString(DatabaseHelper.IDX_CATEGORY));
        entry.setNotes(cursor.getString(DatabaseHelper.IDX_NOTES));
        entry.setCreatedAt(cursor.getLong(DatabaseHelper.IDX_CREATED_AT));
        entry.setUpdatedAt(cursor.getLong(DatabaseHelper.IDX_UPDATED_AT));
        entry.setFavorite(cursor.getInt(DatabaseHelper.IDX_IS_FAVORITE) == 1);
        // Null guards required: existing rows have NULL in TOTP columns after migration.
        // getString() on a NULL column returns null in Android — that's fine.
        // But getInt() on a NULL column returns 0, which would corrupt our defaults.
        if (!cursor.isNull(DatabaseHelper.IDX_TOTP_SECRET)) {
            entry.setTotpSecret(cursor.getString(DatabaseHelper.IDX_TOTP_SECRET));
        }
        if (!cursor.isNull(DatabaseHelper.IDX_TOTP_ISSUER)) {
            entry.setTotpIssuer(cursor.getString(DatabaseHelper.IDX_TOTP_ISSUER));
        }
        entry.setTotpDigits(cursor.isNull(DatabaseHelper.IDX_TOTP_DIGITS)
                ? 6 : cursor.getInt(DatabaseHelper.IDX_TOTP_DIGITS));
        entry.setTotpPeriod(cursor.isNull(DatabaseHelper.IDX_TOTP_PERIOD)
                ? 30 : cursor.getInt(DatabaseHelper.IDX_TOTP_PERIOD));
        return entry;
    }

    private List<VaultEntry> cursorToList(Cursor cursor) {
        List<VaultEntry> entries = new ArrayList<>();
        if (cursor == null) return entries;
        try {
            if (cursor.moveToFirst()) {
                do { entries.add(cursorToEntry(cursor)); }
                while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        return entries;
    }
}

