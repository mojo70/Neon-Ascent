package com.neon.ascent.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "biohacking_data")
data class BiohackingData(
    @PrimaryKey val userId: Int = 0,
    // Baselines
    val bodyFatPercentage: Float? = null,
    val energyScore: Int = 5, // 1-10
    val moodScore: Int = 5,   // 1-10
    val focusScore: Int = 5,  // 1-10
    
    // Lifestyle
    val sleepHours: Float? = null,
    val sleepQuality: Int = 5, // 1-10
    val exerciseFrequency: String? = null,
    val exerciseType: String? = null,
    val supplements: String? = null, 
    val dietType: String? = null,
    val caffeineIntake: String? = null,
    val alcoholIntake: String? = null,
    val stressLevel: Int = 5, // 1-10
    
    // Goals
    val primaryObjective: String? = null,
    val contraindications: String? = null,
    val pregnancyFlag: Boolean = false,
    
    // Sync Status
    val isWearableSynced: Boolean = false,
    val lastSyncTimestamp: Long? = null,
    val currentHeartRate: Int? = null,
    val currentSteps: Long? = null,
    
    // Genetic/Lab Data
    val labResultsPath: String? = null,
    val geneticDataPath: String? = null,
    
    // Consent & Privacy
    val hasConsentedToDataProcessing: Boolean = false,
    val consentAnonymizedUpload: Boolean = false,
    val consentWearableSync: Boolean = false,
    val consentGeneticData: Boolean = false,
    val hasCompletedPrivacyOnboarding: Boolean = false,
    val enableOnDeviceNeuralCore: Boolean = false, // Toggle for Gemini Nano
    
    // AI Report
    val latestReportJson: String? = null,
    val reportTimestamp: Long? = null,
    
    // Bio Age Results
    val calculatedBioAge: Float? = null,
    val calendarAgeAtCalculation: Int? = null,
    val extractedBiomarkersJson: String? = null
)

@Entity(tableName = "bio_protocol_logs")
data class BioProtocolLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val energyScore: Int,
    val sleepQuality: Int,
    val moodScore: Int,
    val focusScore: Int,
    val sideEffects: String? = null,
    val notes: String? = null,
    val protocolId: String, // Links to a specific AI generation
    val isWorking: Boolean? = null // null = unrated, true = working, false = not working
)
