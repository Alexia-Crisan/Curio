package com.example.curio.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.curio.R
import com.example.curio.domain.EndCardTeaser
import com.example.curio.feature.tree.TreeVisuals
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun TeaserLine(teaser: EndCardTeaser, topicTitleOf: (String) -> String, modifier: Modifier = Modifier) {
    if (!teaser.hasContent) return
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    Column(modifier) {
        if (teaser.leavesToday > 0) {
            Text(
                pluralStringResource(R.plurals.teaser_leaves_today, teaser.leavesToday, teaser.leavesToday),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (teaser.stageChangedToday) {
                Text(
                    stringResource(R.string.teaser_stage, stringResource(TreeVisuals.stage(teaser.stageNow))),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
        teaser.next?.let { next ->
            Text(
                stringResource(R.string.teaser_next, topicTitleOf(next.topicId), next.date.format(formatter)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
