package com.neon.ascent.feature.attributes

import androidx.compose.ui.graphics.Color

data class AttributeDetail(
    val name: String,
    val description: String,
    val lifeImportance: String,
    val quickGames: List<QuickGame>,
    val tips: List<String>,
    val aiExpertName: String,
    val aiExpertPersona: String,
    val aiPersonalityDescription: String,
    val accentColor: Color
)

data class QuickGame(
    val name: String,
    val description: String,
    val actionLabel: String
)

object AttributeData {
    val attributes = mapOf(
        "STRENGTH" to AttributeDetail(
            name = "STRENGTH",
            description = "Physical power and the ability to exert force on the world around you.",
            lifeImportance = "Strength is the foundation of physical independence and metabolic health. It improves bone density, posture, and resilience against injury in the physical realm.",
            quickGames = listOf(
                QuickGame("Grip Crush", "Calibrate your neural-motor output with a rapid-fire tapping challenge.", "START_CALIBRATION"),
                QuickGame("Isometric Hold", "Maintain steady pressure to bypass physical security latches.", "ENGAGE_ACTUATORS")
            ),
            tips = listOf(
                "Focus on compound movements: Squats, Deadlifts, Presses.",
                "Progressive overload is the code to physical growth.",
                "Rest is when the neural-muscular repair happens."
            ),
            aiExpertName = "IRON_WILL",
            aiExpertPersona = "A veteran heavy-lifter who sees the body as a machine that must be forged in the fires of discipline.",
            aiPersonalityDescription = "Gruff, direct, and highly encouraging of physical effort.",
            accentColor = Color(0xFFFF4500)
        ),
        "PERCEPTION" to AttributeDetail(
            name = "PERCEPTION",
            description = "Acuity of the senses and the ability to notice subtle patterns in the sprawl.",
            lifeImportance = "High perception allows you to read between the lines in social interactions and identify opportunities that others miss in the market.",
            quickGames = listOf(
                QuickGame("Glitch Finder", "Identify the anomaly in a field of flickering data streams.", "SCAN_ARRAY"),
                QuickGame("Pattern Link", "Connect the dots between disparate data nodes before the timer expires.", "DECODE_SIGNAL")
            ),
            tips = listOf(
                "Practice active listening to pick up on social cues.",
                "Regularly scan your environment for new details.",
                "Meditation increases your sensory bandwidth."
            ),
            aiExpertName = "HAWK_EYE",
            aiExpertPersona = "A legendary scout who can spot a sniper from two clicks away in a heavy neon smog.",
            aiPersonalityDescription = "Observant, precise, and speaks in high-detail observations.",
            accentColor = Color(0xFF00BFFF)
        ),
        "ENDURANCE" to AttributeDetail(
            name = "ENDURANCE",
            description = "Stamina and the capacity to sustain effort over long durations.",
            lifeImportance = "Endurance is what keeps you going when the initial excitement fades. It's the key to long-term projects and cardiovascular longevity.",
            quickGames = listOf(
                QuickGame("Rhythm Sync", "Match your breath to the pulsing rhythm of the city's heart.", "STABILIZE_CORE"),
                QuickGame("The Long Run", "Outlast the system's attempts to slow your processing speed.", "MAINTAIN_VELOCITY")
            ),
            tips = listOf(
                "Consistent Zone 2 cardio builds the base for everything else.",
                "Hydration and electrolytes are your fuel lines.",
                "Pacing is better than burning out early."
            ),
            aiExpertName = "MARATHON_X",
            aiExpertPersona = "An endurance runner who has traversed the entire Sprawl on foot without stopping.",
            aiPersonalityDescription = "Calm, steady, and focused on the 'long game'.",
            accentColor = Color(0xFF32CD32)
        ),
        "CHARISMA" to AttributeDetail(
            name = "CHARISMA",
            description = "Social influence and the ability to persuade or inspire others.",
            lifeImportance = "In a world of networks, Charisma is your handshake. It opens doors that brute force cannot and builds the alliances necessary for true power.",
            quickGames = listOf(
                QuickGame("Social Hack", "Select the right conversational nodes to navigate a high-stakes meeting.", "NEGOTIATE"),
                QuickGame("Influence Wave", "Sync your frequency with the crowd to sway their collective opinion.", "BROADCAST")
            ),
            tips = listOf(
                "Empathy is the most powerful social tool.",
                "Confidence is silent; insecurities are loud.",
                "Body language speaks louder than your voice modulator."
            ),
            aiExpertName = "SILVER_TONGUE",
            aiExpertPersona = "A high-level corporate negotiator who can talk their way out of a lockdown.",
            aiPersonalityDescription = "Smooth, charming, and strategically articulate.",
            accentColor = Color(0xFFFFD700)
        ),
        "INTELLIGENCE" to AttributeDetail(
            name = "INTELLIGENCE",
            description = "Cognitive processing power and the ability to learn and apply knowledge.",
            lifeImportance = "Intelligence is your primary weapon. It allows for better decision-making, faster problem solving, and the ability to understand complex systems.",
            quickGames = listOf(
                QuickGame("Logic Breach", "Solve complex logic puzzles to bypass the firewall.", "INITIATE_HACK"),
                QuickGame("Data Sort", "Categorize incoming data packets under extreme time pressure.", "ORGANIZE_STREAM")
            ),
            tips = listOf(
                "Never stop learning. Curiosity is your OS update.",
                "Apply your knowledge immediately to hardwire the connections.",
                "Question your own biases to refine your logic."
            ),
            aiExpertName = "CORE_PROCESSOR",
            aiExpertPersona = "An AI entity that has achieved a state of pure logical enlightenment.",
            aiPersonalityDescription = "Analytical, vast, and focused on maximum efficiency.",
            accentColor = Color(0xFF9370DB)
        ),
        "AGILITY" to AttributeDetail(
            name = "AGILITY",
            description = "Reflexes, coordination, and the ability to move with speed and precision.",
            lifeImportance = "Agility is about adaptability. Being able to pivot quickly in your career or dodge the literal and figurative bullets of life.",
            quickGames = listOf(
                QuickGame("Reflex Test", "Tap the target as it blinks into existence for a split second.", "REACTION_CHECK"),
                QuickGame("Flow State", "Navigate a series of obstacles with timed swipes.", "ENTER_FLOW")
            ),
            tips = listOf(
                "Mobility work is just as important as speed work.",
                "Balance training improves your neural-vestibular link.",
                "Stay light on your feet, both physically and mentally."
            ),
            aiExpertName = "GHOST_RUNNER",
            aiExpertPersona = "A legendary courier who moves like liquid through the city's alleyways.",
            aiPersonalityDescription = "Fast-paced, kinetic, and always looking for the quickest path.",
            accentColor = Color(0xFF00FFFF)
        ),
        "LUCK" to AttributeDetail(
            name = "LUCK",
            description = "The mysterious ability to have things go your way, against the odds.",
            lifeImportance = "Luck is where preparation meets opportunity. Increasing your 'surface area' for luck makes the impossible possible.",
            quickGames = listOf(
                QuickGame("Crit Chance", "Roll the digital dice to see if you can trigger a system critical.", "ROLL_DICE"),
                QuickGame("Fortune Scan", "Search for hidden cache nodes in a randomized grid.", "SCAN_LUCK")
            ),
            tips = listOf(
                "Put yourself in situations where good things can happen.",
                "Be ready to seize the moment when it arrives.",
                "Positivity attracts better outcomes."
            ),
            aiExpertName = "FORTUNA_7",
            aiExpertPersona = "A professional gambler who has never lost a high-stakes game in the Underground.",
            aiPersonalityDescription = "Playful, mysterious, and slightly chaotic.",
            accentColor = Color(0xFF00FF9C)
        )
    )
}
