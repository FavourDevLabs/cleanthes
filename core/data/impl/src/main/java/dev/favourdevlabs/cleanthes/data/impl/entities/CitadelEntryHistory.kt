package dev.favourdevlabs.cleanthes.data.impl.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "citadel_entry_history",
    foreignKeys = [
        ForeignKey(
            entity = CitadelEntry::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["entryId"]),
        Index(value = ["timestamp"]),
    ]
)
data class CitadelEntryHistory(

    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,

    var entryId: Long = 0,

    // Snapshot of the entry's state immediately BEFORE the edit that triggered this row
    var title: String = "",
    var username: String = "",
    var encryptedPassword: String = "",
    var website: String? = null,
    var notes: String? = null,
    var totpSecret: String? = null,

    var timestamp: Long = 0,

) {
    override fun toString(): String =
        "CitadelEntryHistory{id=$id, entryId=$entryId, timestamp=$timestamp}"
}
