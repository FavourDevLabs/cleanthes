package dev.favourdevlabs.cleanthes.data.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME    = "cleanthes.db";
    private static final int    DATABASE_VERSION = 3; // v3: totpAlgorithm column

    public static final String TABLE_VAULT_ENTRIES = "vault_entries";

    // Column names
    public static final String COLUMN_ID                 = "id";
    public static final String COLUMN_TITLE              = "title";
    public static final String COLUMN_USERNAME           = "username";
    public static final String COLUMN_ENCRYPTED_PASSWORD = "encryptedPassword";
    public static final String COLUMN_WEBSITE            = "website";
    public static final String COLUMN_CATEGORY           = "category";
    public static final String COLUMN_NOTES              = "notes";
    public static final String COLUMN_CREATED_AT         = "createdAt";
    public static final String COLUMN_UPDATED_AT         = "updatedAt";
    public static final String COLUMN_IS_FAVORITE        = "isFavorite";
    // v2 — TOTP core
    public static final String COLUMN_TOTP_SECRET        = "totpSecret";
    public static final String COLUMN_TOTP_ISSUER        = "totpIssuer";
    public static final String COLUMN_TOTP_DIGITS        = "totpDigits";
    public static final String COLUMN_TOTP_PERIOD        = "totpPeriod";
    // v3 — TOTP algorithm
    public static final String COLUMN_TOTP_ALGORITHM     = "totpAlgorithm";

    // Cursor indices — must match SELECT * column order exactly
    public static final int IDX_ID                 = 0;
    public static final int IDX_TITLE              = 1;
    public static final int IDX_USERNAME           = 2;
    public static final int IDX_ENCRYPTED_PASSWORD = 3;
    public static final int IDX_WEBSITE            = 4;
    public static final int IDX_CATEGORY           = 5;
    public static final int IDX_NOTES              = 6;
    public static final int IDX_CREATED_AT         = 7;
    public static final int IDX_UPDATED_AT         = 8;
    public static final int IDX_IS_FAVORITE        = 9;
    // v2
    public static final int IDX_TOTP_SECRET        = 10;
    public static final int IDX_TOTP_ISSUER        = 11;
    public static final int IDX_TOTP_DIGITS        = 12;
    public static final int IDX_TOTP_PERIOD        = 13;
    // v3
    public static final int IDX_TOTP_ALGORITHM     = 14;

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_VAULT_ENTRIES + " ("
                + COLUMN_ID                 + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_TITLE              + " TEXT NOT NULL, "
                + COLUMN_USERNAME           + " TEXT NOT NULL, "
                + COLUMN_ENCRYPTED_PASSWORD + " TEXT NOT NULL, "
                + COLUMN_WEBSITE            + " TEXT, "
                + COLUMN_CATEGORY           + " TEXT NOT NULL DEFAULT 'General', "
                + COLUMN_NOTES              + " TEXT, "
                + COLUMN_CREATED_AT         + " INTEGER NOT NULL, "
                + COLUMN_UPDATED_AT         + " INTEGER NOT NULL, "
                + COLUMN_IS_FAVORITE        + " INTEGER NOT NULL DEFAULT 0, "
                + COLUMN_TOTP_SECRET        + " TEXT DEFAULT NULL, "
                + COLUMN_TOTP_ISSUER        + " TEXT DEFAULT NULL, "
                + COLUMN_TOTP_DIGITS        + " INTEGER NOT NULL DEFAULT 6, "
                + COLUMN_TOTP_PERIOD        + " INTEGER NOT NULL DEFAULT 30, "
                + COLUMN_TOTP_ALGORITHM     + " TEXT NOT NULL DEFAULT 'SHA1'"
                + ");");

        db.execSQL("CREATE INDEX idx_category ON "
                + TABLE_VAULT_ENTRIES + " (" + COLUMN_CATEGORY + ");");
        db.execSQL("CREATE INDEX idx_favorite ON "
                + TABLE_VAULT_ENTRIES + " (" + COLUMN_IS_FAVORITE + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_VAULT_ENTRIES
                    + " ADD COLUMN " + COLUMN_TOTP_SECRET + " TEXT DEFAULT NULL");
            db.execSQL("ALTER TABLE " + TABLE_VAULT_ENTRIES
                    + " ADD COLUMN " + COLUMN_TOTP_ISSUER + " TEXT DEFAULT NULL");
            db.execSQL("ALTER TABLE " + TABLE_VAULT_ENTRIES
                    + " ADD COLUMN " + COLUMN_TOTP_DIGITS + " INTEGER NOT NULL DEFAULT 6");
            db.execSQL("ALTER TABLE " + TABLE_VAULT_ENTRIES
                    + " ADD COLUMN " + COLUMN_TOTP_PERIOD + " INTEGER NOT NULL DEFAULT 30");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_VAULT_ENTRIES
                    + " ADD COLUMN " + COLUMN_TOTP_ALGORITHM + " TEXT NOT NULL DEFAULT 'SHA1'");
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) db.enableWriteAheadLogging();
    }
}

