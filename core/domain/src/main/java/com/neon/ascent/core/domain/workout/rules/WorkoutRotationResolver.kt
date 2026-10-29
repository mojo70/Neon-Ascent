package com.neon.ascent.core.domain.workout.rules

import com.neon.ascent.core.domain.workout.models.CyberCrappRotationMode
import com.neon.ascent.core.domain.workout.models.ProtocolDayType
import com.neon.ascent.core.domain.workout.models.WorkoutProtocol
import com.neon.ascent.core.domain.workout.models.WorkoutRoutine
import com.neon.ascent.core.domain.workout.models.WorkoutSession

object WorkoutRotationResolver {

    /**
     * Resolves the next routine in the rotation based on completed session history and rotation mode.
     *
     * Straight mode stays on the A/B/C strength cycle (Push A, Pull A, Legs A).
     * A/B mode keeps that same day order and alternates the variant inside each day.
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
     * A/B/C here is Push/Pull/Legs, not the exercise-variant suffix.
     */
    fun resolveNextDayTypeLetter(
        sessions: List<WorkoutSession>,
        routines: List<WorkoutRoutine>,
        protocol: WorkoutProtocol = WorkoutProtocol.CYBER_CRAPP,
        rotationMode: CyberCrappRotationMode = CyberCrappRotationMode.STRAIGHT
    ): String {
        val nextRoutine = resolveNextRoutine(sessions, routines, protocol, rotationMode)
        return when (nextRoutine?.let { dayTypeForRoutine(it) }) {
            ProtocolDayType.CC_A -> "A"
            ProtocolDayType.CC_B -> "B"
            ProtocolDayType.CC_C -> "C"
            else -> "A"
        }
    }

    fun isSessionVariantB(session: WorkoutSession): Boolean {
        val notes = session.notes?.lowercase() ?: ""
        // Do not treat a trailing "_b" as variant B. The straight Pull routine id is
        // routine_cybercrapp_b, so any note that quotes that id would look like a B variant.
        return notes.contains("variant_b") ||
               notes.contains("push_b") ||
               notes.contains("pull_b") ||
               notes.contains("legs_b") ||
               notes.contains("push b") ||
               notes.contains("pull b") ||
               notes.contains("legs b") ||
               notes.contains("(b)")
    }

    /**
     * Push / Pull / Legs slot for a CyberCrapp routine.
     *
     * The straight Pull routine is `routine_cybercrapp_b`. Variant routines also end in `_b`
     * (`routine_cybercrapp_pull_b`, `routine_cybercrapp_legs_b`), so the id suffix is not the day.
     */
    fun dayTypeForRoutine(routine: WorkoutRoutine): ProtocolDayType? {
        val id = routine.id.lowercase()
        val name = routine.name.lowercase()
        return when {
            id.contains("legs") || name.contains("legs") || id == "routine_cybercrapp_c" || id.endsWith("_c") || name.contains("squat") ->
                ProtocolDayType.CC_C
            id.contains("pull") || name.contains("pull") || id == "routine_cybercrapp_b" ->
                ProtocolDayType.CC_B
            id.contains("push") || name.contains("push") || id == "routine_cybercrapp_a" || id.endsWith("_a") ->
                ProtocolDayType.CC_A
            else -> null
        }
    }

    fun isRoutineVariantB(routine: WorkoutRoutine): Boolean {
        val id = routine.id.lowercase()
        val name = routine.name.lowercase()
        if (id == "routine_cybercrapp_a" || id == "routine_cybercrapp_b" || id == "routine_cybercrapp_c") {
            return false
        }
        return id.contains("push_b") ||
            id.contains("pull_b") ||
            id.contains("legs_b") ||
            id.contains("_a_b") ||
            id.contains("_b_b") ||
            id.contains("_c_b") ||
            name.contains("push b") ||
            name.contains("pull b") ||
            name.contains("legs b") ||
            name.contains("push (b)") ||
            name.contains("pull (b)") ||
            name.contains("legs (b)")
    }

    private fun findRoutineForDayTypeAndVariant(
        routines: List<WorkoutRoutine>,
        dayType: ProtocolDayType,
        isVariantB: Boolean
    ): WorkoutRoutine? {
        return routines.find { routine ->
            dayTypeForRoutine(routine) == dayType && isRoutineVariantB(routine) == isVariantB
        }
    }

    private fun findRoutineForDayType(routines: List<WorkoutRoutine>, dayType: ProtocolDayType): WorkoutRoutine? {
        val matches = routines.filter { dayTypeForRoutine(it) == dayType }
        return matches.firstOrNull { !isRoutineVariantB(it) } ?: matches.firstOrNull()
    }

    fun resolveDayTypeFromSession(session: WorkoutSession): ProtocolDayType? {
        if (session.protocolDayType != null) return session.protocolDayType

        val notes = session.notes?.lowercase() ?: ""
        return when {
            notes.contains("legs_b") || notes.contains("legs b") || notes.contains("cybercrapp_c") || notes.contains("legs") || notes.contains("squat") ->
                ProtocolDayType.CC_C
            notes.contains("pull_b") || notes.contains("pull b") || notes.contains("cybercrapp_b") || notes.contains("pull") ->
                ProtocolDayType.CC_B
            notes.contains("push_b") || notes.contains("push b") || notes.contains("cybercrapp_a") || notes.contains("push") ->
                ProtocolDayType.CC_A
            else -> null
        }
    }
}
