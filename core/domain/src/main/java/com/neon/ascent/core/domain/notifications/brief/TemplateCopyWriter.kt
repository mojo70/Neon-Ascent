package com.neon.ascent.core.domain.notifications.brief

import com.neon.ascent.core.domain.notifications.models.BriefCopy
import com.neon.ascent.core.domain.notifications.models.BriefFacts
import com.neon.ascent.core.domain.notifications.models.BriefSlot
import com.neon.ascent.core.domain.notifications.models.BriefStance

object TemplateCopyWriter {
    fun write(facts: BriefFacts, stance: BriefStance): BriefCopy {
        return if (facts.slot == BriefSlot.AM) {
            AmTemplateWriter.write(facts, stance)
        } else {
            PmTemplateWriter.write(facts, stance)
        }
    }
}
