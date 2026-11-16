package com.ghdinteractivestudio.jobhunter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ghdinteractivestudio.jobhunter.domain.model.UserProfile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val profileJson: String,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): UserProfile = runCatching {
        Json.decodeFromString<UserProfile>(profileJson)
    }.getOrDefault(UserProfile())

    companion object {
        fun fromDomain(profile: UserProfile): UserProfileEntity = UserProfileEntity(
            id = 1,
            profileJson = Json.encodeToString(profile),
            updatedAt = System.currentTimeMillis()
        )
    }
}
