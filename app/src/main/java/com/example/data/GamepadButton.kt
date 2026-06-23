package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gamepad_buttons")
data class GamepadButton(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val profileId: String,
    val label: String,
    val keyName: String,
    val keyCode: Int,
    val posX: Float,
    val posY: Float,
    val sizeDp: Int = 60
)
