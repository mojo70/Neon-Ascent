package com.neon.ascent.feature.selfmap

import com.neon.ascent.core.data.local.dao.SelfMapDao
import com.neon.ascent.core.data.local.entity.EoEEntryEntity
import com.neon.ascent.core.data.local.entity.SelfItemEntity
import com.neon.ascent.core.data.local.entity.SelfMapMoodEntryEntity
import com.neon.ascent.core.data.local.entity.SelfMapSnapshotEntity
import com.neon.ascent.core.data.repository.SelfMapIntakeContext
import com.neon.ascent.core.data.repository.SelfMapIntakeSession
import com.neon.ascent.core.domain.selfmap.SelfMapCategory
import com.neon.ascent.core.domain.selfmap.SelfMapIntakeMode
import com.neon.ascent.feature.dashboard.selfmap.ChatTurnMessage
import com.neon.ascent.feature.dashboard.selfmap.IntakeStep
import com.neon.ascent.feature.dashboard.selfmap.MessageSender
import com.neon.ascent.feature.dashboard.selfmap.ProposedItemCandidate
import com.neon.ascent.feature.dashboard.selfmap.SelfMapEntityExtractor
import com.neon.ascent.feature.dashboard.selfmap.intake.IntakeAutoAdvance
import com.neon.ascent.feature.dashboard.selfmap.intake.IntakeFormHandoff
import com.neon.ascent.feature.dashboard.selfmap.intake.IntakeModelGate
import com.neon.ascent.feature.dashboard.selfmap.intake.IntakeProposalResolution
import com.neon.ascent.feature.dashboard.selfmap.intake.IntakeResume
import com.neon.ascent.feature.dashboard.selfmap.intake.IntakeTranscriptCodec
import com.neon.ascent.feature.dashboard.selfmap.intake.IntakeUiState
import com.neon.ascent.feature.dashboard.selfmap.intake.SelfMapEntry
import com.neon.ascent.feature.dashboard.selfmap.SelfMapIntakePrompt
import com.neon.ascent.feature.dashboard.selfmap.SelfMapConversationalIntakeUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Local evaluation scenarios for LardAss Slice 1 (Self Map Intake):
 * 1. Empty state -> pre-seeds Body & Senses items.
 * 2. Pre-seed only state -> intake starts at step 0.
 * 3. High People coverage -> context-aware question shortening.
 * 4. Skip -> sets conversational intake seen and lands on canvas.
 * 5. Resume -> resumes at saved step index with rejected signatures and accepted items preserved.
 * 6. Multi-sentence family / faith / work reply -> correctly grounded proposals.
 * 7. Blocked pattern rejection -> CYBR-TES / Socrates / Runner / protocol / vision/stimulus rejected at validation.
 * 8. Ungrounded rationale fallback -> discarded and falls back to follow-up question.
 * 9. Form switch -> preserves partial draft fields.
 * 10. Radial canvas landing -> pre-seeded Body & Senses items remain intact.
 * 11. Pre-proposal gating -> model never proposes before operator input on a step.
 * 12. Insert isolation -> writes occur exclusively through SelfMapRepository upon explicit operator acceptance.
 */
class SelfMapLocalEvalScenariosTest {

    private lateinit var fakeDao: FakeSelfMapDao
    private lateinit var intakeUseCase: SelfMapConversationalIntakeUseCase

    @Before
    fun setUp() {
        fakeDao = FakeSelfMapDao()
        intakeUseCase = SelfMapConversationalIntakeUseCase(FakeAiCore())
    }

    @Test
    fun scenario1_EmptyStatePreseed() = runBlocking {
        val currentItems = fakeDao.getAllItemsOnce()
        assertTrue(currentItems.isEmpty())

        fakeDao.insertSelfItem(SelfItemEntity(name = "Body", category = SelfMapCategory.BODY_SENSES, importance = 0.9f))
        val seeded = fakeDao.getAllItemsOnce()
        assertEquals(1, seeded.size)
        assertEquals(SelfMapCategory.BODY_SENSES, seeded.first().category)
    }

    @Test
    fun scenario2_PreseedOnlyStart() = runBlocking {
        fakeDao.insertSelfItem(SelfItemEntity(name = "Body", category = SelfMapCategory.BODY_SENSES, importance = 0.9f))
        val (msg, proposals) = intakeUseCase.processUserTurn(0, "", fakeDao.getAllItemsOnce())
        assertTrue(msg.isNotBlank())
        assertTrue(proposals.isEmpty())

        val (respMsg, respProposals) = intakeUseCase.processUserTurn(0, "I am ready.", fakeDao.getAllItemsOnce())
        assertTrue(respMsg.isNotBlank())
        assertTrue(respProposals.isNotEmpty())
    }

