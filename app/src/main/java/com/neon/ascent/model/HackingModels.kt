package com.neon.ascent.model

import android.os.Parcelable
import kotlinx.serialization.Serializable

@Serializable
data class Challenge(
    val id: String,
    val tier: DifficultyTier,
    val type: ChallengeType,           // PCAP_ANALYSIS, MEMORY_FORENSICS, etc.
    val focusSkill: SkillType,         // Analysis, Crypto, etc. → ties to rewards
    val title: String,                 // "Corp Node 47 – DNS Exfil Detected"
    val description: String,           // AI core flavor text
    val timeLimitSeconds: Int,
    val bufferSize: Int,               // how many "actions" player has
    val packets: List<Packet>,         // only for PCAP type
    val forensicsData: String? = null, // simplified to String for now (e.g., JSON blob)
    val correctFlag: String,           // hidden string player must extract
    val solutionHint: String,          // shown after timeout or on fail
    val metadata: Map<String, String>  // e.g. "expectedFilter": "dns contains flag"
)

@Serializable
data class Packet(
    val timestamp: Long,
    val srcIp: String,
    val dstIp: String,
    val protocol: Protocol,   // HTTP, DNS, TCP, ICMP, TLS, etc.
    val length: Int,
    val payload: String,      // base64 or hex string for display
    val summary: String       // "GET /login HTTP/1.1" or "DNS query for corp-secret.com"
)

@Serializable
enum class Protocol {
    HTTP, DNS, TCP, ICMP, TLS, UDP
}

@Serializable
enum class ChallengeType { PCAP_ANALYSIS, MEMORY_FORENSICS, CRYPTO_BREAK, WEB_EXPLOIT, MIXED_CHAIN }

@Serializable
enum class SkillType { ANALYSIS, STEALTH, CRYPTO, EXPLOITATION }

@Serializable
enum class DifficultyTier { NOVICE, OPERATIVE, GHOST, NETRUNNER, BLACK_ICE }

@Serializable
data class PcapTemplate(
    val title: String,
    val description: String,
    val solutionHint: String,
    val recommendedFilter: String,
    val learningPoint: String
)
