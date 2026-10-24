package com.example.curio.data.user

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch

/** Application-scoped, so badges unlock on every screen. The snapshot is only a trigger; evaluateNow() reads fresh state. */
class AchievementTracker(
    private val scope: CoroutineScope,
    private val activity: UserActivityRepository,
    private val achievements: AchievementRepository,
) {
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            activity.observeSnapshot().conflate().collect { achievements.evaluateNow() }
        }
    }

    fun stop() { job?.cancel() }
}