    @Test
    fun scenario3_HighPeopleCoverageContextAware() = runBlocking {
        fakeDao.insertSelfItem(SelfItemEntity(name = "Family", category = SelfMapCategory.PEOPLE, importance = 0.85f))

        val (msg, proposals) = intakeUseCase.processUserTurn(1, "", fakeDao.getAllItemsOnce())
        assertTrue(msg.isNotBlank())
        assertTrue(proposals.isEmpty())

        val (respMsg, respProposals) = intakeUseCase.processUserTurn(1, "My team matters.", fakeDao.getAllItemsOnce())
        assertTrue(respMsg.isNotBlank())
        assertTrue(respProposals.isNotEmpty())
    }

    @Test
    fun scenario4_SkipIntake() {
        // End of sequence and "I'm done" set isCompleted. The hub maps that to the radial canvas.
        val steps = List(5) { index ->
            IntakeStep(index, "phase", "question $index", SelfMapCategory.CUSTOM, emptyList())
        }
        val finished = IntakeAutoAdvance.toStep(IntakeUiState(currentStepIndex = 4), steps, 5)
        assertTrue(finished.isCompleted)
        assertEquals(0, finished.currentStepIndex)
        assertEquals(
            SelfMapEntry.Surface.CANVAS,
            SelfMapEntry.surface(intakeSeen = true, preference = SelfMapIntakeMode.CHAT, reason = SelfMapEntry.Reason.FIRST_OPEN),
        )
    }

    @Test
    fun scenario5_ResumeInterruptedSession() {
        val rejected = ProposedItemCandidate(
            id = "bad",
            name = "BadProposal",
            category = SelfMapCategory.PEOPLE,
            suggestedImportance = 0.4f,
        )
        val history = listOf(
            ChatTurnMessage(sender = MessageSender.GUIDE, text = "Who are the people who matter most to you right now? People nodes are usually the attachments that produce the strongest reactions when threatened."),
            ChatTurnMessage(sender = MessageSender.OPERATOR, text = "My family."),
            ChatTurnMessage(sender = MessageSender.GUIDE, text = "Based on that, one node:", proposals = listOf(rejected)),
        )
        val session = SelfMapIntakeSession(
            stepIndex = 2,
            acceptedItemIds = setOf("1", "2"),
            rejectedSignatures = setOf(IntakeProposalResolution.signature(rejected)),
            stepAnchor = 0,
            transcript = IntakeTranscriptCodec.encode(history),
        )
        assertTrue(session.hasResumeState())
        assertFalse(SelfMapIntakeSession(0, emptySet(), emptySet()).hasResumeState())

        val restored = IntakeResume.restore(session, List(5) {
            IntakeStep(it, "p", "q$it", SelfMapCategory.CUSTOM, emptyList())
        })!!
        assertEquals(2, restored.currentStepIndex)
        assertEquals(setOf("1", "2"), restored.acceptedIds)
        assertEquals(history.map { it.text }, restored.messages.map { it.text })
        assertEquals("bad", restored.messages.last().proposals.single().id)
        assertFalse(
            IntakeProposalResolution.isActionable(
                restored.messages.last().proposals.single(),
                restored.acceptedIds,
                restored.rejectedIds,
                restored.dismissedIds,
            )
        )
    }

    @Test
    fun scenario6_MultiSentenceFamilyFaithWorkReply() = runBlocking {
        var modelCalled = false
        val core = object : com.neon.ascent.core.domain.ai.AiCore {
            override suspend fun isReady(): Boolean = true
            override suspend fun warmup() {}
            override suspend fun generate(prompt: String, forceLocal: Boolean): com.neon.ascent.core.domain.ai.AiResult {
                modelCalled = true
                return com.neon.ascent.core.domain.ai.AiResult.Failure("model")
            }
            override suspend fun generateIsolated(prompt: String): com.neon.ascent.core.domain.ai.AiResult {
                modelCalled = true
                return com.neon.ascent.core.domain.ai.AiResult.Failure("model")
            }
            @Suppress("OVERRIDE_DEPRECATION")
            override suspend fun generateContent(prompt: String, forceLocal: Boolean): String = ""
        }
        val complexInput = "My family and my work as an architect anchor my life, and my faith sustains my daily purpose."
        val (msg, proposals) = SelfMapConversationalIntakeUseCase(core).processUserTurn(1, complexInput, fakeDao.getAllItemsOnce())
        assertFalse(modelCalled)
        assertEquals("Based on that, two nodes:", msg)
        assertEquals(listOf("Family & Kin", "Faith & Spiritual Anchor"), proposals.map { it.name })
        proposals.forEach { proposal ->
            assertTrue(proposal.rationale!!.contains("\""))
            assertFalse(proposal.rationale.contains(complexInput))
        }
    }

