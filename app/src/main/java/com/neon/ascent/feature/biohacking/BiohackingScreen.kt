package com.neon.ascent.feature.biohacking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.feature.charactercreation.CyberButtonShape
import com.neon.ascent.feature.charactercreation.CyberFrame
import com.neon.ascent.feature.charactercreation.CyberGridBackground
import com.neon.ascent.feature.dashboard.PixelatedSilhouette

@Composable
fun BiohackingScreen(onBack: () -> Unit) {
    var selectedSector by remember { mutableStateOf("TOTAL_SYNC") }
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        CyberGridBackground()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            Text(
                "BIOHACKING_INTERFACE // V.2.1",
                color = Color(0xFF00FFFF),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Zoomed Avatar Selector
            CyberFrame(label = "HOLOGRAPHIC_SELECTOR") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PixelatedSilhouette(modifier = Modifier.fillMaxSize(0.8f))
                    
                    // Interactive Sectors
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f).fillMaxWidth().clickable { selectedSector = "CRANIAL_NODE" })
                        Box(Modifier.weight(2f).fillMaxWidth().clickable { selectedSector = "CORE_CHASSIS" })
                        Box(Modifier.weight(2f).fillMaxWidth().clickable { selectedSector = "MOTOR_EXTREMITIES" })
                    }
                }
            }

            Text(
                "SELECTED_SECTOR: $selectedSector",
                color = Color(0xFF00FFFF),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Bio-Data Intake
            CyberFrame(label = "BIOMETRIC_INTAKE") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BioIntakeField("GENETIC_DATA_UPLOAD", "FILE_STATUS: NOT_LINKED")
                    BioIntakeField("BLOOD_TYPE", "O-POSITIVE")
                    BioIntakeField("LAB_RESULTS", "UPLOAD_PENDING")
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BioSmallSelect("RACE", Modifier.weight(1f))
                        BioSmallSelect("EYE_COLOR", Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BioSmallSelect("HAIR_COLOR", Modifier.weight(1f))
                        BioSmallSelect("SKIN_TONE", Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { /* Generate Tips */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(CyberButtonShape)
                    .border(1.dp, Color(0xFF00FFFF), CyberButtonShape),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A0A0A))
            ) {
                Text("GENERATE AI BIO-HACKS", color = Color(0xFF00FFFF), fontWeight = FontWeight.Black)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Text("RETURN_TO_HUB", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun BioIntakeField(label: String, status: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF00FFFF).copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .background(Color(0xFF050505))
            .padding(12.dp)
    ) {
        Column {
            Text(label, color = Color.Gray, fontSize = 10.sp)
            Text(status, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BioSmallSelect(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .border(1.dp, Color(0xFF00FFFF).copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .background(Color(0xFF050505))
            .padding(8.dp)
    ) {
        Column {
            Text(label, color = Color.Gray, fontSize = 8.sp)
            Text("SELECT...", color = Color(0xFF00FFFF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}
