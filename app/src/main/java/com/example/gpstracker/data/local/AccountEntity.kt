package com.example.gpstracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A local account. The password itself is never stored, only a salted, slow hash of it
 * (see PasswordHasher), so a copied database does not reveal passwords.
 *
 * @property email lower-case, used as the key
 */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val email: String,
    val name: String,
    val passwordHash: String,
    val salt: String,
    val createdAtMs: Long,
)