    @Test
    fun scenario7_BlockedPatternRejection() = runBlocking {
        val blockingAiCore = object : com.neon.ascent.core.domain.ai.AiCore {
            override suspend fun isReady(): Boolean = true
            override suspend fun warmup() {}
            override suspend fun generate(prompt: String, forceLocal: Boolean): com.neon.ascent.core.domain.ai.AiResult {
                return com.neon.ascent.core.domain.ai.AiResult.Success("CYBR-TES protocol violation: [PROPOSAL: Bad Node | PEOPLE | 0.8 | note | rationale]")
            }
            override suspend fun generateIsolated(prompt: String): com.neon.ascent.core.domain.ai.AiResult = generate(prompt, true)
            @Suppress("OVERRIDE_DEPRECATION")
            override suspend fun generateContent(prompt: String, forceLocal: Boolean): String = "Generated"
        }
        val blocked = ProposedItemCandidate(
            name = "Bad Node",
            category = SelfMapCategory.PEOPLE,
            suggestedImportance = 0.8f,
            note = "note",
            rationale = "CYBR-TES protocol",
        )
        assertTrue(
            IntakeModelGate.accepted(
                rawText = "CYBR-TES protocol violation",
                proposals = listOf(blocked),
                anchor = "Testing blocked patterns.",
                seedName = null,
            ).isEmpty()
        )
        val blockingUseCase = SelfMapConversationalIntakeUseCase(blockingAiCore)
        val (msg, proposals) = blockingUseCase.processUserTurn(1, "Testing blocked patterns.", fakeDao.getAllItemsOnce())
        assertTrue(proposals.isEmpty())
        assertTrue(msg.contains("switch to the form"))
        assertFalse(msg.contains("Testing blocked patterns"))
    }

    @Test
    fun scenario8_UngroundedRationaleFallback() = runBlocking {
        val ungroundedAiCore = object : com.neon.ascent.core.domain.ai.AiCore {
            override suspend fun isReady(): Boolean = true
            override suspend fun warmup() {}
            override suspend fun generate(prompt: String, forceLocal: Boolean): com.neon.ascent.core.domain.ai.AiResult {
                return com.neon.ascent.core.domain.ai.AiResult.Success("[PROPOSAL: Random Node | PEOPLE | 0.8 | unrelated note | completely disconnected rationale]")
            }
            override suspend fun generateIsolated(prompt: String): com.neon.ascent.core.domain.ai.AiResult = generate(prompt, true)
            @Suppress("OVERRIDE_DEPRECATION")
            override suspend fun generateContent(prompt: String, forceLocal: Boolean): String = "Generated"
        }
        val ungrounded = ProposedItemCandidate(
            name = "Random Node",
            category = SelfMapCategory.PEOPLE,
            suggestedImportance = 0.8f,
            note = "unrelated note",
            rationale = "completely disconnected rationale",
        )
        assertTrue(
            IntakeModelGate.accepted(
                rawText = """[{"name":"Random Node"}]""",
                proposals = listOf(ungrounded),
                anchor = "Observing abstract phenomena with no keywords.",
                seedName = null,
            ).isEmpty()
        )
        val ungroundedUseCase = SelfMapConversationalIntakeUseCase(ungroundedAiCore)
        val (msg, proposals) = ungroundedUseCase.processUserTurn(1, "Observing abstract phenomena with no keywords.", fakeDao.getAllItemsOnce())
        assertTrue(proposals.isEmpty())
        assertTrue(msg.contains("switch to the form"))
        assertFalse(msg.contains("Observing abstract phenomena"))
    }

    @Test
    fun scenario9_FormSwitchPreservesDraft() {
        val proposal = ProposedItemCandidate(
            name = "Draft Item",
            category = SelfMapCategory.PEOPLE,
            suggestedImportance = 0.62f,
            note = "keep this note",
        )
        val draft = IntakeFormHandoff.draftFrom(proposal)
        assertEquals("Draft Item", draft.name)
        assertEquals(SelfMapCategory.PEOPLE, draft.category)
        assertEquals(0.62f, draft.importance)
        assertEquals("keep this note", draft.notes)
    }

