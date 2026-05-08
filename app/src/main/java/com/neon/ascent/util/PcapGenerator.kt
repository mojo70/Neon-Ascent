package com.neon.ascent.util

import com.neon.ascent.model.*
import kotlin.random.Random
import android.util.Base64

/**
 * Procedural PCAP Challenge Generator for Neon Ascent
 * Fully local, lightweight, and educational.
 */
class PcapGenerator {

    companion object {
        private const val DEFAULT_SEED_SALT = 42L
    }

    /**
     * Main entry point
     */
    fun generate(
        tier: DifficultyTier,
        focusSkill: SkillType,
        seed: Long? = null
    ): Challenge {
        val random = Random(seed ?: (System.currentTimeMillis() + DEFAULT_SEED_SALT))
        val template = pickTemplate(tier, focusSkill, random)

        val packetCount = when (tier) {
            DifficultyTier.NOVICE -> 25..40
            DifficultyTier.OPERATIVE -> 60..90
            DifficultyTier.GHOST -> 140..190
            DifficultyTier.NETRUNNER -> 220..320
            DifficultyTier.BLACK_ICE -> 400..600
        }.random(random)

        val packets = generateBasePackets(packetCount, random)
        val flag = generateFlag(tier, random)

        // Embed the flag using increasing complexity
        embedFlag(packets, flag, tier, focusSkill, random)

        val timeLimit = calculateTimeLimit(tier, focusSkill)
        val bufferSize = calculateBufferSize(tier)

        return Challenge(
            id = "pcap_${tier.name.lowercase()}_${System.currentTimeMillis()}",
            tier = tier,
            type = ChallengeType.PCAP_ANALYSIS,
            focusSkill = focusSkill,
            title = template.title,
            description = template.description,
            timeLimitSeconds = timeLimit,
            bufferSize = bufferSize,
            packets = packets,
            forensicsData = null,
            correctFlag = flag,
            solutionHint = template.solutionHint,
            metadata = mapOf(
                "recommendedFilter" to template.recommendedFilter,
                "learningPoint" to template.learningPoint
            )
        )
    }

    // =====================================================================

    private fun pickTemplate(
        tier: DifficultyTier,
        focusSkill: SkillType,
        random: Random
    ): PcapTemplate {
        val pool = when (tier) {
            DifficultyTier.NOVICE -> noviceTemplates
            DifficultyTier.OPERATIVE -> operativeTemplates
            DifficultyTier.GHOST -> ghostTemplates
            DifficultyTier.NETRUNNER -> netrunnerTemplates
            DifficultyTier.BLACK_ICE -> blackIceTemplates
        }
        return pool.random(random)
    }

    private fun generateBasePackets(count: Int, random: Random): MutableList<Packet> {
        val packets = mutableListOf<Packet>()
        val baseTime = System.currentTimeMillis() - 3600000L // start 1 hour ago

        repeat(count) {
            packets.add(
                Packet(
                    timestamp = baseTime + (it * 800L) + random.nextLong(-400, 400),
                    srcIp = generateRandomIp(random),
                    dstIp = generateRandomIp(random),
                    protocol = Protocol.entries.random(random),
                    length = (60..1500).random(random),
                    payload = "", // filled during embedding
                    summary = ""
                )
            )
        }
        return packets
    }

    private fun generateFlag(tier: DifficultyTier, random: Random): String {
        val prefix = "FLAG-"
        val length = when (tier) {
            DifficultyTier.NOVICE -> 8
            DifficultyTier.OPERATIVE -> 12
            DifficultyTier.GHOST -> 16
            DifficultyTier.NETRUNNER -> 24
            DifficultyTier.BLACK_ICE -> 32
        }
        val chars = ('A'..'Z') + ('0'..'9')
        return prefix + (1..length).map { chars.random(random) }.joinToString("")
    }

