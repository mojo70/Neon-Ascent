package com.neon.ascent.core.domain.workout.rules

import com.neon.ascent.core.domain.workout.models.CyberCrappRotationMode
import com.neon.ascent.core.domain.workout.models.ProtocolDayType
import com.neon.ascent.core.domain.workout.models.WorkoutProtocol
import com.neon.ascent.core.domain.workout.models.WorkoutRoutine
import com.neon.ascent.core.domain.workout.models.WorkoutSession

object WorkoutRotationResolver {

    /**
     * Resolves the next routine in the rotation based on completed session history and rotation mode.
     */
    fun resolveNextRoutine(
        sessions: List<WorkoutSession>,
        routines: List<WorkoutRoutine>,
        protocol: WorkoutProtocol = WorkoutProtocol.CYBER_CRAPP,
        rotationMode: CyberCrappRotationMode = CyberCrappRotationMode.STRAIGHT
    ): WorkoutRoutine? {
        val protocolRoutines = routines.filter { it.protocol == protocol }
        if (protocolRoutines.isEmpty()) return null

        val lastSession = sessions
            .filter { it.protocol == protocol }
            .maxByOrNull { it.date }

        val nextDayType = if (lastSession == null) {
            ProtocolDayType.CC_A
        } else {
            val lastDayType = resolveDayTypeFromSession(lastSession)
            when (lastDayType) {
                ProtocolDayType.CC_A -> ProtocolDayType.CC_B
                ProtocolDayType.CC_B -> ProtocolDayType.CC_C
                ProtocolDayType.CC_C -> ProtocolDayType.CC_A
                else -> ProtocolDayType.CC_A
            }
        }

        if (rotationMode == CyberCrappRotationMode.STRAIGHT) {
            return findRoutineForDayTypeAndVariant(protocolRoutines, nextDayType, isVariantB = false)
                ?: findRoutineForDayType(protocolRoutines, nextDayType)
                ?: protocolRoutines.first()
        }

        // A_B mode: alternate variants only within the target day type
        val lastSessionForDayType = sessions
            .filter { it.protocol == protocol && resolveDayTypeFromSession(it) == nextDayType }
            .maxByOrNull { it.date }

        val targetVariantB = if (lastSessionForDayType == null) {
            false
        } else {
            !isSessionVariantB(lastSessionForDayType)
        }

        return findRoutineForDayTypeAndVariant(protocolRoutines, nextDayType, isVariantB = targetVariantB)
            ?: findRoutineForDayTypeAndVariant(protocolRoutines, nextDayType, isVariantB = false)
            ?: findRoutineForDayType(protocolRoutines, nextDayType)
            ?: protocolRoutines.first()
    }

    /**
     * Resolves the day type letter ("A", "B", or "C") for the next session.
     */
    fun resolveNextDayTypeLetter(
        sessions: List<WorkoutSession>,
        routines: List<WorkoutRoutine>,
        protocol: WorkoutProtocol = WorkoutProtocol.CYBER_CRAPP,
        rotationMode: CyberCrappRotationMode = CyberCrappRotationMode.STRAIGHT
    ): String {
        val nextRoutine = resolveNextRoutine(sessions, routines, protocol, rotationMode)
        return when {
            nextRoutine?.id?.lowercase()?.endsWith("_a") == true || nextRoutine?.name?.contains("Push", ignoreCase = true) == true -> "A"
            nextRoutine?.id?.lowercase()?.endsWith("_b") == true || nextRoutine?.name?.contains("Pull", ignoreCase = true) == true -> "B"
            nextRoutine?.id?.lowercase()?.endsWith("_c") == true || nextRoutine?.name?.contains("Legs", ignoreCase = true) == true -> "C"
            else -> "A"
        }
    }

    fun isSessionVariantB(session: WorkoutSession): Boolean {
        val notes = session.notes?.lowercase() ?: ""
        return notes.contains("variant_b") ||
               notes.contains("push_b") ||
               notes.contains("pull_b") ||
               notes.contains("legs_b") ||
               notes.contains("push b") ||
               notes.contains("pull b") ||
               notes.contains("legs b") ||
               notes.contains("(b)") ||
               notes.endsWith("_b")
    }

    private fun findRoutineForDayTypeAndVariant(
        routines: List<WorkoutRoutine>,
        dayType: ProtocolDayType,
        isVariantB: Boolean
    ): WorkoutRoutine? {
        return routines.find { routine ->
            val id = routine.id.lowercase()
            val name = routine.name.lowercase()

            val matchesDayType = when (dayType) {
                ProtocolDayType.CC_A -> id.contains("push") || name.contains("push") || id == "routine_cybercrapp_a" || id.endsWith("_a")
                ProtocolDayType.CC_B -> id.contains("pull") || name.contains("pull") || id == "routine_cybercrapp_b"
                ProtocolDayType.CC_C -> id.contains("legs") || name.contains("legs") || id == "routine_cybercrapp_c" || id.endsWith("_c") || name.contains("squat")
                else -> id.contains("push") || name.contains("push")
            }

            if (!matchesDayType) return@find false

            val matchesVariantB = when (dayType) {
                ProtocolDayType.CC_A -> id.contains("push_b") || id.contains("_a_b") || name.contains("push b") || name.contains("push (b)") || (id != "routine_cybercrapp_a" && id.endsWith("_b"))
                ProtocolDayType.CC_B -> id.contains("pull_b") || id.contains("_b_b") || name.contains("pull b") || name.contains("pull (b)")
                ProtocolDayType.CC_C -> id.contains("legs_b") || id.contains("_c_b") || name.contains("legs b") || name.contains("legs (b)")
                else -> false
            }

            if (isVariantB) matchesVariantB else !matchesVariantB
        }
    }

    private fun findRoutineForDayType(routines: List<WorkoutRoutine>, dayType: ProtocolDayType): WorkoutRoutine? {
        val matchSuffix = when (dayType) {
            ProtocolDayType.CC_A -> "_a"
            ProtocolDayType.CC_B -> "_b"
            ProtocolDayType.CC_C -> "_c"
            else -> "_a"
        }
        val matchKeyword = when (dayType) {
            ProtocolDayType.CC_A -> "push"
            ProtocolDayType.CC_B -> "pull"
            ProtocolDayType.CC_C -> "legs"
            else -> "push"
        }
        return routines.find {
            it.id.lowercase().endsWith(matchSuffix) ||
            it.name.lowercase().contains(" ($matchKeyword)") ||
            it.name.lowercase().contains(matchKeyword)
        }
    }

    fun resolveDayTypeFromSession(session: WorkoutSession): ProtocolDayType? {
        if (session.protocolDayType != null) return session.protocolDayType

        val notes = session.notes?.lowercase() ?: ""
        return when {
            notes.contains("cybercrapp_a") || notes.contains("push") -> ProtocolDayType.CC_A
            notes.contains("cybercrapp_b") || notes.contains("pull") -> ProtocolDayType.CC_B
            notes.contains("cybercrapp_c") || notes.contains("legs") || notes.contains("squat") -> ProtocolDayType.CC_C
            else -> null
        }
    }
}
