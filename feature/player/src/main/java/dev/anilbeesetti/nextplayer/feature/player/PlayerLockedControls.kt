package dev.anilbeesetti.nextplayer.feature.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.extensions.copy
import dev.anilbeesetti.nextplayer.feature.player.buttons.PlayerButton

// Heights of the unlocked controls that sit below the lock row: the button row, the seekbar and
// the two gaps in between. Keeping the same offsets makes the icon stay in place when locking.
private val CONTROLS_BELOW_LOCK_ROW = 40.dp + 4.dp + 24.dp + 4.dp

@Composable
fun PlayerLockedControls(
    onUnlockControls: () -> Unit,
    modifier: Modifier = Modifier,
    unlockButtonModifier: Modifier = Modifier,
) {
    val systemBarsPadding = WindowInsets.systemBars.union(
        WindowInsets.displayCutout,
    ).asPaddingValues()
    val bottomPadding =
        16.dp.takeIf { systemBarsPadding.calculateBottomPadding() == 0.dp } ?: 0.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(systemBarsPadding.copy(top = 0.dp))
            .padding(horizontal = 16.dp)
            .padding(bottom = bottomPadding + CONTROLS_BELOW_LOCK_ROW),
        contentAlignment = Alignment.BottomCenter,
    ) {
        PlayerButton(
            modifier = unlockButtonModifier,
            containerColor = Color.Black.copy(alpha = 0.5f),
            onClick = onUnlockControls,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_lock),
                contentDescription = stringResource(R.string.controls_unlock),
            )
        }
    }
}
