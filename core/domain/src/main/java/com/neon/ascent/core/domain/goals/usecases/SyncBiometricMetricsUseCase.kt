package com.neon.ascent.core.domain.goals.usecases

import com.neon.ascent.core.domain.goals.models.MetricType
import com.neon.ascent.core.domain.health.HealthManager
import com.neon.ascent.core.domain.repository.AscensionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SyncBiometricMetricsUseCase @Inject constructor(
    private val repository: AscensionRepository,
    private val healthManager: HealthManager
) {
    suspend operator fun invoke() {
        if (!healthManager.isAvailableAndHasPermissions()) return
        
        val snapshot = healthManager.readRecentData(1)
        val directives = repository.getAllDirectives().first()
        
        directives.forEach { directive ->
            val updatedMetrics = directive.successMetrics.map { metric ->
                if (metric.type == MetricType.BIOMETRIC && metric.biometricKey != null) {
                    val newValue = when (metric.biometricKey) {
                        "steps" -> snapshot.steps.sumOf { it.count }.toFloat()
                        "sleep_hours" -> {
                            val totalMillis = snapshot.sleep.sumOf { 
                                java.time.Duration.between(it.startTime, it.endTime).toMillis() 
                            }
                            totalMillis / (1000f * 60 * 60)
                        }
                        "hrv" -> snapshot.hrv.map { it.heartRateVariabilityMillis }.average().toFloat()
                        else -> metric.currentValue
                    }
                    metric.copy(currentValue = newValue)
                } else {
                    metric
                }
            }
            
            if (updatedMetrics != directive.successMetrics) {
                repository.updateDirective(directive.copy(successMetrics = updatedMetrics))
            }
        }
    }
}
