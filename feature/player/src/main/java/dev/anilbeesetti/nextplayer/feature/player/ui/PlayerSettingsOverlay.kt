package dev.anilbeesetti.nextplayer.feature.player.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.anilbeesetti.nextplayer.settings.Setting
import dev.anilbeesetti.nextplayer.settings.navigation.SettingsRoute
import dev.anilbeesetti.nextplayer.settings.navigation.aboutPreferencesEntry
import dev.anilbeesetti.nextplayer.settings.navigation.audioPreferencesEntry
import dev.anilbeesetti.nextplayer.settings.navigation.generalPreferencesEntry
import dev.anilbeesetti.nextplayer.settings.navigation.gesturePreferencesEntry
import dev.anilbeesetti.nextplayer.settings.navigation.navigateToAboutPreferences
import dev.anilbeesetti.nextplayer.settings.navigation.navigateToAudioPreferences
import dev.anilbeesetti.nextplayer.settings.navigation.navigateToGeneralPreferences
import dev.anilbeesetti.nextplayer.settings.navigation.navigateToGesturePreferences
import dev.anilbeesetti.nextplayer.settings.navigation.navigateToPlayerPreferences
import dev.anilbeesetti.nextplayer.settings.navigation.navigateToSubtitlePreferences
import dev.anilbeesetti.nextplayer.settings.navigation.playerPreferencesEntry
import dev.anilbeesetti.nextplayer.settings.navigation.settingsEntry
import dev.anilbeesetti.nextplayer.settings.navigation.subtitlePreferencesEntry

/**
 * Full screen settings shown on top of the player, so preferences stay reachable without leaving
 * playback. The back action pops the inner stack first and closes the overlay afterwards.
 */
@Composable
fun PlayerSettingsOverlay(modifier: Modifier = Modifier, onDismiss: () -> Unit) {
    val backStack = rememberNavBackStack(SettingsRoute)
    val navigateUp: () -> Unit = {
        if (backStack.size > 1) backStack.removeLastOrNull() else onDismiss()
    }

    BackHandler(onBack = navigateUp)

    val entries = rememberDecoratedNavEntries<NavKey>(
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            settingsEntry(
                onNavigateUp = navigateUp,
                onItemClick = { setting ->
                    when (setting) {
                        Setting.PLAYER -> backStack.navigateToPlayerPreferences()
                        Setting.GESTURES -> backStack.navigateToGesturePreferences()
                        Setting.AUDIO -> backStack.navigateToAudioPreferences()
                        Setting.SUBTITLE -> backStack.navigateToSubtitlePreferences()
                        Setting.GENERAL -> backStack.navigateToGeneralPreferences()
                        Setting.ABOUT -> backStack.navigateToAboutPreferences()
                    }
                },
            )
            playerPreferencesEntry(onNavigateUp = navigateUp)
            gesturePreferencesEntry(onNavigateUp = navigateUp)
            audioPreferencesEntry(onNavigateUp = navigateUp)
            subtitlePreferencesEntry(onNavigateUp = navigateUp)
            generalPreferencesEntry(onNavigateUp = navigateUp)
            aboutPreferencesEntry(onNavigateUp = navigateUp)
        },
    )

    NavDisplay(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
        entries = entries,
        onBack = navigateUp,
    )
}
