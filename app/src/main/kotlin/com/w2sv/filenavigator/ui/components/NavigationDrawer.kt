package com.w2sv.filenavigator.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FloatSpringSpec
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ShareCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.w2sv.androidutils.coroutines.launchDelayed
import com.w2sv.androidutils.generic.appPlayStoreUrl
import com.w2sv.androidutils.generic.openUrlWithActivityNotFoundHandling
import com.w2sv.androidutils.notifying.showToast
import com.w2sv.filenavigator.BuildConfig
import com.w2sv.filenavigator.R
import com.w2sv.filenavigator.ui.screens.main.MainScreenViewModel
import com.w2sv.filenavigator.ui.screens.main.components.NavigatorSettingsDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val drawerAnim = FloatSpringSpec(Spring.DampingRatioMediumBouncy)

suspend fun DrawerState.closeAnimated(anim: AnimationSpec<Float> = drawerAnim) {
    animateTo(DrawerValue.Closed, anim)
}

suspend fun DrawerState.openAnimated(anim: AnimationSpec<Float> = drawerAnim) {
    animateTo(DrawerValue.Open, anim)
}

@Composable
fun NavigationDrawer(
    state: DrawerState,
    modifier: Modifier = Modifier,
    homeScreenViewModel: MainScreenViewModel = viewModel(),
    scope: CoroutineScope = rememberCoroutineScope(),
    content: @Composable () -> Unit
) {
    val theme by homeScreenViewModel.unconfirmedInAppTheme.collectAsState()
    val themeRequiringUpdate by homeScreenViewModel.unconfirmedInAppTheme.statesDissimilar.collectAsState()

    var showThemeDialog by rememberSaveable {
        mutableStateOf(false)
    }
        .apply {
            if (value) {
                ThemeSelectionDialog(
                    onDismissRequest = {
                        scope.launch {
                            homeScreenViewModel.unconfirmedInAppTheme.reset()
                        }
                        value = false
                    },
                    selectedTheme = { theme },
                    onThemeSelected = { homeScreenViewModel.unconfirmedInAppTheme.value = it },
                    applyButtonEnabled = { themeRequiringUpdate },
                    onApplyButtonClick = {
                        scope.launch {
                            homeScreenViewModel.unconfirmedInAppTheme.sync()
                        }
                        value = false
                    }
                )
            }
        }
    var showSettingsDialog by rememberSaveable {
        mutableStateOf(false)
    }
        .apply {
            if (value) {
                NavigatorSettingsDialog(closeDialog = { value = false })
            }
        }

    ModalNavigationDrawer(
        modifier = modifier,
        drawerContent = {
            NavigationDrawerSheet(
                closeDrawer = { scope.launch { state.closeAnimated() } },
                onItemSettingsPressed = {
                    scope.launchDelayed(250L) {
                        showSettingsDialog = true
                    }
                },
                onItemThemePressed = {
                    // show dialog after delay for display of navigationDrawer close animation
                    scope.launchDelayed(250L) {
                        showThemeDialog = true
                    }
                }
            )
        },
        drawerState = state
    ) {
        content()
    }
}

fun DrawerState.offsetFraction(maxWidthPx: Int): State<Float> =
    derivedStateOf { 1 + offset.value / maxWidthPx }

@Composable
private fun NavigationDrawerSheet(
    closeDrawer: () -> Unit,
    onItemSettingsPressed: () -> Unit,
    onItemThemePressed: () -> Unit,
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current
) {
    ModalDrawerSheet(modifier = modifier) {
        Column(
            modifier = Modifier
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.padding(vertical = 32.dp)) {
                Image(
                    painterResource(id = R.drawable.ic_launcher_foreground),
                    null,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                VersionText(Modifier.padding(top = 26.dp))
            }
            Divider(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 12.dp)
            )
            remember {
                listOf(
                    NavigationDrawerItem(
                        com.w2sv.navigator.R.drawable.ic_settings_24,
                        R.string.navigator_settings
                    ) {
                        onItemSettingsPressed()
                    },
                    NavigationDrawerItem(
                        R.drawable.ic_nightlight_24,
                        R.string.theme
                    ) {
                        onItemThemePressed()
                    },
                    NavigationDrawerItem(
                        R.drawable.ic_share_24,
                        R.string.share
                    ) {
                        ShareCompat.IntentBuilder(it)
                            .setType("text/plain")
                            .setText(context.getString(R.string.share_action_text))
                            .startChooser()
                    },
                    NavigationDrawerItem(
                        R.drawable.ic_star_rate_24,
                        R.string.rate
                    ) {
                        try {
                            it.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(appPlayStoreUrl(it))
                                )
                                    .setPackage("com.android.vending")
                            )
                        } catch (e: ActivityNotFoundException) {
                            it.showToast(context.getString(R.string.you_re_not_signed_into_the_play_store))
                        }
                    },
                    NavigationDrawerItem(
                        R.drawable.ic_policy_24,
                        R.string.privacy_policy
                    ) {
                        it.openUrlWithActivityNotFoundHandling("https://github.com/w2sv/WiFi-Widget/blob/main/PRIVACY-POLICY.md")
                    }
                )
            }
                .forEach {
                    NavigationDrawerItem(properties = it, closeDrawer = closeDrawer)
                }
        }
    }
}

@Composable
fun VersionText(modifier: Modifier = Modifier) {
    AppFontText(
        text = stringResource(id = R.string.version).format(BuildConfig.VERSION_NAME),
        modifier = modifier
    )
}

@Immutable
private data class NavigationDrawerItem(
    @DrawableRes val icon: Int,
    @StringRes val label: Int,
    val callback: (Context) -> Unit
)

@Composable
private fun NavigationDrawerItem(
    properties: NavigationDrawerItem,
    closeDrawer: () -> Unit,
    context: Context = LocalContext.current
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                properties.callback(context)
                closeDrawer()
            }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier
                .size(size = 28.dp),
            painter = painterResource(id = properties.icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )

        AppFontText(
            text = stringResource(id = properties.label),
            modifier = Modifier.padding(start = 16.dp),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}