package dev.anilbeesetti.nextplayer.settings.navigation

import androidx.compose.runtime.SideEffect
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import dev.anilbeesetti.nextplayer.settings.screens.about.AboutPreferencesScreen
import dev.anilbeesetti.nextplayer.settings.screens.about.AboutPreferencesViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
object AboutPreferencesRoute : NavKey

fun NavBackStack<NavKey>.navigateToAboutPreferences() {
    add(element = AboutPreferencesRoute)
}

fun EntryProviderScope<NavKey>.aboutPreferencesEntry(onNavigateUp: () -> Unit) {
    entry<AboutPreferencesRoute> {
        val output = AboutPreferencesViewModel.Output(navigateUp = onNavigateUp)
        val viewModel = koinViewModel<AboutPreferencesViewModel>(
            parameters = { parametersOf(output) },
        )
        SideEffect { viewModel.output = output }
        AboutPreferencesScreen(viewModel = viewModel)
    }
}
