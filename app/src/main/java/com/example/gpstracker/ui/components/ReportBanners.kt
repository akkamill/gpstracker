package com.example.gpstracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gpstracker.R
import com.example.gpstracker.ui.model.GeoPointUi
import com.example.gpstracker.ui.model.ReportAheadUi
import com.example.gpstracker.ui.model.ReportKind
import com.example.gpstracker.ui.model.RoadReportUi
import com.example.gpstracker.ui.theme.DriveMetricsTheme

private const val SLIDE_MS = 250

/** Keeps showing the last value while a banner slides out after its value became null. */
@Composable
private fun <T : Any> rememberLast(value: T?): T? {
    val last = remember { mutableStateOf(value) }
    if (value != null) last.value = value
    return last.value
}

/** "Police ahead, 400 m". Stays while the report is ahead; the distance counts down. */
@Composable
fun ReportAheadBanner(ahead: ReportAheadUi?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = ahead != null,
        modifier = modifier,
        enter = slideInVertically(tween(SLIDE_MS)) { -it },
        exit = slideOutVertically(tween(SLIDE_MS)) { -it },
    ) {
        rememberLast(ahead)?.let { ReportAheadContent(it) }
    }
}

@Composable
private fun ReportAheadContent(ahead: ReportAheadUi) {
    val kind = stringResource(ahead.kind.label())
    val title = stringResource(R.string.report_ahead_title, kind)
    val detail = if (ahead.roadLabel.isEmpty()) {
        ahead.distanceLabel
    } else {
        stringResource(R.string.report_ahead_detail, ahead.distanceLabel, ahead.roadLabel)
    }
    BannerRow(
        kind = ahead.kind,
        modifier = Modifier.semantics(mergeDescendants = true) {
            liveRegion = LiveRegionMode.Polite
            contentDescription = "$title, $detail"
        },
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** After passing someone's report: "Police on Savanorių pr. Still there?" with two big answers. */
@Composable
fun StillTherePrompt(report: RoadReportUi?, onAnswer: (String, Boolean) -> Unit, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = report != null,
        modifier = modifier,
        enter = slideInVertically(tween(SLIDE_MS)) { it },
        exit = slideOutVertically(tween(SLIDE_MS)) { it },
    ) {
        rememberLast(report)?.let { StillThereContent(it, onAnswer) }
    }
}

@Composable
private fun StillThereContent(report: RoadReportUi, onAnswer: (String, Boolean) -> Unit) {
    val kind = stringResource(report.kind.label())
    val title = if (report.roadLabel.isEmpty()) kind else stringResource(R.string.report_on_road, kind, report.roadLabel)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.medium)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        ) {
            Icon(report.kind.icon(), contentDescription = null, tint = report.kind.color(), modifier = Modifier.size(28.dp))
            Column(Modifier.padding(start = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    stringResource(R.string.report_still_there_question),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { onAnswer(report.id, true) },
                modifier = Modifier.weight(1f).heightIn(min = 56.dp),
            ) { Text(stringResource(R.string.report_still_there_yes)) }
            OutlinedButton(
                onClick = { onAnswer(report.id, false) },
                // the theme's outline color matches this background, so the border needs more contrast here
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.weight(1f).heightIn(min = 56.dp),
            ) { Text(stringResource(R.string.report_still_there_no)) }
        }
    }
}

/** Short thank-you after the driver reported something. */
@Composable
fun ReportSentBanner(kind: ReportKind?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = kind != null,
        modifier = modifier,
        enter = slideInVertically(tween(SLIDE_MS)) { -it },
        exit = slideOutVertically(tween(SLIDE_MS)) { -it },
    ) {
        rememberLast(kind)?.let {
            val text = stringResource(R.string.report_sent, stringResource(it.label()))
            BannerRow(
                kind = it,
                modifier = Modifier.semantics(mergeDescendants = true) {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = text
                },
            ) {
                Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun BannerRow(kind: ReportKind, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.medium)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(kind.icon(), contentDescription = null, tint = kind.color(), modifier = Modifier.size(28.dp))
        Column(Modifier.padding(start = 12.dp)) { content() }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1417)
@Composable
private fun ReportAheadPreview() {
    DriveMetricsTheme { ReportAheadBanner(ReportAheadUi("p", ReportKind.Police, "400 m", "Savanorių pr.")) }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1417)
@Composable
private fun StillTherePreview() {
    DriveMetricsTheme {
        StillTherePrompt(
            RoadReportUi("p", ReportKind.Police, GeoPointUi(54.9, 23.9), "Savanorių pr.", mine = false),
            onAnswer = { _, _ -> },
        )
    }
}
