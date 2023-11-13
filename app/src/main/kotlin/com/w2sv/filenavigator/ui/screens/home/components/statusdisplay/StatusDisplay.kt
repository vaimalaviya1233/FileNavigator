package com.w2sv.filenavigator.ui.screens.home.components.statusdisplay

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.w2sv.filenavigator.R
import com.w2sv.filenavigator.ui.components.AppFontText
import com.w2sv.filenavigator.ui.screens.Screen
import com.w2sv.filenavigator.ui.sharedviewmodels.AppViewModel
import com.w2sv.filenavigator.ui.sharedviewmodels.NavigatorViewModel
import com.w2sv.filenavigator.ui.theme.AppColor
import com.w2sv.filenavigator.ui.theme.DefaultElevatedCardElevation
import com.w2sv.navigator.FileNavigator

@Composable
fun StatusDisplay(
    modifier: Modifier = Modifier,
    navigatorStateVM: NavigatorViewModel = viewModel(),
    appVM: AppViewModel = viewModel(),
    context: Context = LocalContext.current
) {
    val navigatorIsRunning by navigatorStateVM.isRunning.collectAsState()
    val startDateTime by navigatorStateVM.startDateTime.collectAsState()

    val statusTextProperties by remember {
        derivedStateOf {
            if (navigatorIsRunning) {
                StatusTextProperties(R.string.active, AppColor.success)
            } else {
                StatusTextProperties(R.string.inactive, AppColor.error)
            }
        }
    }

    val toggleNavigatorButtonProperties by remember {
        derivedStateOf {
            if (navigatorIsRunning)
                ToggleNavigatorButtonProperties(
                    AppColor.error,
                    R.drawable.ic_stop_24,
                    R.string.stop_navigator
                ) { FileNavigator.stop(context) }
            else
                ToggleNavigatorButtonProperties(
                    AppColor.success,
                    R.drawable.ic_start_24,
                    R.string.start_navigator
                ) { FileNavigator.start(context) }
        }
    }

    ElevatedCard(
        modifier = modifier.fillMaxSize(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = DefaultElevatedCardElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppFontText(
                    text = stringResource(R.string.navigator_status),
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.width(14.dp))
                AppFontText(
                    text = stringResource(id = statusTextProperties.textRes),
                    fontSize = 20.sp,
                    color = statusTextProperties.color
                )
            }
            AnimatedVisibility(visible = navigatorIsRunning && startDateTime != null) {
                Column {
                    RunTimeDisplay(
                        startDateTime = startDateTime!!,
                        modifier = Modifier.padding(top = 14.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                ToggleNavigatorButton(
                    properties = toggleNavigatorButtonProperties,
                    modifier = Modifier
                        .height(65.dp)
                        .width(160.dp)
                )
                FilledTonalIconButton(
                    onClick = { appVM.setScreen(Screen.NavigatorSettings) },
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        painter = painterResource(id = com.w2sv.navigator.R.drawable.ic_settings_24),
                        contentDescription = null
                    )
                }
            }
        }
    }
}

private data class StatusTextProperties(@StringRes val textRes: Int, val color: Color)