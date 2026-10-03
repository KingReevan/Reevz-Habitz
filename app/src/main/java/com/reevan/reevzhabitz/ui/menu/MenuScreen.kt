package com.reevan.reevzhabitz.ui.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.reevan.reevzhabitz.ui.navigation.Destination
import com.reevan.reevzhabitz.ui.theme.ReevzHabitzTheme

/**
 * A video game's main menu: the five sections as a centred stack of wide buttons with gaps
 * between them. Phase 2 gives it its final look.
 */
@Composable
fun MenuScreen(
    onOpen: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    // fillMaxSize before verticalScroll keeps the viewport height as a minimum, so the stack
    // stays vertically centred and only scrolls if it truly doesn't fit (landscape, big fonts).
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 24.dp),
    ) {
        Destination.menuEntries.forEach { destination ->
            Button(
                onClick = { onOpen(destination) },
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(
                    text = destination.label.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 640)
@Composable
private fun MenuScreenPreview() {
    ReevzHabitzTheme {
        Surface {
            MenuScreen(onOpen = {})
        }
    }
}
