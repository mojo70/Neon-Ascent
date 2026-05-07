package com.neon.ascent.domain.usecase

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GenerateCyberLoreUseCase @Inject constructor() {

    fun generateLore(answers: Map<Int, String>): String {
        val prompt = """
You are a master cyberpunk lore-weaver. Turn the following life story into a short, immersive, first-person cyber-lore biography (150-250 words).

Key facts:
- Origin: ${answers[1] ?: ""}
- Early life vibe: ${answers[2] ?: ""}
- Major turning point: ${answers[3] ?: ""}
- Current reality: ${answers[4] ?: ""}
- Proudest achievement: ${answers[5] ?: ""}
- Wish to change: ${answers[6] ?: ""}
- Grandest aspiration: ${answers[7] ?: ""}
- Ideal future self: ${answers[8] ?: ""}
- Legacy wanted: ${answers[9] ?: ""}
- Cyber/tech flavor: ${answers[11] ?: ""}

Style: Gritty but hopeful cyberpunk. Mix street poetry with digital mysticism. Make the user feel like the protagonist of their own legend. End with a powerful forward-looking statement that ties their past to their dreams.

Output only the biography, no explanations.
"""

        // TODO: Later → call local Gemma with this prompt
        // For now we can use a simple template or placeholder
        return "I was forged in the neon underbelly of ${answers[1] ?: "a sprawling megacity"}... [full generated lore here]"
    }
}
