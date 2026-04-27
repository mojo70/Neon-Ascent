package com.neon.ascent.util

fun derivePersonalityArchetype(mbti: String, alignment: String): Pair<String, String> {
    return when {
        mbti.startsWith("INF") && alignment.contains("Good") -> 
            "THE IDEALIST" to "Driven by strong values and a desire to help others. You are the glitch in the corporate machine that fights for the people."
        mbti.startsWith("INT") -> 
            "THE STRATEGIST" to "Analytical and goal-oriented. You see the city as a series of systems to be optimized or exploited."
        mbti.contains("ENF") && alignment.contains("Chaotic") -> 
            "THE ADVOCATE" to "Enthusiastic and inspiring. Your creativity is your weapon against the neon monotony."
        mbti.contains("IST") -> 
            "THE PRAGMATIST" to "Observant and adaptable. You don't care about the 'why', only that the job gets done and you get paid."
        else -> 
            "THE EDGE-RUNNER" to "A versatile survivalist in the sprawl. You balance logic and instinct to stay one step ahead of the ICE."
    }
}
