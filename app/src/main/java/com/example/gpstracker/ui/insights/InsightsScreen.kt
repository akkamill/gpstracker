package com.example.gpstracker.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gpstracker.R
import com.example.gpstracker.data.fake.PreviewStates
import com.example.gpstracker.ui.components.AccountAvatar
import com.example.gpstracker.ui.components.EmptyState
import com.example.gpstracker.ui.components.FuelCard
import com.example.gpstracker.ui.components.GuestBanner
import com.example.gpstracker.ui.components.InfoBottomSheet
import com.example.gpstracker.ui.components.charts.ChartLegend
import com.example.gpstracker.ui.components.charts.ComparisonBarChart
import com.example.gpstracker.ui.components.charts.TrendLineChart
import com.example.gpstracker.ui.model.InsightsPeriod
import com.example.gpstracker.ui.model.InsightsUiState
import com.example.gpstracker.ui.theme.Concrete
import com.example.gpstracker.ui.theme.DriveMetricsTheme
import com.example.gpstracker.ui.theme.NightBlue

/**
 * You against a simulated benchmark driver: scores, trend, event categories and
 * the fuel estimate. Everything compared or estimated is labelled as such.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    state: InsightsUiState,
    isGuest: Boolean,
    onPeriodChange: (InsightsPeriod) -> Unit,
    onOpenSettings: () -> Unit,
    onSetFuelPrice: () -> Unit,
    onCreateAccount: () -> Unit,
    onAboutBenchmark: () -> Unit,
    modifier: Modifier = Modifier,
    avatarInitials: String? = null,
    onGoToDrive: () -> Unit = {},
) {
    var showBenchmarkInfo by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.insights_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            AccountAvatar(initials = avatarInitials, onClick = onOpenSettings)
        }
        if (isGuest) GuestBanner(onCreateAccount)

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = state.period == InsightsPeriod.Week,
                onClick = { onPeriodChange(InsightsPeriod.Week) },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
            ) { Text(stringResource(R.string.period_week)) }
            SegmentedButton(
                selected = state.period == InsightsPeriod.Month,
                onClick = { onPeriodChange(InsightsPeriod.Month) },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
            ) { Text(stringResource(R.string.period_month)) }
        }

        if (!state.hasEnoughData) {
            EmptyState(
                icon = Icons.Rounded.Insights,
                title = stringResource(R.string.insights_empty_title),
                message = stringResource(
                    if (state.period == InsightsPeriod.Week) R.string.insights_empty_week else R.string.insights_empty_month,
                ),
                actionLabel = stringResource(R.string.insights_go_to_drive),
                onAction = onGoToDrive,
            )
        } else {
            ScoreComparison(state, onInfoClick = {
                showBenchmarkInfo = true
                onAboutBenchmark()
            })

            Text(
                text = stringResource(R.string.insights_trend_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            ChartLegend()
            TrendLineChart(
                labels = state.trendLabels,
                you = state.yourTrend,
                benchmark = state.benchmarkTrend,
            )

            Text(
                text = stringResource(R.string.insights_compare_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            ComparisonBarChart(state.comparison)
        }

        FuelCard(
            estimate = state.fuel,
            savedLabel = state.savedVsPreviousLabel,
            projectionLabel = state.projectedYearlySavingLabel,
            onSetPrice = onSetFuelPrice,
            savedIsWorse = state.savedIsWorse,
        )
    }

    if (showBenchmarkInfo) {
        InfoBottomSheet(
            title = stringResource(R.string.insights_benchmark),
            text = stringResource(R.string.benchmark_info),
            onDismiss = { showBenchmarkInfo = false },
        )
    }
}

@Composable
private fun ScoreComparison(state: InsightsUiState, onInfoClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column {
                Text(
                    text = state.yourScore.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    color = NightBlue,
                )
                Text(
                    text = stringResource(R.string.insights_you),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column {
                Text(
                    text = state.benchmarkScore.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    color = Concrete,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.insights_benchmark),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    IconButton(onClick = onInfoClick) {
                        Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.benchmark_info_cd))
                    }
                }
            }
        }
        Text(
            text = state.verdict,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private const val BG = 0xFF0F1417
private const val PHONE = "spec:width=411dp,height=1500dp"

@Composable
private fun PreviewScreen(state: InsightsUiState, isGuest: Boolean = false) {
    DriveMetricsTheme {
        InsightsScreen(state, isGuest, {}, {}, {}, {}, {}, avatarInitials = "KA")
    }
}

@Preview(name = "Week, better", showBackground = true, backgroundColor = BG, device = PHONE)
@Composable
private fun InsightsWeekPreview() = PreviewScreen(PreviewStates.insightsWeekBetter)

@Preview(name = "Month, worse braking", showBackground = true, backgroundColor = BG, device = PHONE)
@Composable
private fun InsightsMonthPreview() = PreviewScreen(PreviewStates.insightsMonthWorse)

@Preview(name = "Not enough data", showBackground = true, backgroundColor = BG, device = "spec:width=411dp,height=891dp")
@Composable
private fun InsightsNotEnoughPreview() = PreviewScreen(PreviewStates.insightsNotEnoughData)

@Preview(name = "No fuel price", showBackground = true, backgroundColor = BG, device = PHONE)
@Composable
private fun InsightsNoPricePreview() = PreviewScreen(PreviewStates.insightsNoPrice)

@Preview(name = "Guest", showBackground = true, backgroundColor = BG, device = PHONE)
@Composable
private fun InsightsGuestPreview() = PreviewScreen(PreviewStates.insightsWeekBetter, isGuest = true)
