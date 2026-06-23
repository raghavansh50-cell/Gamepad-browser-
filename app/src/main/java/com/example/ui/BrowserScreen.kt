package com.example.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.HapticFeedbackConstants
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.GamepadButton
import kotlinx.coroutines.flow.collect
import kotlin.math.roundToInt

// Preset keys metadata for mapping selection
data class CommonKey(val displayName: String, val keyName: String, val keyCode: Int)

val PRESET_GAME_KEYS = listOf(
    CommonKey("SPACE (Jump/Interact)", "Space", 32),
    CommonKey("ENTER (Start)", "Enter", 13),
    CommonKey("ESC (Pause/Menu)", "Escape", 27),
    CommonKey("Arrow UP (▲)", "ArrowUp", 38),
    CommonKey("Arrow DOWN (▼)", "ArrowDown", 40),
    CommonKey("Arrow LEFT (◀)", "ArrowLeft", 37),
    CommonKey("Arrow RIGHT (▶)", "ArrowRight", 39),
    CommonKey("W (Move UP)", "KeyW", 87),
    CommonKey("A (Move LEFT)", "KeyA", 65),
    CommonKey("S (Move DOWN)", "KeyS", 83),
    CommonKey("D (Move RIGHT)", "KeyD", 68),
    CommonKey("Z (Action A)", "KeyZ", 90),
    CommonKey("X (Action B)", "KeyX", 88),
    CommonKey("C (Action C)", "KeyC", 67),
    CommonKey("Shift", "ShiftLeft", 16),
    CommonKey("Ctrl", "ControlLeft", 17),
    CommonKey("Tab", "Tab", 9),
    CommonKey("Q Key", "KeyQ", 81),
    CommonKey("E Key", "KeyE", 69),
    CommonKey("R Key", "KeyR", 82),
    CommonKey("F Key", "KeyF", 70),
    CommonKey("Number 1", "Digit1", 49),
    CommonKey("Number 2", "Digit2", 50),
    CommonKey("Number 3", "Digit3", 51),
    CommonKey("Number 4", "Digit4", 52),
    CommonKey("Backspace", "Backspace", 8)
)

// Preloaded beautiful retro bookmarks
data class WebBookmark(val title: String, val url: String, val iconName: String, val desc: String)

