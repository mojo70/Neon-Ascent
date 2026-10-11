package com.neon.ascent.core.domain.selfmap

enum class SelfMapCategory(val displayName: String, val description: String) {
    BODY_SENSES("Body & Senses", "Physical form, sensory experiences, health, and appearance"),
    PEOPLE("People", "Family, partners, friends, social circles, and connections"),
    STUFF("Stuff", "Possessions, material assets, tools, and environment"),
    ROLES_ACCOMPLISHMENTS("Roles & Accomplishments", "Titles, career, status, achievements, and responsibilities"),
    IDEAS_LIKES("Ideas / Likes / Dislikes", "Beliefs, values, preferences, opinions, and core concepts"),
    CUSTOM("Custom", "Unique attachments or personal constructs")
}
