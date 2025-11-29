package com.neon.ascent.feature.charactercreation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neon.ascent.ui.theme.NeonAscentTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterCreationScreen() {
    var name by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("") } // "Male", "Female"
    var dob by remember { mutableStateOf("") }
    var units by remember { mutableStateOf("Imperial") } // "Imperial", "Metric"
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var somatotype by remember { mutableStateOf(5f) } // 0-10 slider: Ecto to Endo

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Forge Your Neon Ascent", style = MaterialTheme.typography.headlineLarge)

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )

        // Sex Selection (Radio buttons)
        Row {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = sex == "Male", onClick = { sex = "Male" })
                Text("Male")
            }
            Spacer(Modifier.width(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = sex == "Female", onClick = { sex = "Female" })
                Text("Female")
            }
        }

        OutlinedTextField(
            value = dob,
            onValueChange = { dob = it },
            label = { Text("Date of Birth (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth()
        )

        // Units Toggle
        Row {
            Text("Units: ")
            Row {
                RadioButton(selected = units == "Imperial", onClick = { units = "Imperial" })
                Text("Inches/lbs")
            }
            Spacer(Modifier.width(8.dp))
            Row {
                RadioButton(selected = units == "Metric", onClick = { units = "Metric" })
                Text("cm/kg")
            }
        }

        OutlinedTextField(
            value = height,
            onValueChange = { height = it },
            label = { Text("Height (${if (units == "Imperial") "in" else "cm"})") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = weight,
            onValueChange = { weight = it },
            label = { Text("Weight (${if (units == "Imperial") "lbs" else "kg"})") },
            modifier = Modifier.fillMaxWidth()
        )

        // Somatotype Slider
        Text("Body Type (Ectomorph ← → Endomorph)")
        Slider(
            value = somatotype,
            onValueChange = { somatotype = it },
            valueRange = 0f..10f,
            modifier = Modifier.fillMaxWidth()
        )
        // TODO: Add silhouettes based on value

        Button(onClick = { /* Save to DB, next screen */ }) {
            Text("Next: Personality Quiz")
        }
    }
}