val GAME_BOOKMARKS = listOf(
    WebBookmark("Poki Games", "https://poki.com", "🎮", "Hundreds of mobile-optimized casual games"),
    WebBookmark("Play Classic", "https://playclassic.games", "👾", "Retro retro PC, DOS, & game console emulators in-browser"),
    WebBookmark("CrazyGames", "https://www.crazygames.com", "🔥", "Exciting modern 3D & 2D web action games"),
    WebBookmark("RetroGames", "https://www.retrogames.cz", "🕹️", "Play thousands of classic 8-bit & 16-bit arcade games"),
    WebBookmark("MiniClip", "https://www.miniclip.com", "🎯", "Legendary physics, sports and party web games")
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val currentUrl by viewModel.currentUrl.collectAsStateWithLifecycle()
    val canGoBack by viewModel.canGoBack.collectAsStateWithLifecycle()
    val canGoForward by viewModel.canGoForward.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val currentButtons by viewModel.currentButtons.collectAsStateWithLifecycle()
    val isEditMode by viewModel.isEditMode.collectAsStateWithLifecycle()
    val hideButtonsEntirely by viewModel.hideButtonsEntirely.collectAsStateWithLifecycle()
    val editingButton by viewModel.editingButton.collectAsStateWithLifecycle()
    val availableProfiles by viewModel.availableProfiles.collectAsStateWithLifecycle()
    val selectedProfile by viewModel.selectedProfile.collectAsStateWithLifecycle()

    var urlInputText by remember { mutableStateOf("") }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var showProfilesDropdown by remember { mutableStateOf(false) }
    var showProfileCreatorDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }

    // Let the URL input stay in sync with ViewModel's state
    LaunchedEffect(currentUrl) {
        urlInputText = currentUrl
    }

    // Scaffold contains the main screen with elegant browser controls
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column {
                // Browser URL toolbar & Navigation
                Surface(
                    tonalElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    modifier = Modifier.shadow(4.dp)
                ) {
                    Column(modifier = Modifier.statusBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            // Back button
                            IconButton(
                                onClick = { webViewInstance?.goBack() },
                                enabled = canGoBack,
                                modifier = Modifier.testTag("nav_back_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }

                            // Forward button
                            IconButton(
                                onClick = { webViewInstance?.goForward() },
                                enabled = canGoForward,
                                modifier = Modifier.testTag("nav_forward_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward")
                            }

                            // Reload/Home button (dynamic)
                            if (currentUrl.isNotEmpty()) {
                                IconButton(onClick = { webViewInstance?.reload() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reload")
                                }
                            } else {
                                IconButton(onClick = { viewModel.setUrl("https://google.com") }) {
                                    Icon(Icons.Default.Search, contentDescription = "Search")
                                }
                            }

                            // URL Address Field
                            TextField(
                                value = urlInputText,
                                onValueChange = { urlInputText = it },
                                placeholder = { Text("Search or type URL...", fontSize = 14.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(24.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                ),
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Go
                                ),
                                keyboardActions = KeyboardActions(
                                    onGo = {
                                        viewModel.setUrl(urlInputText)
                                    }
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .padding(horizontal = 4.dp)
                                    .testTag("url_input_field")
                            )

                            // Clear / Home Icon button
                            if (currentUrl.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setUrl("") }) {
                                    Icon(Icons.Default.Home, contentDescription = "Go Home")
                                }
                            }

                            // Controller/Overlay Quick Menu Trigger
                            Box {
                                IconButton(
                                    onClick = { showProfilesDropdown = true },
                                    modifier = Modifier.testTag("profiles_menu_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Games,
                                        contentDescription = "Controls Manager",
                                        tint = if (isEditMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }

                                DropdownMenu(
                                    expanded = showProfilesDropdown,
                                    onDismissRequest = { showProfilesDropdown = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Edit Controls Mode", fontWeight = FontWeight.Bold) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (isEditMode) Icons.Default.CheckCircle else Icons.Default.Edit,
                                                contentDescription = null,
                                                tint = if (isEditMode) Color.Green else LocalContentColor.current
                                            )
                                        },
                                        onClick = {
                                            viewModel.setEditMode(!isEditMode)
                                            showProfilesDropdown = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Show/Hide Key Overlays") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (hideButtonsEntirely) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = null
                                            )
                                        },
                                        onClick = {
                                            viewModel.toggleButtonsHidden()
                                            showProfilesDropdown = false
                                        }
                                    )
                                    HorizontalDivider()

                                    // Profiles Section Header
                                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                        Text(
                                            text = "Gamepad Profiles",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }

                                    // List available controller profiles
                                    availableProfiles.forEach { profile ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = profile,
                                                    fontWeight = if (profile == selectedProfile) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (profile == selectedProfile) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = if (profile == selectedProfile) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                                    contentDescription = null
                                                )
                                            },
                                            onClick = {
                                                viewModel.selectProfile(profile)
                                                showProfilesDropdown = false
                                            }
                                        )
                                    }

                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("New Custom Profile") },
                                        leadingIcon = { Icon(Icons.Default.AddCircle, contentDescription = null) },
                                        onClick = {
                                            newProfileName = ""
                                            showProfileCreatorDialog = true
                                            showProfilesDropdown = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Reset Current Profile") },
                                        leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                        onClick = {
                                            viewModel.resetCurrentProfileToDefault()
                                            showProfilesDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // Loader progress
                        if (isLoading) {
                            LinearProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth().height(3.dp).padding(top = 2.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(3.dp))
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (currentUrl.isEmpty()) {
                // RENDER WELCOME / START SCREEN IF URL IS EMPTY
                WelcomeStartDashboard(
                    selectedProfile = selectedProfile,
                    availableProfiles = availableProfiles,
                    onSelectProfile = { viewModel.selectProfile(it) },
                    onNavigateToUrl = { url -> viewModel.setUrl(url) },
                    onCreateNewProfile = { name -> viewModel.createNewProfile(name) }
                )
            } else {
                // RENDER ACTIVE BROWSER VIEWPORT WITH ON-SCREEN OVERLAYS
                GamepadBrowserViewport(
                    viewModel = viewModel,
                    isEditMode = isEditMode,
                    hideButtonsEntirely = hideButtonsEntirely,
                    currentButtons = currentButtons,
                    onInitWebView = { webViewInstance = it }
                )
            }
        }
    }

    // Setup Custom Profile Builder Dialog
    if (showProfileCreatorDialog) {
        AlertDialog(
            onDismissRequest = { showProfileCreatorDialog = false },
            title = { Text("Create Game Profile") },
            text = {
                Column {
                    Text(
                        "Design your own button mapping key profile! Enter web game model layout name:",
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = newProfileName,
                        onValueChange = { newProfileName = it },
                        label = { Text("Profile Name (e.g. Doom Co-op)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newProfileName.isNotEmpty()) {
                            viewModel.createNewProfile(newProfileName)
                        }
                        showProfileCreatorDialog = false
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileCreatorDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Setup Single Overlay Button Editor Dialog (Modify keys in Edit Mode)
    if (editingButton != null) {
        val buttonToEdit = editingButton!!
        var tempLabel by remember(buttonToEdit) { mutableStateOf(buttonToEdit.label) }
        var tempKeyName by remember(buttonToEdit) { mutableStateOf(buttonToEdit.keyName) }
        var tempKeyCode by remember(buttonToEdit) { mutableStateOf(buttonToEdit.keyCode) }
        var tempSize by remember(buttonToEdit) { mutableStateOf(buttonToEdit.sizeDp.toFloat()) }

        AlertDialog(
            onDismissRequest = { viewModel.stopEditingButton() },
            title = { Text("Configure Keyboard Key Button") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Display current properties
                    OutlinedTextField(
                        value = tempLabel,
                        onValueChange = { tempLabel = it },
                        label = { Text("Button Label Text / Icon") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )

                    // Keyboard key binding search helper
                    Text(
                        text = "Map to PC keyboard key:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // Quick buttons list for fast binding allocation
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(PRESET_GAME_KEYS) { preset ->
                            SuggestionChip(
                                onClick = {
                                    tempKeyName = preset.keyName
                                    tempKeyCode = preset.keyCode
                                    if (tempLabel == "KEY" || tempLabel.isEmpty() || tempLabel == buttonToEdit.label) {
                                        // Auto update label to match
                                        tempLabel = preset.displayName.substringBefore(" ").take(5)
                                    }
                                },
                                label = { Text(preset.displayName.substringBefore(" ")) }
                            )
                        }
                    }

                    // Key code details fields
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = tempKeyName,
                            onValueChange = { tempKeyName = it },
                            label = { Text("Key code string") },
                            readOnly = false,
                            modifier = Modifier.weight(1.3f)
                        )
                        OutlinedTextField(
                            value = tempKeyCode.toString(),
                            onValueChange = { newValue ->
                                tempKeyCode = newValue.toIntOrNull() ?: tempKeyCode
                            },
                            label = { Text("KeyCode Int") },
                            modifier = Modifier.weight(0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Size Slider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Size (${tempSize.toInt()}dp): ",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(90.dp)
                        )
                        Slider(
                            value = tempSize,
                            onValueChange = { tempSize = it },
                            valueRange = 36f..96f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = buttonToEdit.copy(
                            label = tempLabel,
                            keyName = tempKeyName,
                            keyCode = tempKeyCode,
                            sizeDp = tempSize.toInt()
                        )
                        viewModel.saveEditedButton(updated)
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            viewModel.deleteButton(buttonToEdit)
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete Button")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { viewModel.stopEditingButton() }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

@Composable
fun WelcomeStartDashboard(
    selectedProfile: String,
    availableProfiles: List<String>,
    onSelectProfile: (String) -> Unit,
    onNavigateToUrl: (String) -> Unit,
    onCreateNewProfile: (String) -> Unit
) {
    var searchInputText by remember { mutableStateOf("") }
    var showQuickAddProfile by remember { mutableStateOf(false) }
    var quickProfileName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Area
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .shadow(2.dp, CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Games,
                contentDescription = "Gamepad Icon",
                modifier = Modifier.size(60.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Gamepad Web Browser",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Custom responsive on-screen buttons for PC retro and emulator web games. Drag, resize, and assign keys!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Search bar
        OutlinedTextField(
            value = searchInputText,
            onValueChange = { searchInputText = it },
            placeholder = { Text("Search classic web games or enter address...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                if (searchInputText.isNotEmpty()) {
                    var target = searchInputText.trim()
                    if (target.contains(".") && !target.contains(" ")) {
                        onNavigateToUrl(target)
                    } else {
                        onNavigateToUrl("https://www.google.com/search?q=${target.replace(" ", "+")}")
                    }
                }
            }),
            modifier = Modifier.fillMaxWidth().testTag("welcome_search_bar")
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Bookmarks Grid Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Popular PC Gaming Sites",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bookmark Cards
        GAME_BOOKMARKS.forEach { bookmark ->
            Card(
                onClick = { onNavigateToUrl(bookmark.url) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .shadow(1.dp, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = bookmark.iconName,
                        fontSize = 28.sp,
                        modifier = Modifier.padding(end = 14.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = bookmark.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = bookmark.desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Navigate to web game"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Profiles header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.SportsEsports, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Key Control Profile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick profile chips row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(availableProfiles) { profile ->
                val isSelected = profile == selectedProfile
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectProfile(profile) },
                    label = { Text(profile) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
        Text(
            text = "Tip: When gaming, click the Controller icon in the browser header to enter 'Edit Controls Mode' which allows you to move key inputs, add keys, or scale them to fit your layout.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        )
    }
}


@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GamepadBrowserViewport(
    viewModel: BrowserViewModel,
    isEditMode: Boolean,
    hideButtonsEntirely: Boolean,
    currentButtons: List<GamepadButton>,
    onInitWebView: (WebView) -> Unit
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val currentUrl by viewModel.currentUrl.collectAsStateWithLifecycle()
    val keyEventTrigger = viewModel.keyEventTrigger
    val context = LocalContext.current

    // Observe and inject evaluated javascript events
    LaunchedEffect(keyEventTrigger) {
        keyEventTrigger.collect { event ->
            val codeStr = event.key
            // Build JS-friendly target key mapping
            val keyStr = event.key
                .replace("Key", "")
                .replace("Digit", "")
                .replace("ArrowUp", "ArrowUp")
                .replace("ArrowDown", "ArrowDown")
                .replace("ArrowLeft", "ArrowLeft")
                .replace("ArrowRight", "ArrowRight")
                .replace("Space", " ")
                .let { if (it.length == 1 && it[0].isLetter()) it.lowercase() else it }

            val js = """
                (function() {
                    var target = document.activeElement || document.body || document;
                    var eventOpts = {
                        key: '$keyStr',
                        code: '$codeStr',
                        keyCode: ${event.keyCode},
                        which: ${event.keyCode},
                        bubbles: true,
                        cancelable: true,
                        view: window
                    };
                    var ev = new KeyboardEvent('${event.type}', eventOpts);
                    target.dispatchEvent(ev);
                    document.dispatchEvent(ev);
                    window.dispatchEvent(ev);
                })();
            """.trimIndent()
            webViewRef?.evaluateJavascript(js, null)
        }
    }

    // Capture parent screen dimensions in pixels for accurate drag percentages
    var areaWidthPx by remember { mutableStateOf(1) }
    var areaHeightPx by remember { mutableStateOf(1) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                areaWidthPx = coordinates.size.width
                areaHeightPx = coordinates.size.height
            }
            .testTag("webview_and_overlays_container")
    ) {
        // 1. Android Native WebView
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            viewModel.setNavigationState(
                                canBack = view?.canGoBack() == true,
                                canForward = view?.canGoForward() == true,
                                loading = true
                            )
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            viewModel.setNavigationState(
                                canBack = view?.canGoBack() == true,
                                canForward = view?.canGoForward() == true,
                                loading = false
                            )
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            return false // load in webview itself
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            viewModel.setNavigationState(
                                canBack = view?.canGoBack() == true,
                                canForward = view?.canGoForward() == true,
                                loading = newProgress < 100
                            )
                        }
                    }

                    // Enable responsive full gaming capabilities
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        builtInZoomControls = true
                        displayZoomControls = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }

                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                    onInitWebView(this)
                    webViewRef = this
                    loadUrl(currentUrl)
                }
            },
            update = { webView ->
                // Ensure WebView address matches when VM changes the URL
                if (webView.url != currentUrl && currentUrl.isNotEmpty()) {
                    webView.loadUrl(currentUrl)
                }
            },
            modifier = Modifier.fillMaxSize().testTag("android_webview")
        )

        // 2. Buttons overlay (Only when not hidden)
        if (!hideButtonsEntirely) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val maxW = maxWidth
                val maxH = maxHeight

                currentButtons.forEachIndexed { index, btn ->
                    GamepadFloatingButton(
                        button = btn,
                        index = index,
                        isEditMode = isEditMode,
                        parentWidthPx = areaWidthPx,
                        parentHeightPx = areaHeightPx,
                        maxW = maxW,
                        maxH = maxH,
                        onDrag = { newX, newY ->
                            viewModel.updateButtonPosition(index, newX, newY)
                        },
                        onEdit = {
                            viewModel.startEditingButton(btn)
                        },
                        onPress = {
                            viewModel.triggerKey("keydown", btn.keyName, btn.keyCode)
                        },
                        onRelease = {
                            viewModel.triggerKey("keyup", btn.keyName, btn.keyCode)
                        }
                    )
                }
            }
        }

        // 3. Edit mode overlay drawer control
        AnimatedVisibility(
            visible = isEditMode,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                tonalElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().shadow(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "EDIT BUTTON LAYOUT",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Drag keys anywhere. Tap a key to configure / map.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Quick Add Button
                        FilledTonalButton(
                            onClick = { viewModel.addNewButton() }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Key")
                        }

                        // Close/Quit Mode Button
                        Button(
                            onClick = {
                                viewModel.saveCurrentLayout()
                                viewModel.setEditMode(false)
                            }
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Done")
                        }
                    }
                }
            }
        }

        // 4. Compact Floating bubble to toggle Edit controls when NOT in Edit mode (Saves screen space!)
        if (!isEditMode && !hideButtonsEntirely) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    .clickable { viewModel.setEditMode(true) }
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Layout Quick Bubble",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun GamepadFloatingButton(
    button: GamepadButton,
    index: Int,
    isEditMode: Boolean,
    parentWidthPx: Int,
    parentHeightPx: Int,
    maxW: androidx.compose.ui.unit.Dp,
    maxH: androidx.compose.ui.unit.Dp,
    onDrag: (Float, Float) -> Unit,
    onEdit: () -> Unit,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    // Convert parent boundaries to coordinates
    val widthDp = button.sizeDp.dp

    var relativeX by remember(button.posX) { mutableStateOf(button.posX) }
    var relativeY by remember(button.posY) { mutableStateOf(button.posY) }

    // Let them visually shift with compose dragging
    var isPressedState by remember { mutableStateOf(false) }

    // Re-resolve positions when database model updates
    LaunchedEffect(button.posX, button.posY) {
        relativeX = button.posX
        relativeY = button.posY
    }

    // Math location for placing buttons safely
    val offsetX = (relativeX * maxW.value).dp - (widthDp / 2)
    val offsetY = (relativeY * maxH.value).dp - (widthDp / 2)

    val finalX = offsetX.coerceAtLeast(0.dp)
    val finalY = offsetY.coerceAtLeast(0.dp)

    // Beautifully scoped single key bubble
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .offset(x = finalX, y = finalY)
            .size(widthDp)
            .shadow(
                elevation = if (isPressedState) 1.dp else 4.dp,
                shape = CircleShape
            )
            .border(
                width = 2.dp,
                color = when {
                    isPressedState -> MaterialTheme.colorScheme.primary
                    isEditMode -> MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    else -> Color.White.copy(alpha = 0.5f)
                },
                shape = CircleShape
            )
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = when {
                        isPressedState -> listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                        isEditMode -> listOf(
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                        )
                        else -> listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.75f)
                        )
                    }
                )
            )
            .clickable(
                enabled = isEditMode,
                onClick = onEdit
            )
            .pointerInput(button.id, isEditMode) {
                if (isEditMode) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val deltaXPercent = dragAmount.x / parentWidthPx
                            val deltaYPercent = dragAmount.y / parentHeightPx
                            relativeX = (relativeX + deltaXPercent).coerceIn(0f, 1f)
                            relativeY = (relativeY + deltaYPercent).coerceIn(0f, 1f)
                            onDrag(relativeX, relativeY)
                        }
                    )
                } else {
                    // Dedicated multi-touch scope specifically matching the exact button area bounds
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown()
                            isPressedState = true
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onPress()

                            var fingerUp = false
                            while (!fingerUp) {
                                val event = awaitPointerEvent()
                                val anyPres = event.changes.any { it.pressed }
                                if (!anyPres) {
                                    fingerUp = true
                                    isPressedState = false
                                    onRelease()
                                }
                            }
                        }
                    }
                }
            }
            .testTag("gamepad_key_${button.label}")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp)
        ) {
            Text(
                text = button.label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (button.label.length > 4) 10.sp else 14.sp
                ),
                color = if (isPressedState) MaterialTheme.colorScheme.onPrimary else Color.White,
                textAlign = TextAlign.Center
            )
            if (isEditMode) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Move indicator",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}
