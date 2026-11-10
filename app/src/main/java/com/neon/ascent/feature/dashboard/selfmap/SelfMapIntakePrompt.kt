package com.neon.ascent.feature.dashboard.selfmap

import com.neon.ascent.core.data.repository.SelfMapIntakeContext
import com.neon.ascent.core.domain.selfmap.SelfMapCategory

/**
 * Dedicated system prompt for Self Map intake. No persona, no questions, no prose.
 * Callers append the loaded context and the operator's last message as data.
 */
object SelfMapIntakePrompt {
    val SYSTEM: String = """
        [SYSTEM: Self Map Intake]
        Emit only a JSON array. Ground every object in the operator's last message.
        Each object must contain "name" (string), "category" (string), "importance" (number from 0.40 to 0.90), "note" (string), and "rationale" (string).
        "category" must be one of BODY_SENSES, PEOPLE, STUFF, ROLES_ACCOMPLISHMENTS, IDEAS_LIKES, CUSTOM.
        "rationale" must reuse words from the operator's last message.
        No questions. No text before or after the array.
    """.trimIndent()

    fun request(lastMessage: String, context: SelfMapIntakeContext): String {
        val items = if (context.activeItems.isEmpty()) {
            "Active items: none."
        } else {
            "Active items: " + context.activeItems.joinToString("; ") { item ->
                "${item.name} [${item.category.name}]"
            }
        }
        val body = if (context.bodyAndSenses.isEmpty()) {
            "Body & Senses: none."
        } else {
            "Body & Senses: " + context.bodyAndSenses.joinToString(", ") { it.name }
        }
        val categories = if (context.categoriesInUse.isEmpty()) {
            "Categories in use: none."
        } else {
            "Categories in use: " + context.categoriesInUse.joinToString(", ") { it.name }
        }
        return """
            $SYSTEM

            $items
            $body
            $categories
            Operator message: "$lastMessage"
        """.trimIndent()
    }

    fun anchored(lastMessage: String, context: SelfMapIntakeContext, seedName: String, task: String): String {
        return """
            $SYSTEM

            Seed node: "$seedName"
            $task
            Operator message: "$lastMessage"
            Active items: ${context.activeItems.size}
        """.trimIndent()
    }

    /** One form-field suggestion. Same system text as chat. One object, category fixed by the form. */
    fun formAssist(lastMessage: String, context: SelfMapIntakeContext, category: SelfMapCategory): String {
        return """
            ${request(lastMessage, context)}

            Emit one object.
            "category" must be ${category.name}.
        """.trimIndent()
    }
}
