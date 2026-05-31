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
            instance = new VaultRepository(
                    new VaultDao(DatabaseHelper.getInstance(context.getApplicationContext())));
        }
        return instance;
    }

    public VaultRepository(VaultDao vaultDao) {
        this.vaultDao = vaultDao;
    }

    // -------------------------------------------------------------------------
    // Write
    // -------------------------------------------------------------------------

    /** No TOTP — backward-compatible overload. */
    public long addEntry(String title, String userName, String plainPassword,
            String website, String category, String notes,
            boolean isFavorite, SecretKey key) throws Exception {
        return addEntry(title, userName, plainPassword, website, category, notes,
                isFavorite, null, null, 6, 30, "SHA1", key);
    }

    /** Full overload — includes all TOTP fields. All TOTP params nullable. */
    public long addEntry(String title, String userName, String plainPassword,
            String website, String category, String notes, boolean isFavorite,
            String plainTotpSecret, String totpIssuer,
            int totpDigits, int totpPeriod, String totpAlgorithm,
            SecretKey key) throws Exception {

        String encPwd = CryptoManager.encrypt(plainPassword, key);
        String encTotp = (plainTotpSecret != null && !plainTotpSecret.isEmpty())
                ? CryptoManager.encrypt(plainTotpSecret, key)
                : null;

        long now = System.currentTimeMillis();
        VaultEntry entry = new VaultEntry(title, userName, encPwd,
                website, category, notes, isFavorite);
        entry.setCreatedAt(now);
        entry.setUpdatedAt(now);
        entry.setTotpSecret(encTotp);
        entry.setTotpIssuer(totpIssuer);
        entry.setTotpDigits(totpDigits);
        entry.setTotpPeriod(totpPeriod);
        entry.setTotpAlgorithm(totpAlgorithm != null ? totpAlgorithm : "SHA1");

        long id = vaultDao.insert(entry);
        if (id != -1)
            entry.setId(id);
        return id;
    }

    public int updateEntry(VaultEntry entry, String plainPassword, SecretKey key) throws Exception {
        entry.setEncryptedPassword(CryptoManager.encrypt(plainPassword, key));
        if (entry.getTotpSecret() != null && !entry.getTotpSecret().isEmpty()) {
            entry.setTotpSecret(CryptoManager.encrypt(entry.getTotpSecret(), key));
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

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    public List<VaultEntry> getAllEntries(SecretKey key) throws Exception {
        return decryptAll(vaultDao.getAllEntries(), key);
    }

    public VaultEntry getEntryById(long id, SecretKey key) throws Exception {
        VaultEntry e = vaultDao.getEntryById(id);
        return e == null ? null : decrypt(e, key);
    }

    public List<VaultEntry> searchEntries(String query, SecretKey key) throws Exception {
        return decryptAll(vaultDao.searchEntries(query), key);
    }

    public List<VaultEntry> getEntriesByCategory(String cat, SecretKey key) throws Exception {
        return decryptAll(vaultDao.getEntriesByCategory(cat), key);
    }

    public List<VaultEntry> getFavoriteEntries(SecretKey key) throws Exception {
        return decryptAll(vaultDao.getFavoriteEntries(), key);
    }

    public List<String> getAllCategories() {
        return vaultDao.getAllCategories();
    }

    public int getEntryCount() {
        return vaultDao.getEntryCount();
    }

    // -------------------------------------------------------------------------

    private VaultEntry decrypt(VaultEntry e, SecretKey key) throws Exception {
        e.setEncryptedPassword(CryptoManager.decrypt(e.getEncryptedPassword(), key));
        if (e.getTotpSecret() != null && !e.getTotpSecret().isEmpty()) {
            e.setTotpSecret(CryptoManager.decrypt(e.getTotpSecret(), key));
        }
        return e;
    }

    private List<VaultEntry> decryptAll(List<VaultEntry> list, SecretKey key) throws Exception {
        for (VaultEntry e : list)
            decrypt(e, key);
        return list;
    }
}
