package com.reevan.reevzhabitz.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.reevan.reevzhabitz.R
import com.reevan.reevzhabitz.ui.theme.ReevzHabitzTheme

/**
 * The header bar's own height, excluding the status bar. Thin by request — Material's TopAppBar is
 * 64dp — but no thinner than the 48dp an icon button needs to stay an accessible touch target.
 */
private val HeaderHeight = 48.dp

private const val CRUMB_SEPARATOR = "  >  "

/** Home's header: the date on the left, action icons on the right. */
@Composable
fun DateHeader(
    date: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    HeaderBar(modifier) {
        Text(
            text = date,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
        )
        actions()
    }
}

/**
 * Every other screen's header: a back arrow, then the path to this screen, e.g.
 * `Home > Menu > Add Habit`. The current screen is emphasised; the trail before it is dimmed.
 *
 * If the trail is too long for the width it is cut from the *start*, so the current screen's name
 * is always the part that stays visible.
 */
@Composable
fun BreadcrumbHeader(
    crumbs: List<String>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val trailColor = MaterialTheme.colorScheme.onSurfaceVariant
    val text = buildAnnotatedString {
        crumbs.forEachIndexed { index, crumb ->
            val isCurrent = index == crumbs.lastIndex
            if (isCurrent) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(crumb) }
            } else {
                withStyle(SpanStyle(color = trailColor)) {
                    append(crumb)
                    append(CRUMB_SEPARATOR)
                }
            }
        }
    }
    HeaderBar(modifier) {
        IconButton(onClick = onBack) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = "Back",
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.StartEllipsis,
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp),
        )
    }
}

/** Shared frame: draws behind the status bar, then a thin divider underneath. */
@Composable
private fun HeaderBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HeaderHeight)
                    .padding(horizontal = 4.dp),
                content = content,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Preview
@Composable
private fun DateHeaderPreview() {
    ReevzHabitzTheme {
        DateHeader(date = "03 October") {
            IconButton(onClick = {}) {
                Icon(painterResource(R.drawable.ic_menu), contentDescription = "Menu")
            }
        }
    }
}

@Preview
@Composable
private fun BreadcrumbHeaderPreview() {
    ReevzHabitzTheme {
        BreadcrumbHeader(crumbs = listOf("Home", "Menu", "Add Habit"), onBack = {})
    }
}
