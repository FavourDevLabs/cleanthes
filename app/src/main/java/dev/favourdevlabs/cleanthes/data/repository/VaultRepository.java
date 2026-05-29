package dev.favourdevlabs.cleanthes.data.repository;

import android.content.Context;

import dev.favourdevlabs.cleanthes.data.db.DatabaseHelper;
import dev.favourdevlabs.cleanthes.data.db.VaultDao;
import dev.favourdevlabs.cleanthes.data.entities.VaultEntry;
import dev.favourdevlabs.cleanthes.security.CryptoManager;

import java.util.List;
import javax.crypto.SecretKey;

public class VaultRepository {

    private final VaultDao vaultDao;
    private static VaultRepository instance;

    public static synchronized VaultRepository getInstance(Context context) {
        if (instance == null) {
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(context.getApplicationContext());
            instance = new VaultRepository(new VaultDao(dbHelper));
        }
        return instance;
    }

    public VaultRepository(VaultDao vaultDao) {
        this.vaultDao = vaultDao;
    }

    // -----------------------------------------------------------------------
    // Write operations
    // -----------------------------------------------------------------------

    /** Original signature — no TOTP. Delegates to the full overload. */
    public long addEntry(String title, String userName, String plainPassword,
            String website, String category, String notes,
            boolean isFavorite, SecretKey secretKey) throws Exception {
        return addEntry(title, userName, plainPassword, website, category, notes,
                isFavorite, null, null, 6, 30, secretKey);
    }

    /** Full signature — includes TOTP. All TOTP params are nullable. */
    public long addEntry(String title, String userName, String plainPassword,
            String website, String category, String notes, boolean isFavorite,
            String plainTotpSecret, String totpIssuer,
            int totpDigits, int totpPeriod,
            SecretKey secretKey) throws Exception {

        String encryptedPassword = CryptoManager.encrypt(plainPassword, secretKey);
        String encryptedTotp = (plainTotpSecret != null && !plainTotpSecret.isEmpty())
                ? CryptoManager.encrypt(plainTotpSecret, secretKey)
                : null;

        long now = System.currentTimeMillis();
        VaultEntry entry = new VaultEntry(title, userName, encryptedPassword,
                website, category, notes, isFavorite);
        entry.setCreatedAt(now);
        entry.setUpdatedAt(now);
        entry.setTotpSecret(encryptedTotp);
        entry.setTotpIssuer(totpIssuer);
        entry.setTotpDigits(totpDigits);
        entry.setTotpPeriod(totpPeriod);

        long newId = vaultDao.insert(entry);
        if (newId != -1)
            entry.setId(newId);
        return newId;
    }

    /**
     * Update an existing entry.
     *
     * IMPORTANT: by the time this is called, entry.getTotpSecret() holds PLAINTEXT
     * —
     * decryptEntry() already decrypted it on load, and the form may have changed
     * it.
     * We encrypt it here before writing back to the DB.
     */
    public int updateEntry(VaultEntry entry, String plainPassword, SecretKey secretKey) throws Exception {
        entry.setEncryptedPassword(CryptoManager.encrypt(plainPassword, secretKey));

        if (entry.getTotpSecret() != null && !entry.getTotpSecret().isEmpty()) {
            entry.setTotpSecret(CryptoManager.encrypt(entry.getTotpSecret(), secretKey));
        } else {
            entry.setTotpSecret(null);
        }

        entry.setUpdatedAt(System.currentTimeMillis());
        return vaultDao.update(entry);
    }

    public int deleteEntry(long id) {
        return vaultDao.deleteById(id);
    }

    public int wipeVault() {
        return vaultDao.deleteAll();
    }

    // -----------------------------------------------------------------------
    // Read operations
    // -----------------------------------------------------------------------

    public List<VaultEntry> getAllEntries(SecretKey secretKey) throws Exception {
        return decryptEntries(vaultDao.getAllEntries(), secretKey);
    }

    public VaultEntry getEntryById(long id, SecretKey secretKey) throws Exception {
        VaultEntry entry = vaultDao.getEntryById(id);
        if (entry == null)
            return null;
        return decryptEntry(entry, secretKey);
    }

    public List<VaultEntry> searchEntries(String query, SecretKey secretKey) throws Exception {
        return decryptEntries(vaultDao.searchEntries(query), secretKey);
    }

    public List<VaultEntry> getEntriesByCategory(String category, SecretKey secretKey) throws Exception {
        return decryptEntries(vaultDao.getEntriesByCategory(category), secretKey);
    }

    public List<VaultEntry> getFavoriteEntries(SecretKey secretKey) throws Exception {
        return decryptEntries(vaultDao.getFavoriteEntries(), secretKey);
    }

    public List<String> getAllCategories() {
        return vaultDao.getAllCategories();
    }

    public int getEntryCount() {
        return vaultDao.getEntryCount();
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private VaultEntry decryptEntry(VaultEntry entry, SecretKey secretKey) throws Exception {
        entry.setEncryptedPassword(
                CryptoManager.decrypt(entry.getEncryptedPassword(), secretKey));
        // Only decrypt TOTP secret when one is actually stored
        if (entry.getTotpSecret() != null && !entry.getTotpSecret().isEmpty()) {
            entry.setTotpSecret(
                    CryptoManager.decrypt(entry.getTotpSecret(), secretKey));
        }
        return entry;
    }

    private List<VaultEntry> decryptEntries(List<VaultEntry> entries,
            SecretKey secretKey) throws Exception {
        for (VaultEntry entry : entries)
            decryptEntry(entry, secretKey);
        return entries;
    }
}