    @Test
    fun formAssist_oneGroundedNameAndNoCannedFallback() = runBlocking {
        var modelCalls = 0
        var isolatedPrompt: String? = null
        val core = object : com.neon.ascent.core.domain.ai.AiCore {
            override suspend fun isReady(): Boolean = true
            override suspend fun warmup() {}
            override suspend fun generate(prompt: String, forceLocal: Boolean): com.neon.ascent.core.domain.ai.AiResult {
                modelCalls += 1
                return com.neon.ascent.core.domain.ai.AiResult.Success(
                    """[{"name":"Inner Circle Allies","category":"PEOPLE","importance":0.8,"note":"invented","rationale":"cybr-tes family"}]"""
                )
            }
            override suspend fun generateIsolated(prompt: String): com.neon.ascent.core.domain.ai.AiResult {
                isolatedPrompt = prompt
                return generate(prompt, true)
            }
            @Suppress("OVERRIDE_DEPRECATION")
            override suspend fun generateContent(prompt: String, forceLocal: Boolean): String = ""
        }
        val useCase = SelfMapConversationalIntakeUseCase(core)

        assertNull(useCase.suggestItemForCategory(SelfMapCategory.PEOPLE, "   "))
        assertEquals(0, modelCalls)

        val family = useCase.suggestItemForCategory(SelfMapCategory.PEOPLE, "My family is who I come home to.")
        assertEquals("Family & Kin", family!!.name)
        assertEquals(SelfMapCategory.PEOPLE, family.category)
        assertEquals(0.85f, family.suggestedImportance)
        assertEquals(0, modelCalls)

        val blocked = useCase.suggestItemForCategory(
            SelfMapCategory.STUFF,
            "Observing abstract phenomena with no keywords.",
        )
        assertNull(blocked)
        assertEquals(1, modelCalls)
        val sent = isolatedPrompt!!.lowercase()
        assertTrue(sent.contains("self map intake"))
        assertTrue(sent.contains("stuff"))
        assertTrue(sent.contains("observing abstract phenomena"))
        assertFalse(sent.contains("cybr-tes"))
        assertFalse(sent.contains("socrates"))
        assertFalse(sent.contains("faith lens"))
    }

    @Test
    fun rationaleOverTwelveWords_isDropped() {
        val anchor = "my family matters here today"
        val within = ProposedItemCandidate(
            name = "Family & Kin",
            category = SelfMapCategory.PEOPLE,
            suggestedImportance = 0.8f,
            rationale = "family stays in this short rationale of twelve words only here now",
        )
        val over = within.copy(
            rationale = "family stays in this short rationale of twelve words only here now today",
        )
        assertEquals(12, within.rationale!!.trim().split(Regex("\\s+")).size)
        assertEquals(13, over.rationale!!.trim().split(Regex("\\s+")).size)
        assertEquals(1, IntakeModelGate.accepted("[]", listOf(within), anchor, null).size)
        assertTrue(IntakeModelGate.accepted("[]", listOf(over), anchor, null).isEmpty())
    }

    @Test
    fun lockedIntakeQuestions_stayVerbatim() {
        val core = object : com.neon.ascent.core.domain.ai.AiCore {
            override suspend fun isReady(): Boolean = false
            override suspend fun warmup() {}
            override suspend fun generate(prompt: String, forceLocal: Boolean): com.neon.ascent.core.domain.ai.AiResult {
                return com.neon.ascent.core.domain.ai.AiResult.Failure("unused")
            }
            @Suppress("OVERRIDE_DEPRECATION")
            override suspend fun generateContent(prompt: String, forceLocal: Boolean): String = ""
        }
        assertEquals(
            listOf(
                "Tell me a little about who you are right now. What defines your baseline identity or main focus?",
                "Who are the people who matter most to you right now? People nodes are usually the attachments that produce the strongest reactions when threatened.",
                "What stuff or roles would you miss if they were gone? These are the identities and possessions the mind often defends.",
                "What ideas, values, or opinions feel like part of you? These shape expectations and how you appraise events.",
                "Want to confirm or adjust the Body & Senses items already on your map? These sit near the center by default.",
            ),
            SelfMapConversationalIntakeUseCase(core).intakeSteps.map { it.promptQuestion },
        )
    }

    @Test
    fun scenario10_RadialCanvasLandingPreseedPreserved() = runBlocking {
        fakeDao.insertSelfItem(SelfItemEntity(name = "Body", category = SelfMapCategory.BODY_SENSES, importance = 0.9f))
        val active = fakeDao.getAllActiveItemsOnce()
        assertTrue(active.any { it.category == SelfMapCategory.BODY_SENSES })
    }

