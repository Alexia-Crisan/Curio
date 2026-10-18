package com.example.curio.hardware

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

class HapticsController(
    var enabled: Boolean = true
) {
    fun confirm(hapticFeedback: HapticFeedback?) {
        if (enabled) {
            hapticFeedback?.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    fun tick(hapticFeedback: HapticFeedback?) {
        if (enabled) {
            hapticFeedback?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
}
