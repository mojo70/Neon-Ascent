package com.neon.ascent.feature.goals.ui.ascension

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.neon.ascent.core.common.NeonCyan
import com.neon.ascent.core.common.NeonPink
import com.neon.ascent.core.common.neonBorder
import com.neon.ascent.core.domain.goals.models.AscensionDirective
import com.neon.ascent.core.domain.goals.models.MentorMode
import kotlinx.coroutines.launch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.goals.models.*
import com.neon.ascent.core.domain.repository.AscensionRepository
import com.neon.ascent.feature.goals.domain.usecases.NeonMentorUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class NeuralMentorUiState(
    val directives: List<AscensionDirective> = emptyList(),
    val selectedDirective: AscensionDirective? = null,
    val mentorMode: MentorMode = MentorMode.GUIDE,
    val messages: List<MentorUiMessage> = emptyList(),
    val inputVal: String = "",
    val isGenerating: Boolean = false
)

@HiltViewModel
class NeuralMentorViewModel @Inject constructor(
    private val repository: AscensionRepository,
    private val mentorUseCase: NeonMentorUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NeuralMentorUiState())
    val uiState = _uiState.asStateFlow()

    private var missions: List<AscensionMission> = emptyList()
    private var tasks: List<AscensionTask> = emptyList()

    init {
        viewModelScope.launch {
            repository.getAllDirectives().collect { dirs ->
                _uiState.update { state ->
                    val defaultDir = state.selectedDirective ?: dirs.firstOrNull()
                    state.copy(directives = dirs, selectedDirective = defaultDir)
                }
                loadMissionsAndTasks()
            }
        }
    }

    private fun loadMissionsAndTasks() {
        val selectedDir = _uiState.value.selectedDirective ?: return
        viewModelScope.launch {
            missions = repository.getActiveMissions().first().filter { it.directiveId == selectedDir.id }
            tasks = repository.getAllRecurringTasks().first().filter { t ->
                missions.any { it.id == t.parentId } || t.parentId == selectedDir.id
            }
        }
    }

    fun selectDirective(directive: AscensionDirective) {
        _uiState.update { it.copy(selectedDirective = directive) }
        loadMissionsAndTasks()
    }

    fun selectMode(mode: MentorMode) {
        _uiState.update { it.copy(mentorMode = mode) }
    }

    fun updateInput(text: String) {
        _uiState.update { it.copy(inputVal = text) }
    }

    fun sendMessage() {
        val input = _uiState.value.inputVal
        if (input.isBlank()) return

        val directive = _uiState.value.selectedDirective
        if (directive == null) {
            _uiState.update { state ->
                state.copy(
                    messages = state.messages + MentorUiMessage("Select or create a Directive first to establish a neural anchor.", isFromUser = false)
                )
            }
            return
        }

        val mode = _uiState.value.mentorMode

        viewModelScope.launch {
            val userMsg = MentorUiMessage(input, isFromUser = true)
            _uiState.update { state ->
                state.copy(
                    messages = state.messages + userMsg,
                    inputVal = "",
                    isGenerating = true
                )
            }

            try {
                val response = mentorUseCase.getMentorDialogue(
                    directive = directive,
                    missions = missions,
                    tasks = tasks,
                    mode = mode,
                    message = input
                )
                val aiMsg = MentorUiMessage(response, isFromUser = false)
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages + aiMsg,
                        isGenerating = false
                    )
                }
            } catch (e: Exception) {
                val errorMsg = MentorUiMessage("CONNECTION_INTERRUPTED: ${e.message ?: "Unknown neural sync failure."}", isFromUser = false)
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages + errorMsg,
                        isGenerating = false
                    )
                }
            }
        }
    }

    fun injectContext(contextJson: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            val welcomePrompt = "SYSTEM_PING: The operator accessed Neural Mentor with context: $contextJson. Analyze this context and welcome them as Cyber Socrates (CYBR-TES). Let them know how you can help."
            try {
                val response = mentorUseCase.getMentorDialogue(
                    directive = _uiState.value.selectedDirective ?: AscensionDirective(id="", title="SYSTEM_CORE", description="General Optimization"),
                    missions = emptyList(),
                    tasks = emptyList(),
                    mode = MentorMode.GUIDE,
                    message = welcomePrompt
                )
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages + MentorUiMessage(response, isFromUser = false),
                        isGenerating = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeuralMentorScreen(
    initialContextJson: String? = null,
    navController: NavController,
    viewModel: NeuralMentorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var showDirectiveDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(initialContextJson) {
        if (initialContextJson != null) {
            viewModel.injectContext(initialContextJson)
        } else if (uiState.messages.isEmpty()) {
            // Initial greeting if no custom context
            viewModel.updateInput("Establish neural link.")
            viewModel.sendMessage()
        }
    }

    // Auto-scroll chat list on new messages
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            coroutineScope.launch {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "NEURAL_MENTOR // CYBR-TES",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF020508),
                    titleContentColor = NeonCyan
                )
            )
        },
        containerColor = Color(0xFF020508)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- CONTEXT ANCHORS (Directive / Mode) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Directive Selection
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = Color.Black,
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDirectiveDropdown = !showDirectiveDropdown }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uiState.selectedDirective?.title?.uppercase() ?: "SELECT ANCHOR DIRECTIVE",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Icon(
                                imageVector = if (showDirectiveDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showDirectiveDropdown,
                        onDismissRequest = { showDirectiveDropdown = false },
                        modifier = Modifier
                            .background(Color(0xFF0F141D))
                            .border(1.dp, NeonCyan, RoundedCornerShape(4.dp))
                    ) {
                        if (uiState.directives.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("NO_ACTIVE_DIRECTIVES", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                                onClick = {}
                            )
                        } else {
                            uiState.directives.forEach { dir ->
                                DropdownMenuItem(
                                    text = { Text(dir.title.uppercase(), color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                                    onClick = {
                                        viewModel.selectDirective(dir)
                                        showDirectiveDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Mentor Mode Selector
                Row(
                    modifier = Modifier
                        .border(1.dp, NeonPink.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .background(Color.Black)
                        .padding(2.dp)
                ) {
                    MentorMode.values().forEach { mode ->
                        val isSelected = uiState.mentorMode == mode
                        Box(
                            modifier = Modifier
                                .background(if (isSelected) NeonPink else Color.Transparent, RoundedCornerShape(2.dp))
                                .clickable { viewModel.selectMode(mode) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = mode.name,
                                color = if (isSelected) Color.Black else Color.LightGray,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // --- CHAT WINDOW ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f))
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    uiState.messages.forEach { msg ->
                        val alignment = if (msg.isFromUser) Alignment.End else Alignment.Start
                        val bubbleColor = if (msg.isFromUser) Color(0xFF0A121A) else Color(0xFF030D08)
                        val borderColor = if (msg.isFromUser) NeonPink.copy(alpha = 0.3f) else NeonCyan.copy(alpha = 0.3f)
                        val label = if (msg.isFromUser) "OPERATOR" else "CYBR-TES"
                        val labelColor = if (msg.isFromUser) NeonPink else NeonCyan

                        Column(
                            modifier = Modifier.align(alignment).fillMaxWidth(0.85f),
                            horizontalAlignment = if (msg.isFromUser) Alignment.End else Alignment.Start
                        ) {
                            Text(
                                text = label,
                                color = labelColor.copy(alpha = 0.8f),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .background(bubbleColor, RoundedCornerShape(4.dp))
                                    .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    if (uiState.isGenerating) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                color = NeonCyan,
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp
                            )
                            Text(
                                "CYBR-TES DECRYPTING_RESPONSE...",
                                color = NeonCyan.copy(alpha = 0.6f),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }
            }

            // --- COMMAND INPUT BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .border(1.dp, NeonCyan, RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = uiState.inputVal,
                    onValueChange = { viewModel.updateInput(it) },
                    textStyle = TextStyle(
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    ),
                    modifier = Modifier.weight(1f),
                    cursorBrush = SolidColor(NeonCyan),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        viewModel.sendMessage()
                    }),
                    decorationBox = { innerTextField ->
                        if (uiState.inputVal.isEmpty()) {
                            Text(
                                "ENTER_QUERY_FOR_CYBER_SOCRATES...",
                                color = Color.White.copy(alpha = 0.25f),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        innerTextField()
                    }
                )

                IconButton(
                    onClick = { viewModel.sendMessage() },
                    enabled = uiState.inputVal.isNotBlank() && !uiState.isGenerating,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "SEND",
                        tint = if (uiState.inputVal.isNotBlank() && !uiState.isGenerating) NeonCyan else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