    private fun embedFlag(
        packets: MutableList<Packet>,
        flag: String,
        tier: DifficultyTier,
        focusSkill: SkillType,
        random: Random
    ) {
        val targetPacketIndex = ((packets.size * 0.6).toInt() + random.nextInt(-5, 6))
            .coerceIn(3, packets.size - 3)

        val target = packets[targetPacketIndex]

        when (tier) {
            DifficultyTier.NOVICE -> {
                // Simple HTTP Basic Auth leak
                val newTarget = target.copy(
                    protocol = Protocol.HTTP,
                    summary = "POST /api/upload HTTP/1.1",
                    payload = "Authorization: Basic ${base64("admin:$flag")}"
                )
                packets[targetPacketIndex] = newTarget
            }

            DifficultyTier.OPERATIVE -> {
                // DNS TXT record with Base64
                val newTarget = target.copy(
                    protocol = Protocol.DNS,
                    summary = "DNS TXT query for corp-node47.neonascent.internal",
                    payload = "TXT: \"exfil=${base64(flag)}\""
                )
                packets[targetPacketIndex] = newTarget
            }

            DifficultyTier.GHOST -> {
                // XOR + ICMP
                val xorKey = random.nextInt(1, 255).toByte()
                val xored = flag.toByteArray().map { (it.toInt() xor xorKey.toInt()).toChar() }.joinToString("")
                val newTarget = target.copy(
                    protocol = Protocol.ICMP,
                    summary = "ICMP Echo Request",
                    payload = "data: $xored | key_hint: 0x${xorKey.toString(16)}"
                )
                packets[targetPacketIndex] = newTarget
            }

            DifficultyTier.NETRUNNER -> {
                // Multi-layer: DNS tunnel → Base64 → simple XOR
                val layer1 = base64(flag)
                val xorKey = 0xA5.toByte()
                val layer2 = layer1.toByteArray().map { (it.toInt() xor xorKey.toInt()).toChar() }.joinToString("")
                
                val newTarget = target.copy(
                    protocol = Protocol.DNS,
                    summary = "DNS Query (suspicious subdomain)",
                    payload = "sub: ${layer2.take(32)}.exfil.neonascent.internal | layer:2"
                )
                packets[targetPacketIndex] = newTarget
                
                // Add second packet with key hint in another protocol
                if (targetPacketIndex + 3 < packets.size) {
                    val hintPacket = packets[targetPacketIndex + 3]
                    val newHintPacket = hintPacket.copy(
                        protocol = Protocol.TLS,
                        summary = "TLS ClientHello",
                        payload = "SNI: key=0xA5 | session_id=layer3"
                    )
                    packets[targetPacketIndex + 3] = newHintPacket
                }
            }

            DifficultyTier.BLACK_ICE -> {
                // Ultra complex: Fragmented exfil + Multi-layer encryption
                val xorKey = 0xDE.toByte()
                val layer1 = base64(flag)
                val layer2 = layer1.toByteArray().map { (it.toInt() xor xorKey.toInt()).toChar() }.joinToString("")
                
                // Fragment across 3 packets
                val parts = layer2.chunked(layer2.length / 3 + 1)
                parts.forEachIndexed { index, part ->
                    val idx = targetPacketIndex + (index * 5)
                    if (idx < packets.size) {
                        packets[idx] = packets[idx].copy(
                            protocol = Protocol.TCP,
                            summary = "TCP PSH, ACK [Seq=${1000 + index}, Win=64240]",
                            payload = "data_chunk_${index + 1}: $part"
                        )
                    }
                }
                
                // Key hidden in a separate "Decoy" packet
                val hintIdx = (targetPacketIndex - 10).coerceAtLeast(0)
                packets[hintIdx] = packets[hintIdx].copy(
                    protocol = Protocol.HTTP,
                    summary = "GET /internal/debug/keys/0xDE HTTP/1.1",
                    payload = "X-Debug-Header: entropy_check_passed"
                )
            }
        }

        // Fill noise packets with realistic summaries
        for (i in packets.indices) {
            val p = packets[i]
            if (p.summary.isEmpty()) {
                packets[i] = p.copy(
                    summary = generateNoiseSummary(p.protocol, random),
                    payload = generateNoisePayload(p.protocol, random)
                )
            }
        }
    }

    // =====================================================================
    // Helper functions
    private fun base64(input: String): String =
        Base64.encodeToString(input.toByteArray(), Base64.NO_WRAP)

    private fun generateRandomIp(random: Random): String {
        return "${(10..220).random(random)}.${(1..254).random(random)}.${(1..254).random(random)}.${(1..254).random(random)}"
    }

