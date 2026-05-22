package com.neon.ascent.domain.usecase

import com.neon.ascent.feature.biohacking.AiProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GenerateCyberLoreUseCase @Inject constructor(
    private val aiProvider: AiProvider
) {

    suspend fun generateLore(answers: Map<Int, String>): String {
        val prompt = """
You are a master cyberpunk lore-weaver. Turn the following life story into a short, immersive, first-person cyber-lore biography (150-250 words).

Key facts:
- Origin: ${answers[1] ?: "Unknown"}
- Early life vibe: ${answers[2] ?: "Gritty"}
- Major turning point: ${answers[3] ?: "The glitch"}
- Current reality: ${answers[4] ?: "Surviving the sprawl"}
- Proudest achievement: ${answers[5] ?: "Living another day"}
- Wish to change: ${answers[6] ?: "The past"}
- Grandest aspiration: ${answers[7] ?: "Rising above the neon"}
- Ideal future self: ${answers[8] ?: "Digital deity"}
- Legacy wanted: ${answers[9] ?: "Unforgettable echo"}
- Cyber/tech flavor: ${answers[11] ?: "Glitchy chrome"}

Style: Gritty but hopeful cyberpunk. Mix street poetry with digital mysticism. Make the user feel like the protagonist of their own legend. End with a powerful forward-looking statement that ties their past to their dreams.

Output only the biography, no explanations.
"""
        val result = aiProvider.generateContent(prompt)
        return if (result.startsWith("ERROR:")) {
            "I was forged in the neon underbelly... [AI OFFLINE: ${result}]"
        } else {
            result.trim()
        }
    }

    suspend fun generateWeeklyUpdate(
        currentStory: String,
        accomplishments: List<String>
    ): String {
        val accomplishmentsText = accomplishments.joinToString("\n") { "- $it" }
        val prompt = """
You are a master cyberpunk lore-weaver. Append a new chapter to a runner's legend based on their accomplishments this past week.

PREVIOUS LEGEND:
$currentStory

ACCOMPLISHMENTS THIS WEEK:
$accomplishmentsText

TASK: Write a new, immersive, first-person "Weekly Log" (100-150 words) that weaves these accomplishments into the ongoing narrative. 
Style: Gritty, street-tech, poetic. The tone should reflect the difficulty and triumph of surviving and progressing in a megacity.
Use terms like 'Eddies', 'Chrome', 'The Net', 'ICE', 'Chooms'.

Output only the new chapter content, no headers or explanations.
"""
        val result = aiProvider.generateContent(prompt)
        return if (result.startsWith("ERROR:")) {
            "Another week in the sprawl. Tasks done, data mined. The grind continues... [AI OFFLINE]"
        } else {
            result.trim()
        }
    }
}