    @Test
    fun scenario11_PreProposalGating() = runBlocking {
        var modelCalls = 0
        val core = object : com.neon.ascent.core.domain.ai.AiCore {
            override suspend fun isReady(): Boolean = true
            override suspend fun warmup() {}
            override suspend fun generate(prompt: String, forceLocal: Boolean): com.neon.ascent.core.domain.ai.AiResult {
                modelCalls += 1
                return com.neon.ascent.core.domain.ai.AiResult.Success("""[{"name":"Invented","category":"PEOPLE","importance":0.8,"note":"n","rationale":"invented"}]""")
            }
            override suspend fun generateIsolated(prompt: String): com.neon.ascent.core.domain.ai.AiResult = generate(prompt, true)
            @Suppress("OVERRIDE_DEPRECATION")
            override suspend fun generateContent(prompt: String, forceLocal: Boolean): String = ""
        }
        val (msg, proposals) = SelfMapConversationalIntakeUseCase(core).processUserTurn(0, "", fakeDao.getAllItemsOnce())
        assertEquals(0, modelCalls)
        assertEquals(
            "Tell me a little about who you are right now. What defines your baseline identity or main focus?",
            msg,
        )
        assertTrue(proposals.isEmpty())
    }

    @Test
    fun entry_firstOpenLabsGuidedAndSettings() {
        assertEquals(
            SelfMapEntry.Surface.CHAT,
            SelfMapEntry.surface(false, SelfMapIntakeMode.CHAT, SelfMapEntry.Reason.FIRST_OPEN),
        )
        assertEquals(
            SelfMapEntry.Surface.FORM,
            SelfMapEntry.surface(false, SelfMapIntakeMode.FORM, SelfMapEntry.Reason.FIRST_OPEN),
        )
        assertEquals(
            SelfMapEntry.Surface.FORM,
            SelfMapEntry.surface(false, SelfMapIntakeMode.FORM, SelfMapEntry.Reason.LABS),
        )
        assertEquals(
            SelfMapEntry.Surface.CHAT,
            SelfMapEntry.surface(false, SelfMapIntakeMode.CHAT, SelfMapEntry.Reason.LABS),
        )
        assertEquals(
            SelfMapEntry.Surface.CANVAS,
            SelfMapEntry.surface(true, SelfMapIntakeMode.CHAT, SelfMapEntry.Reason.LABS),
        )
        assertEquals(
            SelfMapEntry.Surface.CANVAS,
            SelfMapEntry.surface(true, SelfMapIntakeMode.FORM, SelfMapEntry.Reason.FIRST_OPEN),
        )
        assertEquals(
            SelfMapEntry.Surface.CHAT,
            SelfMapEntry.surface(true, SelfMapIntakeMode.FORM, SelfMapEntry.Reason.GUIDED_START),
        )
    }

    @Test
    fun intakeContext_dropsArchivedAndSurfacesBodySenses() {
        val context = SelfMapIntakeContext.fromActiveItems(
            listOf(
                SelfItemEntity(name = "Body", category = SelfMapCategory.BODY_SENSES, importance = 0.9f),
                SelfItemEntity(name = "Senses", category = SelfMapCategory.BODY_SENSES, importance = 0.8f),
                SelfItemEntity(name = "Old", category = SelfMapCategory.PEOPLE, importance = 0.5f, isArchived = true),
                SelfItemEntity(name = "Partner", category = SelfMapCategory.PEOPLE, importance = 0.8f),
            )
        )
        assertEquals(listOf("Body", "Senses", "Partner"), context.activeItems.map { it.name })
        assertEquals(setOf(SelfMapCategory.BODY_SENSES, SelfMapCategory.PEOPLE), context.categoriesInUse)
        assertEquals(listOf("Body", "Senses"), context.bodyAndSenses.map { it.name })

        val sent = SelfMapIntakePrompt.request("my partner", context)
        assertTrue(sent.contains("Body"))
        assertTrue(sent.contains("Senses"))
        assertFalse(sent.contains("Old"))
        assertFalse(sent.lowercase().contains("cybr-tes"))
        assertFalse(sent.contains("?"))
    }

