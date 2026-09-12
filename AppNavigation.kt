package com.neon.ascent

import androidx.compose.runtime.Composable
import com.neon.ascent.feature.charactercreation.CreationViewModel
import com.neon.ascent.feature.dashboard.DashboardViewModel
import com.neon.ascent.feature.notifications.ui.NotificationPermissionViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun RootAppNavigation(
    creationViewModel: CreationViewModel = hiltViewModel(),
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    notificationViewModel: NotificationPermissionViewModel = hiltViewModel(),
    workoutViewModel: com.neon.ascent.feature.workout.ui.WorkoutViewModel = hiltViewModel(),
    loadingViewModel: com.neon.ascent.feature.loading.LoadingViewModel = hiltViewModel()
) {
    AppNavigation(
        creationViewModel = creationViewModel,
        dashboardViewModel = dashboardViewModel,
        notificationViewModel = notificationViewModel,
        workoutViewModel = workoutViewModel,
        loadingViewModel = loadingViewModel
    )
}
