package dev.favourdevlabs.cleanthes.data.entities;

public class VaultEntry {

    private long id;
    private String title;
    private String username;
    private String encryptedPassword;
    private String website;
    private String category;
    private String notes;
    private long createdAt;
    private long updatedAt;
    private boolean isFavorite;

    // TOTP fields — null totpSecret means this entry has no TOTP configured.
    // totpSecret is AES-256-GCM encrypted before being stored, same as
    // encryptedPassword.
    // totpIssuer is plaintext (e.g. "GitHub") — not sensitive on its own.
    private String totpSecret;
    private String totpIssuer;
    private int totpDigits = 6; // 6 for virtually every real-world service
    private int totpPeriod = 30; // 30 seconds — the RFC default

    public VaultEntry() {
    }

    public VaultEntry(String title, String username, String encryptedPassword,
            String website, String category, String notes, boolean isFavorite) {
        this.title = title;
        this.username = username;
        this.encryptedPassword = encryptedPassword;
        this.website = website;
        this.category = category;
        this.notes = notes;
        this.isFavorite = isFavorite;
    }

    /** True when this entry has a TOTP secret configured. */
    public boolean hasTOTP() {
        return totpSecret != null && !totpSecret.isEmpty();
    }

    // --- Getters ---

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getUsername() {
        return username;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public String getWebsite() {
        return website;
    }

    public String getCategory() {
        return category;
    }

    public String getNotes() {
        return notes;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public String getTotpSecret() {
        return totpSecret;
    }

    public String getTotpIssuer() {
        return totpIssuer;
    }

    public int getTotpDigits() {
        return totpDigits;
    }

    public int getTotpPeriod() {
        return totpPeriod;
    }

    // --- Setters ---

    public void setId(long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public void setTotpSecret(String totpSecret) {
        this.totpSecret = totpSecret;
    }

    public void setTotpIssuer(String totpIssuer) {
        this.totpIssuer = totpIssuer;
    }

    public void setTotpDigits(int totpDigits) {
        this.totpDigits = totpDigits;
    }

    public void setTotpPeriod(int totpPeriod) {
        this.totpPeriod = totpPeriod;
    }

    @Override
    public String toString() {
        return "VaultEntry{"
                + "id=" + id
                + ", title='" + title + '\''
                + ", username='" + username + '\''
                + ", category='" + category + '\''
                + ", isFavorite=" + isFavorite
                + ", hasTOTP=" + hasTOTP()
                + ", createdAt=" + createdAt
                + '}';
    }
}
