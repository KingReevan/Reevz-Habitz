package com.reevan.reevzhabitz.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.reevan.reevzhabitz.ui.common.SectionPlaceholder
import com.reevan.reevzhabitz.ui.theme.ReevzHabitzTheme

/**
 * App shell: a single Scaffold owning the top bar, with the current section as its content.
 *
 * There is only one section so far. When there are several, switch between them with plain state
 * rather than a navigation library until sections actually grow sub-screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReevzHabitzApp() {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text("Reevz Habitz") })
        },
    ) { innerPadding ->
        SectionPlaceholder(
            name = "Habits",
            note = "Nothing here yet.",
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReevzHabitzAppPreview() {
    ReevzHabitzTheme {
        ReevzHabitzApp()
    }
}
