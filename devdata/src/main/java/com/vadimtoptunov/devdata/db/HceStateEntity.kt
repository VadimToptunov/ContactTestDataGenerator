package com.vadimtoptunov.devdata.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row table that holds the currently-active identity for HCE emulation.
 * Always has exactly one row with id = "singleton".
 */
@Entity(tableName = "hce_state")
data class HceStateEntity(
    @PrimaryKey val id: String = "singleton",
    val activeIdentityId: String? = null
)
