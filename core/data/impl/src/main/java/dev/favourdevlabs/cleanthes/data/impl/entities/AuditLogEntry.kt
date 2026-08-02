package dev.favourdevlabs.cleanthes.data.impl.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audit_log",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["eventType"]),
    ],
)
data class AuditLogEntry(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    // UNLOCK_SUCCESS | UNLOCK_FAILURE | ENTRY_VIEWED | ENTRY_CREATED | ENTRY_EDITED | ENTRY_DELETED | EXPORT
    var eventType: String = "",
    // Nullable — unlock/export events have no associated citadel entry
    var entryId: Long? = null,
    // Denormalized snapshot — survives entry rename/deletion so the log stays readable
    var entryTitle: String? = null,
    var timestamp: Long = 0,
) {
    override fun toString(): String = "AuditLogEntry{id=$id, eventType='$eventType', entryTitle=$entryTitle}"
}