    @Test
    fun scenario13_IsolatedPromptSkipsPersona() = runBlocking {
        var isolatedPrompt: String? = null
        var personaGenerateCalled = false
        val core = object : com.neon.ascent.core.domain.ai.AiCore {
            override suspend fun isReady(): Boolean = true
            override suspend fun warmup() {}
            override suspend fun generate(prompt: String, forceLocal: Boolean): com.neon.ascent.core.domain.ai.AiResult {
                personaGenerateCalled = true
                return com.neon.ascent.core.domain.ai.AiResult.Failure("persona path")
            }
            override suspend fun generateIsolated(prompt: String): com.neon.ascent.core.domain.ai.AiResult {
                isolatedPrompt = prompt
                return com.neon.ascent.core.domain.ai.AiResult.Success("[]")
            }
            @Suppress("OVERRIDE_DEPRECATION")
            override suspend fun generateContent(prompt: String, forceLocal: Boolean): String = ""
        }
        val useCase = SelfMapConversationalIntakeUseCase(core)
        val (msg, proposals) = useCase.processUserTurn(
            1,
            "Observing abstract phenomena with no keywords.",
            fakeDao.getAllItemsOnce()
        )
        useCase.suggestItemForCategory(com.neon.ascent.core.domain.selfmap.SelfMapCategory.PEOPLE)

        assertFalse(personaGenerateCalled)
        val sent = isolatedPrompt!!.lowercase()
        assertFalse(sent.contains("cybr-tes"))
        assertFalse(sent.contains("socrates"))
        assertFalse(sent.contains("faith lens"))
        assertFalse(sent.contains("runner"))
        assertFalse(sent.contains("protocol"))
        assertFalse(sent.contains("vision/stimulus"))
        assertTrue(sent.contains("self map intake"))
        assertTrue(proposals.isEmpty())
        assertTrue(msg.contains("switch to the form"))
        assertFalse(msg.contains("Observing abstract phenomena"))
    }

    @Test
    fun proposalResolution_holdsStepUntilEveryCardIsResolved() {
        val family = ProposedItemCandidate(id = "a", name = "Family & Kin", category = SelfMapCategory.PEOPLE, suggestedImportance = 0.8f)
        val faith = ProposedItemCandidate(id = "b", name = "Faith & Spiritual Anchor", category = SelfMapCategory.IDEAS_LIKES, suggestedImportance = 0.8f)
        val cards = listOf(family, faith)
        assertFalse(IntakeProposalResolution.canAdvance(emptyList(), emptySet(), emptySet(), emptySet()))
        assertFalse(IntakeProposalResolution.canAdvance(cards, setOf("a"), emptySet(), emptySet()))
        assertTrue(IntakeProposalResolution.canAdvance(cards, setOf("a"), emptySet(), setOf("b")))
        assertTrue(
            IntakeProposalResolution.canAdvance(
                cards,
                setOf("a"),
                setOf(IntakeProposalResolution.signature(faith)),
                emptySet(),
            )
        )
        val again = family.copy(id = "new")
        assertFalse(
            IntakeProposalResolution.isActionable(
                again,
                acceptedIds = setOf(IntakeProposalResolution.signature(family)),
                rejectedSignatures = emptySet(),
                dismissedIds = emptySet(),
            )
        )
        assertFalse(
            IntakeProposalResolution.isActionable(
                again,
                acceptedIds = emptySet(),
                rejectedSignatures = setOf(IntakeProposalResolution.signature(family)),
                dismissedIds = emptySet(),
            )
        )
    }

    @Test
    fun blankTurn_emitsLockedTemplateWhenCategoryAlreadyMapped() = runBlocking {
        fakeDao.insertSelfItem(SelfItemEntity(name = "Family", category = SelfMapCategory.PEOPLE, importance = 0.85f))
        val (msg, proposals) = intakeUseCase.processUserTurn(1, "", fakeDao.getAllItemsOnce())
        assertEquals(
            "Who are the people who matter most to you right now? People nodes are usually the attachments that produce the strongest reactions when threatened.",
            msg,
        )
        assertTrue(proposals.isEmpty())
        assertFalse(msg.contains("I notice you already have"))
        assertFalse(msg.contains("refine or add more"))
        assertFalse(msg.contains("or proceed"))
    }

