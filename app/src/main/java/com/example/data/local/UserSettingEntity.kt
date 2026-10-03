package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettingEntity(
    @PrimaryKey
    val key: String,
    val value: String
)
