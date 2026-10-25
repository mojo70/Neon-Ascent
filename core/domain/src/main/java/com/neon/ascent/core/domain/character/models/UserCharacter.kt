package com.neon.ascent.core.domain.character.models

data class UserCharacter(
    val id: Int = 0,
    val name: String,
    val netrunnerName: String? = null,
    val sex: String,
    val dob: String,
    val units: String,
    val heightFeet: String? = null,
    val heightInches: String? = null,
    val heightCm: String? = null,
    val weight: String,
    val somatotype: Float,
    val mbti: String? = null,
    val alignment: String? = null,
    val archetype: String? = null,
    val level: Int = 1,
    val experience: Long = 0,
    val iceLevel: Int = 1,
    val eddies: Int = 0,
    val secureEddies: Int = 0,
    val hasBreachedBefore: Boolean = false,
    val isSystemDatabaseUnlocked: Boolean = false,
    val walletConnected: Boolean = false,
    val strength: Int? = null,
    val perception: Int? = null,
    val endurance: Int? = null,
    val charisma: Int? = null,
    val agility: Int? = null,
    val luck: Int? = null,
    val intelligence: Int? = null,
    val holyGhost: Int? = 0,
    val holyGhostExp: Int = 0,
    val prayerStreak: Int = 0,
    val lastPrayerDate: Long = 0L,
    val waterBaptized: Boolean = false,
    val holySpiritBaptized: Boolean = false,
    val hasTonguesAura: Boolean = false,
    val avatarPath: String? = null,
    val isCreationComplete: Boolean = false,
    val neuralLoad: Float = 0.2f,
    val chessElo: Int = 1000,
    val equippedCyberware: String? = null,
    val cyberdeckName: String? = "MILITECH_PARALINE",
    val ramSlots: Int = 8,
    val usedRam: Int = 0,
    val quickhackSlots: Int = 4,
    val loadedQuickhacks: String? = "SHORT_CIRCUIT,OVERHEAT"
) {
    fun getChessRank(): String {
        return when {
            chessElo < 800 -> "GHOST_IN_SHELL"
            chessElo < 1000 -> "SCRIPT_KIDDIE"
            chessElo < 1200 -> "DATA_MINER"
            chessElo < 1400 -> "NET_RUNNER"
            chessElo < 1600 -> "CORE_BREACHER"
            chessElo < 1800 -> "SYSTEM_ADMIN"
            chessElo < 2000 -> "NEURAL_ARCHITECT"
            chessElo < 2200 -> "DARK_WEB_MASTER"
            chessElo < 2400 -> "QUANTUM_OVERLORD"
            chessElo < 2600 -> "ARCH_NETRUNNER"
            else -> "THE_SINGULARITY"
        }
    }

    fun getChessTitle(): String {
        return when {
            chessElo < 1200 -> "UNRANKED"
            chessElo < 1400 -> "CLUB_PLAYER"
            chessElo < 1600 -> "CLASS_A"
            chessElo < 1800 -> "EXPERT"
            chessElo < 2000 -> "CM"
            chessElo < 2200 -> "M"
            chessElo < 2400 -> "SM"
            chessElo < 2600 -> "GM"
            else -> "SGM"
        }
    }

    fun getEquippedList(): List<String> = equippedCyberware?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    fun getQuickhackList(): List<String> = loadedQuickhacks?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

    fun convertUnits(targetUnit: String): UserCharacter {
        val currentIsImperial = units.equals("Imperial", ignoreCase = true)
        val targetIsImperial = targetUnit.equals("Imperial", ignoreCase = true)

        if (currentIsImperial == targetIsImperial) {
            return this.copy(units = if (targetIsImperial) "Imperial" else "Metric")
        }

        if (targetIsImperial) {
            // Converting Metric -> Imperial
            val weightKg = weight.toDoubleOrNull()
            val newWeight = if (weightKg != null && weightKg > 0) {
                val lbs = weightKg * 2.2046226218
                String.format(java.util.Locale.US, "%.1f", lbs)
            } else weight

            val cmVal = heightCm?.toDoubleOrNull()
            val (newFeet, newInches) = if (cmVal != null && cmVal > 0) {
                val totalInches = kotlin.math.round(cmVal / 2.54).toInt()
                val feet = totalInches / 12
                val inches = totalInches % 12
                feet.toString() to inches.toString()
            } else {
                (heightFeet ?: "0") to (heightInches ?: "0")
            }

            return this.copy(
                units = "Imperial",
                weight = newWeight,
                heightFeet = newFeet,
                heightInches = newInches,
                heightCm = heightCm
            )
        } else {
            // Converting Imperial -> Metric
            val weightLbs = weight.toDoubleOrNull()
            val newWeight = if (weightLbs != null && weightLbs > 0) {
                val kg = weightLbs * 0.45359237
                String.format(java.util.Locale.US, "%.1f", kg)
            } else weight

            val feet = heightFeet?.toIntOrNull() ?: 0
            val inches = heightInches?.toIntOrNull() ?: 0
            val newCm = if (feet > 0 || inches > 0) {
                val totalInches = feet * 12 + inches
                val cm = kotlin.math.round(totalInches * 2.54).toInt()
                cm.toString()
            } else {
                heightCm ?: "0"
            }

            return this.copy(
                units = "Metric",
                weight = newWeight,
                heightCm = newCm,
                heightFeet = heightFeet,
                heightInches = heightInches
            )
        }
    }

    fun formattedHeight(targetUnit: String = this.units): String {
        val targetIsImperial = targetUnit.equals("Imperial", ignoreCase = true)
        return if (targetIsImperial) {
            if (!heightFeet.isNullOrBlank() && !heightInches.isNullOrBlank()) {
                "${heightFeet}'${heightInches}\""
            } else if (!heightCm.isNullOrBlank()) {
                val cmVal = heightCm.toDoubleOrNull() ?: 0.0
                val totalInches = kotlin.math.round(cmVal / 2.54).toInt()
                val feet = totalInches / 12
                val inches = totalInches % 12
                "${feet}'${inches}\""
            } else {
                "0'0\""
            }
        } else {
            if (!heightCm.isNullOrBlank()) {
                "${heightCm} CM"
            } else if (!heightFeet.isNullOrBlank()) {
                val feet = heightFeet.toIntOrNull() ?: 0
                val inches = heightInches?.toIntOrNull() ?: 0
                val totalInches = feet * 12 + inches
                val cm = kotlin.math.round(totalInches * 2.54).toInt()
                "${cm} CM"
            } else {
                "0 CM"
            }
        }
    }

    fun formattedWeight(targetUnit: String = this.units): String {
        val charIsImperial = units.equals("Imperial", ignoreCase = true)
        val targetIsImperial = targetUnit.equals("Imperial", ignoreCase = true)
        val numericWeight = weight.toDoubleOrNull()

        return if (numericWeight == null || numericWeight <= 0) {
            if (targetIsImperial) "0 LBS" else "0 KG"
        } else if (charIsImperial == targetIsImperial) {
            val unitLabel = if (targetIsImperial) "LBS" else "KG"
            "${weight} $unitLabel"
        } else if (targetIsImperial) {
            // Character is Metric (KG), target is Imperial (LBS)
            val lbs = numericWeight * 2.2046226218
            String.format(java.util.Locale.US, "%.1f LBS", lbs)
        } else {
            // Character is Imperial (LBS), target is Metric (KG)
            val kg = numericWeight * 0.45359237
            String.format(java.util.Locale.US, "%.1f KG", kg)
        }
    }
}
