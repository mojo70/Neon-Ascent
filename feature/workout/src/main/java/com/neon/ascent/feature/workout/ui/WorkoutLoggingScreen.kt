package com.neon.ascent.feature.workout.ui

 import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.domain.workout.models.*

@Composable
fun WorkoutLoggingScreen(
    onBack: () -> Unit,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050A0E)) // Deep Cyber Dark
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            WorkoutHeader(uiState, onBack)
            
            if (uiState.session == null) {
                ProtocolSelection(onSelect = { viewModel.startSession(it) })
            } else {
                ActiveWorkoutContent(uiState, viewModel)
            }
        }
    }
}

@Composable
fun WorkoutHeader(uiState: WorkoutUiState, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF00FFAA))
        }
        Text(
            text = uiState.session?.protocol?.name ?: "INITIALIZE SESSION",
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.Gray)
    }
}

@Composable
fun ProtocolSelection(onSelect: (WorkoutProtocol) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("SELECT PROTOCOL", color = Color(0xFF00CCFF), fontSize = 24.sp, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(32.dp))
        
        WorkoutProtocol.entries.forEach { protocol ->
            Button(
                onClick = { onSelect(protocol) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A1520)),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, if (protocol == WorkoutProtocol.CYBER_CRAPP) Color(0xFFFF0055) else Color(0xFF00CCFF))
            ) {
                Text(protocol.name, color = Color.White, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun ActiveWorkoutContent(uiState: WorkoutUiState, viewModel: WorkoutViewModel) {
    if (uiState.currentExercise == null) {
        ExercisePicker(uiState.availableExercises) { viewModel.selectExercise(it) }
    } else {
        ExerciseLogger(uiState, viewModel)
    }
}

@Composable
fun ExercisePicker(exercises: List<Exercise>, onSelect: (Exercise) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("SELECT TARGET EXERCISE", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 16.dp))
        }
        items(exercises) { exercise ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onSelect(exercise) },
                color = Color(0xFF0A1520),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, Color(0xFF00CCFF).copy(alpha = 0.3f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).background(Color.DarkGray, CircleShape))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(exercise.name, color = Color.White, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
fun ExerciseLogger(uiState: WorkoutUiState, viewModel: WorkoutViewModel) {
    val exercise = uiState.currentExercise ?: return
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Exercise Info
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF0A1520),
            border = BorderStroke(1.dp, Color(0xFF00FFAA).copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(exercise.name.uppercase(), color = Color(0xFF00FFAA), fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text(exercise.description, color = Color.LightGray, fontSize = 12.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (uiState.session?.protocol == WorkoutProtocol.CYBER_CRAPP) {
            CyberCrappLogger(uiState, viewModel)
        } else {
            StandardLogger(uiState, viewModel)
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = { /* Finish exercise */ },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFAA)),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text("COMPLETE EXERCISE", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CyberCrappLogger(uiState: WorkoutUiState, viewModel: WorkoutViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("REST-PAUSE CLUSTER", color = Color(0xFFFF0055), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        
        Row(modifier = Modifier.padding(16.dp)) {
            ClusterIndicator(1, uiState.currentClusterIndex)
            ClusterIndicator(2, uiState.currentClusterIndex)
            ClusterIndicator(3, uiState.currentClusterIndex)
        }
        
        if (uiState.isResting) {
            Text("${uiState.restTimeRemaining}s", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Black)
            Text("BREATHE", color = Color(0xFF00CCFF))
        } else {
            var reps by remember { mutableIntStateOf(0) }
            var weight by remember { mutableFloatStateOf(0f) }
            
            // Log Input
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (reps > 0) reps-- }) { Icon(Icons.Default.Remove, "", tint = Color.White) }
                Text("$reps", color = Color.White, fontSize = 32.sp)
                IconButton(onClick = { reps++ }) { Icon(Icons.Default.Add, "", tint = Color.White) }
            }
            
            Button(
                onClick = { viewModel.logSet(weight, reps) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0055))
            ) {
                Text("LOG MINI-SET")
            }
        }
    }
}

@Composable
fun ClusterIndicator(index: Int, currentIndex: Int?) {
    val active = currentIndex != null && currentIndex >= index
    Box(
        modifier = Modifier
            .size(12.dp)
            .padding(2.dp)
            .background(if (active) Color(0xFFFF0055) else Color.DarkGray, CircleShape)
    )
}

@Composable
fun StandardLogger(uiState: WorkoutUiState, viewModel: WorkoutViewModel) {
    // Placeholder for standard sets
    Text("STANDARD LOGGING MODE", color = Color.White)
}
