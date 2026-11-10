package com.neon.ascent.feature.dashboard.selfmap

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.feature.dashboard.selfmap.intake.SelfMapIntakeTerminal
import com.neon.ascent.feature.dashboard.selfmap.intake.SelfMapIntakeViewModel

@Composable
fun SelfMapConversationalIntakeScreen(
    viewModel: SelfMapViewModel,
    onFinishIntake: () -> Unit,
    onSkipToManual: () -> Unit,
    modifier: Modifier = Modifier,
    intakeViewModel: SelfMapIntakeViewModel = hiltViewModel()
) {
    val uiState by intakeViewModel.uiState.collectAsState()

    if (uiState.isCompleted) {
        onFinishIntake()
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        SelfMapIntakeTerminal(
            uiState = uiState,
            onSendMessage = { intakeViewModel.submitUserReply(it) },
            onSwitchToForm = { proposal ->
                if (proposal != null) {
                    viewModel.updateDraftForm(
                        name = proposal.name,
                        category = proposal.category,
                        importance = proposal.suggestedImportance
                    )
                }
                viewModel.switchSessionIntakeMode(com.neon.ascent.core.domain.selfmap.SelfMapIntakeMode.FORM)
                onSkipToManual()
            },
            onAccept = { intakeViewModel.acceptProposal(it) },
            onReject = { intakeViewModel.rejectProposal(it) },
            onAcceptAndEdit = { proposal ->
                viewModel.updateDraftForm(
                    name = proposal.name,
                    category = proposal.category,
                    importance = proposal.suggestedImportance
                )
                // We also consider it accepted in the session so it doesn't reappear
                intakeViewModel.acceptProposal(proposal)
                viewModel.switchSessionIntakeMode(com.neon.ascent.core.domain.selfmap.SelfMapIntakeMode.FORM)
                onSkipToManual()
            },
            onMoreLikeThis = { intakeViewModel.requestMoreLikeThis(it) },
            onSkipCategory = { intakeViewModel.skipCurrentStep() }
        )
    }
}
