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
        return dbHelper.getWritableDatabase()
                .insert(DatabaseHelper.TABLE_VAULT_ENTRIES, null, toContentValues(entry));
    }

    public int update(VaultEntry entry) {
        return dbHelper.getWritableDatabase().update(
                DatabaseHelper.TABLE_VAULT_ENTRIES,
                toContentValues(entry),
                DatabaseHelper.COLUMN_ID + " = ?",
                new String[]{ String.valueOf(entry.getId()) });
    }

    public int deleteById(long id) {
        return dbHelper.getWritableDatabase().delete(
                DatabaseHelper.TABLE_VAULT_ENTRIES,
                DatabaseHelper.COLUMN_ID + " = ?",
                new String[]{ String.valueOf(id) });
    }

    public int deleteAll() {
        return dbHelper.getWritableDatabase()
                .delete(DatabaseHelper.TABLE_VAULT_ENTRIES, null, null);
    }

    public List<VaultEntry> getAllEntries() {
        String order = DatabaseHelper.COLUMN_IS_FAVORITE + " DESC, "
                + DatabaseHelper.COLUMN_TITLE + " ASC";
        return cursorToList(dbHelper.getReadableDatabase()
                .query(DatabaseHelper.TABLE_VAULT_ENTRIES, null, null, null, null, null, order));
    }

    public VaultEntry getEntryById(long id) {
        Cursor cursor = dbHelper.getReadableDatabase().query(
                DatabaseHelper.TABLE_VAULT_ENTRIES, null,
                DatabaseHelper.COLUMN_ID + " = ?",
                new String[]{ String.valueOf(id) },
                null, null, null);
        if (cursor == null) return null;
        try {
            return cursor.moveToFirst() ? fromCursor(cursor) : null;
        } finally {
            cursor.close();
        }
    }

    public List<VaultEntry> searchEntries(String query) {
        String like  = "%" + query + "%";
        String where = DatabaseHelper.COLUMN_TITLE    + " LIKE ? OR "
                     + DatabaseHelper.COLUMN_USERNAME + " LIKE ?";
        String order = DatabaseHelper.COLUMN_IS_FAVORITE + " DESC, "
                     + DatabaseHelper.COLUMN_TITLE + " ASC";
        return cursorToList(dbHelper.getReadableDatabase()
                .query(DatabaseHelper.TABLE_VAULT_ENTRIES, null,
                        where, new String[]{ like, like }, null, null, order));
    }

    public List<VaultEntry> getEntriesByCategory(String category) {
        String order = DatabaseHelper.COLUMN_IS_FAVORITE + " DESC, "
                     + DatabaseHelper.COLUMN_TITLE + " ASC";
        return cursorToList(dbHelper.getReadableDatabase()
                .query(DatabaseHelper.TABLE_VAULT_ENTRIES, null,
                        DatabaseHelper.COLUMN_CATEGORY + " = ?",
                        new String[]{ category }, null, null, order));
    }

    public List<VaultEntry> getFavoriteEntries() {
        return cursorToList(dbHelper.getReadableDatabase()
                .query(DatabaseHelper.TABLE_VAULT_ENTRIES, null,
                        DatabaseHelper.COLUMN_IS_FAVORITE + " = ?",
                        new String[]{ "1" }, null, null,
                        DatabaseHelper.COLUMN_TITLE + " ASC"));
    }

    public int getEntryCount() {
        Cursor c = dbHelper.getReadableDatabase()
                .rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_VAULT_ENTRIES, null);
        try { return c.moveToFirst() ? c.getInt(0) : 0; } finally { c.close(); }
    }

    public List<String> getAllCategories() {
        List<String> list = new ArrayList<>();
        Cursor c = dbHelper.getReadableDatabase().rawQuery(
                "SELECT DISTINCT " + DatabaseHelper.COLUMN_CATEGORY
                + " FROM " + DatabaseHelper.TABLE_VAULT_ENTRIES
                + " ORDER BY " + DatabaseHelper.COLUMN_CATEGORY + " ASC", null);
        try {
            if (c.moveToFirst()) do { list.add(c.getString(0)); } while (c.moveToNext());
        } finally { c.close(); }
        return list;
    }

    // -------------------------------------------------------------------------

    private ContentValues toContentValues(VaultEntry e) {
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COLUMN_TITLE,              e.getTitle());
        cv.put(DatabaseHelper.COLUMN_USERNAME,           e.getUsername());
        cv.put(DatabaseHelper.COLUMN_ENCRYPTED_PASSWORD, e.getEncryptedPassword());
        cv.put(DatabaseHelper.COLUMN_WEBSITE,            e.getWebsite());
        cv.put(DatabaseHelper.COLUMN_CATEGORY,           e.getCategory());
        cv.put(DatabaseHelper.COLUMN_NOTES,              e.getNotes());
        cv.put(DatabaseHelper.COLUMN_CREATED_AT,         e.getCreatedAt());
        cv.put(DatabaseHelper.COLUMN_UPDATED_AT,         e.getUpdatedAt());
        cv.put(DatabaseHelper.COLUMN_IS_FAVORITE,        e.isFavorite() ? 1 : 0);
        cv.put(DatabaseHelper.COLUMN_TOTP_SECRET,        e.getTotpSecret());
        cv.put(DatabaseHelper.COLUMN_TOTP_ISSUER,        e.getTotpIssuer());
        cv.put(DatabaseHelper.COLUMN_TOTP_DIGITS,        e.getTotpDigits());
        cv.put(DatabaseHelper.COLUMN_TOTP_PERIOD,        e.getTotpPeriod());
        cv.put(DatabaseHelper.COLUMN_TOTP_ALGORITHM,     e.getTotpAlgorithm());
        return cv;
    }

    private VaultEntry fromCursor(Cursor c) {
        VaultEntry e = new VaultEntry();
        e.setId(c.getLong(DatabaseHelper.IDX_ID));
        e.setTitle(c.getString(DatabaseHelper.IDX_TITLE));
        e.setUsername(c.getString(DatabaseHelper.IDX_USERNAME));
        e.setEncryptedPassword(c.getString(DatabaseHelper.IDX_ENCRYPTED_PASSWORD));
        e.setWebsite(c.getString(DatabaseHelper.IDX_WEBSITE));
        e.setCategory(c.getString(DatabaseHelper.IDX_CATEGORY));
        e.setNotes(c.getString(DatabaseHelper.IDX_NOTES));
        e.setCreatedAt(c.getLong(DatabaseHelper.IDX_CREATED_AT));
        e.setUpdatedAt(c.getLong(DatabaseHelper.IDX_UPDATED_AT));
        e.setFavorite(c.getInt(DatabaseHelper.IDX_IS_FAVORITE) == 1);

        if (!c.isNull(DatabaseHelper.IDX_TOTP_SECRET))
            e.setTotpSecret(c.getString(DatabaseHelper.IDX_TOTP_SECRET));
        if (!c.isNull(DatabaseHelper.IDX_TOTP_ISSUER))
            e.setTotpIssuer(c.getString(DatabaseHelper.IDX_TOTP_ISSUER));

        e.setTotpDigits(c.isNull(DatabaseHelper.IDX_TOTP_DIGITS)
                ? 6 : c.getInt(DatabaseHelper.IDX_TOTP_DIGITS));
        e.setTotpPeriod(c.isNull(DatabaseHelper.IDX_TOTP_PERIOD)
                ? 30 : c.getInt(DatabaseHelper.IDX_TOTP_PERIOD));
        e.setTotpAlgorithm(c.isNull(DatabaseHelper.IDX_TOTP_ALGORITHM)
                ? "SHA1" : c.getString(DatabaseHelper.IDX_TOTP_ALGORITHM));
        return e;
    }

    private List<VaultEntry> cursorToList(Cursor c) {
        List<VaultEntry> list = new ArrayList<>();
        if (c == null) return list;
        try {
            if (c.moveToFirst()) do { list.add(fromCursor(c)); } while (c.moveToNext());
        } finally { c.close(); }
        return list;
    }
}