    private fun generateNoiseSummary(protocol: Protocol, random: Random): String {
        return when (protocol) {
            Protocol.HTTP -> listOf("GET /static/", "POST /api/v1/", "HEAD /health").random(random) + " HTTP/1.1"
            Protocol.DNS -> "DNS Query ${listOf("A", "AAAA", "MX").random(random)} record"
            Protocol.TCP -> "TCP ${listOf("SYN", "ACK", "FIN").random(random)}"
            Protocol.ICMP -> "ICMP Echo ${listOf("Request", "Reply").random(random)}"
            Protocol.TLS -> "TLS Handshake"
            else -> "${protocol.name} Packet"
        }
    }

    private fun generateNoisePayload(protocol: Protocol, random: Random): String {
        return when (protocol) {
            Protocol.HTTP -> "User-Agent: NeonAscent-Runner/${(1..9).random(random)}.0"
            else -> "payload_size=${(32..256).random(random)}"
        }
    }

    private fun calculateTimeLimit(tier: DifficultyTier, focusSkill: SkillType): Int {
        val base = when (tier) {
            DifficultyTier.NOVICE -> 180
            DifficultyTier.OPERATIVE -> 300
            DifficultyTier.GHOST -> 480
            DifficultyTier.NETRUNNER -> 720
            DifficultyTier.BLACK_ICE -> 900
        }
        // Skill bonus: every 20 levels in Analysis reduces time by ~8%
        // Assuming focusSkill level check here, but for now we'll just use 0
        val skillLevel = 0 
        val reduction = if (focusSkill == SkillType.ANALYSIS) (skillLevel / 20) * 8 else 0
        return (base * (1.0 - reduction / 100.0)).toInt()
    }

    private fun calculateBufferSize(tier: DifficultyTier): Int {
        return when (tier) {
            DifficultyTier.NOVICE -> 6
            DifficultyTier.OPERATIVE -> 10
            DifficultyTier.GHOST -> 15
            DifficultyTier.NETRUNNER -> 22
            DifficultyTier.BLACK_ICE -> 30
        }
    }

    // =====================================================================
    // Template pools (expand these as needed)
    private val noviceTemplates = listOf(
        PcapTemplate(
            title = "NeoCorp Employee Upload Leak",
            description = "A low-level employee node is leaking credentials. Find the exfil.",
            solutionHint = "Look for HTTP Basic Auth or Authorization headers",
            recommendedFilter = "http contains \"Authorization\"",
            learningPoint = "Basic Auth sends credentials in easily decodable Base64"
        )
    )

    private val operativeTemplates = listOf(
        PcapTemplate(
            title = "DNS Tunnel Detected – Node 47",
            description = "Suspicious DNS traffic is leaving the network. Extract the payload.",
            solutionHint = "Check TXT records and Base64 content",
            recommendedFilter = "dns contains \"TXT\"",
            learningPoint = "DNS can be used for data exfiltration because it's rarely blocked"
        )
    )

    private val ghostTemplates = listOf(
        PcapTemplate(
            title = "ICMP Covert Channel",
            description = "Advanced persistent threat using ICMP for C2. Crack the hidden data.",
            solutionHint = "Look for unusual payload in ICMP and possible XOR key",
            recommendedFilter = "icmp",
            learningPoint = "Ping packets can carry hidden data (ICMP tunneling)"
        )
    )

    private val netrunnerTemplates = listOf(
        PcapTemplate(
            title = "Multi-Layer Exfil – Arasaka BioVault",
            description = "High-value genetic data is being siphoned. Chain the layers.",
            solutionHint = "Start with DNS, find the key, then decode",
            recommendedFilter = "dns || tls.handshake",
            learningPoint = "Real APTs use multiple obfuscation layers"
        )
    )

    private val blackIceTemplates = listOf(
        PcapTemplate(
            title = "BLACK ICE: Neural-Net Fragmentation",
            description = "A military-grade ICE is fragmenting data across multiple streams. Reconstruct it.",
            solutionHint = "Find the key in HTTP debug logs, then reassemble TCP chunks",
            recommendedFilter = "tcp.flags.push == 1 || http",
            learningPoint = "Advanced threats use fragmentation and decoy packets to evade simple filters"
        )
    )
}