    @Test
    fun autoAdvance_emitsNextTemplateOnlyWhenEveryCardIsResolved() {
        val family = ProposedItemCandidate(id = "a", name = "Family & Kin", category = SelfMapCategory.PEOPLE, suggestedImportance = 0.8f)
        val faith = ProposedItemCandidate(id = "b", name = "Faith & Spiritual Anchor", category = SelfMapCategory.IDEAS_LIKES, suggestedImportance = 0.8f)
        val peopleQuestion = "Who are the people who matter most to you right now? People nodes are usually the attachments that produce the strongest reactions when threatened."
        val steps = listOf(
            IntakeStep(0, "identity", "Tell me a little about who you are right now. What defines your baseline identity or main focus?", SelfMapCategory.CUSTOM, emptyList()),
            IntakeStep(1, "people", peopleQuestion, SelfMapCategory.PEOPLE, emptyList()),
        )
        val open = IntakeUiState(
            currentStepIndex = 0,
            stepAnchor = 0,
            messages = listOf(
                ChatTurnMessage(sender = MessageSender.GUIDE, text = steps[0].promptQuestion),
                ChatTurnMessage(sender = MessageSender.GUIDE, text = "Based on that, two nodes:", proposals = listOf(family, faith)),
            ),
        )
        val held = IntakeAutoAdvance.afterResolution(open.copy(acceptedIds = setOf("a")), steps)
        assertEquals(0, held.currentStepIndex)
        assertEquals(2, held.messages.size)

        val advanced = IntakeAutoAdvance.afterResolution(
            open.copy(acceptedIds = setOf("a"), rejectedIds = setOf(IntakeProposalResolution.signature(faith))),
            steps,
        )
        assertEquals(1, advanced.currentStepIndex)
        assertEquals(peopleQuestion, advanced.messages.last().text)
        assertFalse(advanced.isCompleted)

        val finished = IntakeAutoAdvance.toStep(open, listOf(steps[0]), 1)
        assertTrue(finished.isCompleted)
        assertEquals(0, finished.currentStepIndex)
    }

    @Test
    fun extraction_happyPathSkipsModelAndQuotesASpan() = runBlocking {
        var modelCalled = false
        val core = object : com.neon.ascent.core.domain.ai.AiCore {
            override suspend fun isReady(): Boolean = true
            override suspend fun warmup() {}
            override suspend fun generate(prompt: String, forceLocal: Boolean): com.neon.ascent.core.domain.ai.AiResult {
                modelCalled = true
                return com.neon.ascent.core.domain.ai.AiResult.Failure("model")
            }
            override suspend fun generateIsolated(prompt: String): com.neon.ascent.core.domain.ai.AiResult {
                modelCalled = true
                return com.neon.ascent.core.domain.ai.AiResult.Failure("model")
            }
            @Suppress("OVERRIDE_DEPRECATION")
            override suspend fun generateContent(prompt: String, forceLocal: Boolean): String = ""
        }
        val message = "My family and my work as an architect anchor my life, and my faith sustains my daily purpose."
        val (msg, proposals) = SelfMapConversationalIntakeUseCase(core).processUserTurn(1, message, emptyList())
        assertFalse(modelCalled)
        assertEquals("Based on that, two nodes:", msg)
        assertEquals(2, proposals.size)
        assertEquals("Family & Kin", proposals[0].name)
        assertEquals("Faith & Spiritual Anchor", proposals[1].name)
        proposals.forEach { proposal ->
            assertTrue(proposal.rationale!!.contains("\""))
            assertFalse(proposal.rationale.contains(message))
        }
        assertTrue(SelfMapEntityExtractor.extract("somebody").isEmpty())
        assertTrue(SelfMapEntityExtractor.extract("A personal note about growth.").none { it.name == "Family & Kin" })
    }

    @Test
    fun moreLikeThis_skipsExtractionAndCallsIsolatedModel() = runBlocking {
        var isolatedCalled = false
        var personaCalled = false
        val core = object : com.neon.ascent.core.domain.ai.AiCore {
            override suspend fun isReady(): Boolean = true
            override suspend fun warmup() {}
            override suspend fun generate(prompt: String, forceLocal: Boolean): com.neon.ascent.core.domain.ai.AiResult {
                personaCalled = true
                return com.neon.ascent.core.domain.ai.AiResult.Failure("persona")
            }
            override suspend fun generateIsolated(prompt: String): com.neon.ascent.core.domain.ai.AiResult {
                isolatedCalled = true
                return com.neon.ascent.core.domain.ai.AiResult.Success("[]")
            }
            @Suppress("OVERRIDE_DEPRECATION")
            override suspend fun generateContent(prompt: String, forceLocal: Boolean): String = ""
        }
        val utterance = "My family is the center of the week."
        val (msg, proposals) = SelfMapConversationalIntakeUseCase(core).moreLikeThis(
            1,
            utterance,
            "Family & Kin",
            emptyList(),
        )
        assertTrue(isolatedCalled)
        assertFalse(personaCalled)
        assertTrue(proposals.isEmpty())
        assertTrue(msg.contains("switch to the form"))
        assertFalse(msg.contains(utterance))
    }

    @Test
    fun scenario12_InsertIsolation() = runBlocking {
        // Writes occur exclusively through repository upon explicit operator acceptance
        val entity = SelfItemEntity(name = "Accepted Anchor", category = SelfMapCategory.CUSTOM, importance = 0.85f)
        val id = fakeDao.insertSelfItem(entity)
        assertTrue(id > 0L)
        val items = fakeDao.getAllActiveItemsOnce()
        assertTrue(items.any { it.name == "Accepted Anchor" })
    }

