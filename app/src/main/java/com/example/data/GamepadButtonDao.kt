package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GamepadButtonDao {
    @Query("SELECT * FROM gamepad_buttons WHERE profileId = :profileId")
    fun getButtonsByProfile(profileId: String): Flow<List<GamepadButton>>

    @Query("SELECT DISTINCT profileId FROM gamepad_buttons")
    fun getDistinctProfiles(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertButton(button: GamepadButton): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertButtons(buttons: List<GamepadButton>)

    @Delete
    suspend fun deleteButton(button: GamepadButton)

    @Query("DELETE FROM gamepad_buttons WHERE profileId = :profileId")
    suspend fun deleteProfileButtons(profileId: String)
}
