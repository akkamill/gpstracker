package com.example.gpstracker.ui.tripdetail

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gpstracker.R
import com.example.gpstracker.ui.components.EmptyState
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun TripDetailRoute(
    tripId: String,
    onBack: () -> Unit,
    onSetFuelPrice: () -> Unit,
    viewModel: TripDetailViewModel = koinViewModel(key = tripId) { parametersOf(tripId) },
) {
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val selected by viewModel.selectedEventId.collectAsStateWithLifecycle()
    val trip = detail
    if (trip == null) {
        TripNotFound(onBack)
    } else {
        TripDetailScreen(
            state = trip,
            selectedEventId = selected,
            onEventClick = viewModel::onEventClick,
            onSetFuelPrice = onSetFuelPrice,
            onBack = onBack,
        )
    }
}

/** Shown when the trip is not in the history (any more), instead of someone else's sample trip. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripNotFound(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back_cd))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        EmptyState(
            icon = Icons.Rounded.SearchOff,
            title = stringResource(R.string.trip_not_found_title),
            message = stringResource(R.string.trip_not_found_body),
            modifier = Modifier.padding(padding),
            actionLabel = stringResource(R.string.trip_not_found_action),
            onAction = onBack,
        )
    }
}
