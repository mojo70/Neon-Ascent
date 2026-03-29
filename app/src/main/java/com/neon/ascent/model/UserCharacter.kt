package com.neon.ascent.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_character")
data class UserCharacter(
    @PrimaryKey val id: Int = 0,
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
    val hasBreachedBefore: Boolean = false,
    val walletConnected: Boolean = false,
    val strength: Int? = null,
    val perception: Int? = null,
    val endurance: Int? = null,
    val charisma: Int? = null,
    val agility: Int? = null,
    val luck: Int? = null,
    val holyGhost: Int? = null,
    val avatarPath: String? = null,
    val isCreationComplete: Boolean = false,
    val neuralLoad: Float = 0.2f, // Default starting load
    val chessElo: Int = 1000 // Starting Elo
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
            chessElo < 2000 -> "CM" // Candidate Master
            chessElo < 2200 -> "M"  // Master
            chessElo < 2400 -> "SM" // Senior Master
            chessElo < 2600 -> "GM" // Grandmaster
            else -> "SGM" // Super Grandmaster
        }
    }
}
