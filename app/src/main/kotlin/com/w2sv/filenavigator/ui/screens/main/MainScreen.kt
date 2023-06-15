package com.w2sv.filenavigator.ui.screens.main

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.w2sv.filenavigator.R
import com.w2sv.filenavigator.service.FileNavigatorService
import com.w2sv.filenavigator.ui.AppSnackbar
import com.w2sv.filenavigator.ui.theme.RailwayText
import com.w2sv.filenavigator.ui.theme.md_negative
import com.w2sv.filenavigator.ui.theme.md_positive

@Composable
fun MainScreen(mainScreenViewModel: MainScreenViewModel = viewModel()) {
    val context = LocalContext.current

    var showSettingsDialog by rememberSaveable {
        mutableStateOf(false)
    }
        .apply {
            if (value) {
                SettingsDialog(closeDialog = { value = false })
            }
        }

    Scaffold(snackbarHost = {
        SnackbarHost(mainScreenViewModel.snackbarHostState) { snackbarData ->
            AppSnackbar(snackbarData = snackbarData)
        }
    }) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceAround
            ) {
                Spacer(modifier = Modifier.weight(0.05f))
                Box(modifier = Modifier.weight(0.15f), contentAlignment = Alignment.CenterStart) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        RailwayText(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        SettingsDialogButton(onClick = { showSettingsDialog = true })
                    }
                }

                Box(modifier = Modifier.weight(0.7f), contentAlignment = Alignment.Center) {
                    FileTypeSelectionColumn(Modifier.fillMaxHeight())
                }

                Box(modifier = Modifier.weight(0.25f), contentAlignment = Alignment.Center) {
                    val unconfirmedConfigurationChangesPresent by mainScreenViewModel.unconfirmedNavigatorConfiguration.statesDissimilar.collectAsState()

                    this@Column.AnimatedVisibility(
                        visible = !unconfirmedConfigurationChangesPresent,
                        enter = fadeIn() + slideInHorizontally(),
                        exit = fadeOut() + slideOutHorizontally()
                    ) {
                        StartNavigatorButton(
                            modifier = Modifier
                                .width(220.dp)
                                .height(70.dp)
                        )
                    }

                    this@Column.AnimatedVisibility(
                        visible = unconfirmedConfigurationChangesPresent,
                        enter = fadeIn() + slideInHorizontally(initialOffsetX = { it / 2 }),
                        exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it / 2 })
                    ) {
                        NavigatorConfigurationButtons()
                    }
                }
            }
        }
    }

    EventualManageExternalStorageRational()

    BackHandler {
        mainScreenViewModel.onBackPress(context)
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun StartNavigatorButton(
    modifier: Modifier = Modifier,
    mainScreenViewModel: MainScreenViewModel = viewModel()
) {
    val context = LocalContext.current

    val isNavigatorRunning by mainScreenViewModel.isNavigatorRunning.collectAsState()
    val permissionState =
        rememberPermissionState(permission = Manifest.permission.READ_EXTERNAL_STORAGE) { granted ->
            if (granted) {
                FileNavigatorService.start(context)
            }
        }

    Crossfade(
        targetState = isNavigatorRunning,
        animationSpec = tween(durationMillis = 1250, delayMillis = 250, easing = EaseOutCubic),
        label = ""
    ) {
        val properties = when (it) {
            true -> NavigatorButtonProperties(
                md_negative,
                R.drawable.ic_stop_24,
                R.string.stop_navigator
            ) { FileNavigatorService.stop(context) }

            false -> NavigatorButtonProperties(
                md_positive,
                R.drawable.ic_start_24,
                R.string.start_navigator
            ) {
                when (permissionState.status.isGranted) {
                    true -> FileNavigatorService.start(context)
                    false -> permissionState.launchPermissionRequest()
                }
            }
        }

        ElevatedButton(
            onClick = properties.onClick,
            modifier = modifier
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    painter = painterResource(id = properties.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = properties.color
                )
                RailwayText(
                    text = stringResource(id = properties.labelRes),
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

private data class NavigatorButtonProperties(
    val color: Color,
    @DrawableRes val iconRes: Int,
    @StringRes val labelRes: Int,
    val onClick: () -> Unit
)