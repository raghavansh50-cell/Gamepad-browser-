package com.example.data

import kotlinx.coroutines.flow.Flow

class ButtonRepository(private val dao: GamepadButtonDao) {

    fun getButtonsForProfile(profileId: String): Flow<List<GamepadButton>> =
        dao.getButtonsByProfile(profileId)

    val distinctProfiles: Flow<List<String>> = dao.getDistinctProfiles()

    suspend fun saveButton(button: GamepadButton): Long =
        dao.insertButton(button)

    suspend fun saveButtons(buttons: List<GamepadButton>) =
        dao.insertButtons(buttons)

    suspend fun deleteButton(button: GamepadButton) =
        dao.deleteButton(button)

    suspend fun deleteProfile(profileId: String) =
        dao.deleteProfileButtons(profileId)

    // Preloaded default layouts
    companion object {
        fun getDefaultWasdButtons(): List<GamepadButton> {
            return listOf(
                // Directional Keys (Left side)
                GamepadButton(profileId = "WASD + Action", label = "W", keyName = "KeyW", keyCode = 87, posX = 0.15f, posY = 0.65f, sizeDp = 50),
                GamepadButton(profileId = "WASD + Action", label = "A", keyName = "KeyA", keyCode = 65, posX = 0.07f, posY = 0.77f, sizeDp = 50),
                GamepadButton(profileId = "WASD + Action", label = "S", keyName = "KeyS", keyCode = 83, posX = 0.15f, posY = 0.77f, sizeDp = 50),
                GamepadButton(profileId = "WASD + Action", label = "D", keyName = "KeyD", keyCode = 68, posX = 0.23f, posY = 0.77f, sizeDp = 50),

                // Utility keys (Center/Top)
                GamepadButton(profileId = "WASD + Action", label = "ESC", keyName = "Escape", keyCode = 27, posX = 0.05f, posY = 0.15f, sizeDp = 42),
                GamepadButton(profileId = "WASD + Action", label = "ENT", keyName = "Enter", keyCode = 13, posX = 0.50f, posY = 0.15f, sizeDp = 42),

                // Right action buttons
                GamepadButton(profileId = "WASD + Action", label = "SPACE", keyName = "Space", keyCode = 32, posX = 0.85f, posY = 0.77f, sizeDp = 70),
                GamepadButton(profileId = "WASD + Action", label = "Shift", keyName = "ShiftLeft", keyCode = 16, posX = 0.73f, posY = 0.80f, sizeDp = 50),
                GamepadButton(profileId = "WASD + Action", label = "E", keyName = "KeyE", keyCode = 69, posX = 0.83f, posY = 0.62f, sizeDp = 50),
                GamepadButton(profileId = "WASD + Action", label = "Q", keyName = "KeyQ", keyCode = 81, posX = 0.73f, posY = 0.66f, sizeDp = 50)
            )
        }

        fun getDefaultRetroButtons(): List<GamepadButton> {
            return listOf(
                // Arrows (Left side D-pad arrangement)
                GamepadButton(profileId = "Retro Arcade", label = "▲", keyName = "ArrowUp", keyCode = 38, posX = 0.15f, posY = 0.65f, sizeDp = 50),
                GamepadButton(profileId = "Retro Arcade", label = "◀", keyName = "ArrowLeft", keyCode = 37, posX = 0.07f, posY = 0.77f, sizeDp = 50),
                GamepadButton(profileId = "Retro Arcade", label = "▼", keyName = "ArrowDown", keyCode = 40, posX = 0.15f, posY = 0.77f, sizeDp = 50),
                GamepadButton(profileId = "Retro Arcade", label = "▶", keyName = "ArrowRight", keyCode = 39, posX = 0.23f, posY = 0.77f, sizeDp = 50),

                // Center System Buttons
                GamepadButton(profileId = "Retro Arcade", label = "ESC", keyName = "Escape", keyCode = 27, posX = 0.05f, posY = 0.15f, sizeDp = 42),
                GamepadButton(profileId = "Retro Arcade", label = "SELECT", keyName = "ShiftRight", keyCode = 16, posX = 0.42f, posY = 0.84f, sizeDp = 45),
                GamepadButton(profileId = "Retro Arcade", label = "START", keyName = "Enter", keyCode = 13, posX = 0.53f, posY = 0.84f, sizeDp = 45),

                // Right action buttons (A, B, X, Y equivalent styled)
                GamepadButton(profileId = "Retro Arcade", label = "Z", keyName = "KeyZ", keyCode = 90, posX = 0.78f, posY = 0.78f, sizeDp = 56),
                GamepadButton(profileId = "Retro Arcade", label = "X", keyName = "KeyX", keyCode = 88, posX = 0.88f, posY = 0.68f, sizeDp = 56)
            )
        }

        fun getDefaultSimpleDpadButtons(): List<GamepadButton> {
            return listOf(
                // Minimal directions and One Jump key
                GamepadButton(profileId = "Simple D-Pad", label = "▲", keyName = "ArrowUp", keyCode = 38, posX = 0.15f, posY = 0.65f, sizeDp = 48),
                GamepadButton(profileId = "Simple D-Pad", label = "◀", keyName = "ArrowLeft", keyCode = 37, posX = 0.07f, posY = 0.75f, sizeDp = 48),
                GamepadButton(profileId = "Simple D-Pad", label = "▼", keyName = "ArrowDown", keyCode = 40, posX = 0.15f, posY = 0.75f, sizeDp = 48),
                GamepadButton(profileId = "Simple D-Pad", label = "▶", keyName = "ArrowRight", keyCode = 39, posX = 0.23f, posY = 0.75f, sizeDp = 48),
                GamepadButton(profileId = "Simple D-Pad", label = "JUMP", keyName = "Space", keyCode = 32, posX = 0.85f, posY = 0.72f, sizeDp = 64)
            )
        }
    }
}
