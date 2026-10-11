package com.example.gpstracker.ui.model

/** One category in the you-vs-benchmark bar chart. Values are events per 100 km. */
data class ComparisonRowUi(
    val type: EventType,
    val label: String,
    val you: Double,
    val benchmark: Double,
)

data class InsightsUiState(
    val period: InsightsPeriod = InsightsPeriod.Week,
    /** False when the period has fewer than 3 trips. */
    val hasEnoughData: Boolean = true,
    val yourScore: Int = 0,
    val benchmarkScore: Int = 0,
    val verdict: String = "",
    val trendLabels: List<String> = emptyList(),
    /** Null means no trips in that bucket (a gap in the line). */
    val yourTrend: List<Int?> = emptyList(),
    val benchmarkTrend: List<Int> = emptyList(),
    val comparison: List<ComparisonRowUi> = emptyList(),
    val fuel: FuelEstimateUi? = null,
    val savedVsPreviousLabel: String? = null,
    /** True when [savedVsPreviousLabel] describes extra waste rather than a saving. */
    val savedIsWorse: Boolean = false,
    val projectedYearlySavingLabel: String? = null,
    val distanceKm: Double = 0.0,
)
