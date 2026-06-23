package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ButtonRepository
import com.example.data.GamepadButton
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class WebKeyEvent(
    val type: String, // "keydown" or "keyup"
    val key: String,
    val keyCode: Int
)

class BrowserViewModel(
    application: Application,
    private val repository: ButtonRepository
) : AndroidViewModel(application) {

    // Web navigation states
    private val _currentUrl = MutableStateFlow("")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    private val _canGoBack = MutableStateFlow(false)
    val canGoBack: StateFlow<Boolean> = _canGoBack.asStateFlow()

    private val _canGoForward = MutableStateFlow(false)
    val canGoForward = _canGoForward.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // Keyboard simulation channel
    private val _keyEventTrigger = MutableSharedFlow<WebKeyEvent>(extraBufferCapacity = 120)
    val keyEventTrigger: SharedFlow<WebKeyEvent> = _keyEventTrigger.asSharedFlow()

    // Gamepad button profiles and configuration flow
    private val _selectedProfile = MutableStateFlow("WASD + Action")
    val selectedProfile: StateFlow<String> = _selectedProfile.asStateFlow()

    // Custom profiles saved in DB + standard preset profiles
    val availableProfiles: StateFlow<List<String>> = repository.distinctProfiles
        .map { dbProfiles ->
            val presets = listOf("WASD + Action", "Retro Arcade", "Simple D-Pad")
            (presets + dbProfiles).distinct()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("WASD + Action", "Retro Arcade", "Simple D-Pad"))

    // Current buttons on screen
    private val _currentButtons = MutableStateFlow<List<GamepadButton>>(emptyList())
    val currentButtons: StateFlow<List<GamepadButton>> = _currentButtons.asStateFlow()

    // Edit Mode state
    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    // Tracks if buttons are hidden completely even in Play mode
    private val _hideButtonsEntirely = MutableStateFlow(false)
    val hideButtonsEntirely: StateFlow<Boolean> = _hideButtonsEntirely.asStateFlow()

    // Controls dialogs
    private val _editingButton = MutableStateFlow<GamepadButton?>(null)
    val editingButton: StateFlow<GamepadButton?> = _editingButton.asStateFlow()

    init {
        // Observe current profile buttons from DB or load default presets
        viewModelScope.launch {
            selectedProfile.collectLatest { profile ->
                repository.getButtonsForProfile(profile).collect { dbButtons ->
                    if (dbButtons.isEmpty()) {
                        // Load defaults
                        val defaults = when (profile) {
                            "WASD + Action" -> ButtonRepository.getDefaultWasdButtons()
                            "Retro Arcade" -> ButtonRepository.getDefaultRetroButtons()
                            "Simple D-Pad" -> ButtonRepository.getDefaultSimpleDpadButtons()
                            else -> emptyList()
                        }
                        _currentButtons.value = defaults
                    } else {
                        _currentButtons.value = dbButtons
                    }
                }
            }
        }
    }

    fun setUrl(url: String) {
        var formattedUrl = url.trim()
        if (formattedUrl.isNotEmpty()) {
            if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                formattedUrl = "https://$formattedUrl"
            }
            _currentUrl.value = formattedUrl
        } else {
            _currentUrl.value = ""
        }
    }

    fun setNavigationState(canBack: Boolean, canForward: Boolean, loading: Boolean) {
        _canGoBack.value = canBack
        _canGoForward.value = canForward
        _isLoading.value = loading
    }

    fun triggerKey(type: String, keyName: String, keyCode: Int) {
        viewModelScope.launch {
            _keyEventTrigger.emit(WebKeyEvent(type, keyName, keyCode))
        }
    }

    fun selectProfile(profile: String) {
        _selectedProfile.value = profile
    }

    fun setEditMode(active: Boolean) {
        _isEditMode.value = active
    }

    fun toggleButtonsHidden() {
        _hideButtonsEntirely.value = !_hideButtonsEntirely.value
    }

    // Save current list of buttons to DB (e.g. after dragging or editing)
    fun saveCurrentLayout() {
        viewModelScope.launch {
            val profile = _selectedProfile.value
            // Delete old layout buttons for this profile in DB first so that we replace them clean
            repository.deleteProfile(profile)
            // Strip any IDs of buttons currently held so that Room assigns new primary keys
            val cleaned = _currentButtons.value.map { it.copy(id = 0, profileId = profile) }
            repository.saveButtons(cleaned)
        }
    }

    // Update positions during dragging
    fun updateButtonPosition(buttonIndex: Int, newX: Float, newY: Float) {
        val list = _currentButtons.value.toMutableList()
        if (buttonIndex in list.indices) {
            val btn = list[buttonIndex]
            list[buttonIndex] = btn.copy(posX = newX.coerceIn(0f, 1f), posY = newY.coerceIn(0f, 1f))
            _currentButtons.value = list
        }
    }

    // Add button
    fun addNewButton() {
        val profile = _selectedProfile.value
        val newBtn = GamepadButton(
            id = 0,
            profileId = profile,
            label = "KEY",
            keyName = "KeySpace",
            keyCode = 32,
            posX = 0.5f,
            posY = 0.5f,
            sizeDp = 52
        )
        _currentButtons.value = _currentButtons.value + newBtn
    }

    // Start editing button properties
    fun startEditingButton(button: GamepadButton) {
        _editingButton.value = button
    }

    fun stopEditingButton() {
        _editingButton.value = null
    }

    // Save modified button
    fun saveEditedButton(updated: GamepadButton) {
        val list = _currentButtons.value.toMutableList()
        // If we have an matching id or index, update it
        val index = list.indexOfFirst {
            (it.id > 0 && it.id == updated.id) || 
            (it.posX == _editingButton.value?.posX && it.posY == _editingButton.value?.posY)
        }
        if (index != -1) {
            list[index] = updated
        } else {
            list.add(updated)
        }
        _currentButtons.value = list
        _editingButton.value = null
        // Auto-save changes
        saveCurrentLayout()
    }

    fun deleteButton(button: GamepadButton) {
        val list = _currentButtons.value.toMutableList()
        val index = list.indexOfFirst {
            (it.id > 0 && it.id == button.id) || 
            (it.posX == button.posX && it.posY == button.posY)
        }
        if (index != -1) {
            list.removeAt(index)
        }
        _currentButtons.value = list
        _editingButton.value = null
        // Auto-save changes
        saveCurrentLayout()
    }

    fun createNewProfile(profileName: String) {
        val name = profileName.trim()
        if (name.isNotEmpty() && name !in listOf("WASD + Action", "Retro Arcade", "Simple D-Pad")) {
            viewModelScope.launch {
                // Pre-populate new profile with simple buttons
                val baseButtons = ButtonRepository.getDefaultSimpleDpadButtons().map {
                    it.copy(id = 0, profileId = name)
                }
                repository.saveButtons(baseButtons)
                _selectedProfile.value = name
            }
        }
    }

    // Reset current profile to defaults
    fun resetCurrentProfileToDefault() {
        viewModelScope.launch {
            val profile = _selectedProfile.value
            repository.deleteProfile(profile)
            val defaults = when (profile) {
                "WASD + Action" -> ButtonRepository.getDefaultWasdButtons()
                "Retro Arcade" -> ButtonRepository.getDefaultRetroButtons()
                "Simple D-Pad" -> ButtonRepository.getDefaultSimpleDpadButtons()
                else -> ButtonRepository.getDefaultSimpleDpadButtons().map { it.copy(profileId = profile) }
            }
            _currentButtons.value = defaults
            saveCurrentLayout()
        }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val database = AppDatabase.getDatabase(application)
                    val repository = ButtonRepository(database.gamepadButtonDao())
                    return BrowserViewModel(application, repository) as T
                }
            }
    }
}