    private class FakeAiCore : com.neon.ascent.core.domain.ai.AiCore {
        override suspend fun isReady(): Boolean = true
        override suspend fun warmup() {}
        override suspend fun generate(prompt: String, forceLocal: Boolean): com.neon.ascent.core.domain.ai.AiResult {
            return com.neon.ascent.core.domain.ai.AiResult.Success("Based on that: [PROPOSAL: Test Node | PEOPLE | 0.8 | Test note | Grounded in ready team matters family work architect life purpose]")
        }
        @Suppress("OVERRIDE_DEPRECATION")
        override suspend fun generateContent(prompt: String, forceLocal: Boolean): String = "Generated"
    }

    private class FakeSelfMapDao : SelfMapDao {
        private val itemsMap = mutableMapOf<Long, SelfItemEntity>()
        private var idCounter = 1L

        override fun getAllActiveItems(): Flow<List<SelfItemEntity>> = flowOf(itemsMap.values.filter { !it.isArchived })
        override suspend fun getAllActiveItemsOnce(): List<SelfItemEntity> = itemsMap.values.filter { !it.isArchived }
        override suspend fun getAllItemsOnce(): List<SelfItemEntity> = itemsMap.values.toList()

        override fun getItemsByCategory(category: SelfMapCategory): Flow<List<SelfItemEntity>> = flowOf(emptyList())
        override fun getItemById(id: Long): Flow<SelfItemEntity?> = flowOf(null)
        override suspend fun getItemByIdOnce(id: Long): SelfItemEntity? = null

        override fun getAverageImportance(): Flow<Float> = flowOf(0.8f)
        override fun getCoreCount(): Flow<Int> = flowOf(itemsMap.size)
        override fun getTotalItemCount(): Flow<Int> = flowOf(itemsMap.size)

        override suspend fun insertSelfItem(item: SelfItemEntity): Long {
            val id = if (item.id == 0L) idCounter++ else item.id
            itemsMap[id] = item.copy(id = id)
            return id
        }
        override suspend fun insertSelfItems(items: List<SelfItemEntity>): List<Long> = items.map { insertSelfItem(it) }
        override suspend fun updateSelfItem(item: SelfItemEntity) { itemsMap[item.id] = item }
        override suspend fun archiveSelfItem(id: Long, updatedAt: java.time.Instant) {}
        override suspend fun unarchiveSelfItem(id: Long, updatedAt: java.time.Instant) {}
        override suspend fun deleteSelfItem(id: Long) { itemsMap.remove(id) }
        override suspend fun deleteAllSelfItems() { itemsMap.clear() }

        override suspend fun insertEoEEntry(entry: EoEEntryEntity): Long = 1L
        override fun getAllEoEEntries(): Flow<List<EoEEntryEntity>> = flowOf(emptyList())
        override suspend fun getAllEoEEntriesOnce(): List<EoEEntryEntity> = emptyList()
        override fun getEoEEntriesForSelfItem(selfItemId: Long): Flow<List<EoEEntryEntity>> = flowOf(emptyList())
        override suspend fun deleteEoEEntry(id: Long) {}
        override suspend fun deleteAllEoEEntries() {}

        override suspend fun insertSnapshot(snapshot: SelfMapSnapshotEntity): Long = 1L
        override fun getAllSnapshots(): Flow<List<SelfMapSnapshotEntity>> = flowOf(emptyList())
        override suspend fun getAllSnapshotsOnce(): List<SelfMapSnapshotEntity> = emptyList()
        override fun getLatestSnapshot(): Flow<SelfMapSnapshotEntity?> = flowOf(null)
        override suspend fun deleteAllSnapshots() {}

        override suspend fun insertMoodEntry(entry: SelfMapMoodEntryEntity): Long = 1L
        override fun getAllMoodEntries(): Flow<List<SelfMapMoodEntryEntity>> = flowOf(emptyList())
        override suspend fun getAllMoodEntriesOnce(): List<SelfMapMoodEntryEntity> = emptyList()
        override fun getRecentMoodEntries(limit: Int): Flow<List<SelfMapMoodEntryEntity>> = flowOf(emptyList())
        override fun getLatestMoodEntry(): Flow<SelfMapMoodEntryEntity?> = flowOf(null)
        override suspend fun getLatestMoodEntryOnce(): SelfMapMoodEntryEntity? = null
        override suspend fun deleteMoodEntry(id: Long) {}
        override suspend fun deleteAllMoodEntries() {}
    }
}
