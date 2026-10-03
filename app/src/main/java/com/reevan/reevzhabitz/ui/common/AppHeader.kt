package com.reevan.reevzhabitz.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.reevan.reevzhabitz.R
import com.reevan.reevzhabitz.ui.theme.ReevzHabitzTheme

/**
 * The header bar's own height, excluding the status bar. Thin by request — Material's TopAppBar is
 * 64dp — but no thinner than the 48dp an icon button needs to stay an accessible touch target.
 */
private val HeaderHeight = 48.dp

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
 * `Home > Menu > Add Habit`. The current screen is emphasised; the trail before it is dimmed, and
 * each earlier crumb is a button that jumps straight back to that screen ([onCrumbClick] gets its
 * index in the trail).
 *
 * Each crumb is a full-header-height touch target rather than a link inside one line of text. If
 * the trail is wider than the header it scrolls instead of being cut off, and starts scrolled to
 * the end so the current screen's name is the part in view.
 */
@Composable
fun BreadcrumbHeader(
    crumbs: List<String>,
    onBack: () -> Unit,
    onCrumbClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trailColor = MaterialTheme.colorScheme.onSurfaceVariant
    HeaderBar(modifier) {
        IconButton(onClick = onBack) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = "Back",
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                // reverseScrolling: scroll position 0 is the *end* of the trail.
                .horizontalScroll(rememberScrollState(), reverseScrolling = true)
                .padding(end = 8.dp)
                .testTag(BREADCRUMBS_TAG),
        ) {
            crumbs.forEachIndexed { index, crumb ->
                if (index == crumbs.lastIndex) {
                    Text(
                        text = crumb,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .heightIn(min = HeaderHeight)
                            .clickable(
                                role = Role.Button,
                                onClickLabel = "Go to $crumb",
                            ) { onCrumbClick(index) }
                            .padding(horizontal = 6.dp),
                    ) {
                        Text(
                            text = crumb,
                            style = MaterialTheme.typography.titleSmall,
                            color = trailColor,
                            maxLines = 1,
                        )
                    }
                    Text(
                        text = ">",
                        style = MaterialTheme.typography.titleSmall,
                        color = trailColor,
                        modifier = Modifier.clearAndSetSemantics {},
                    )
                }
            }
        }
    }
}

/** Test tag on the breadcrumb trail. */
const val BREADCRUMBS_TAG = "breadcrumbs"

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
                    // A minimum, not a fixed height, so a large system font grows the bar
                    // instead of clipping the date or breadcrumb.
                    .heightIn(min = HeaderHeight)
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
        BreadcrumbHeader(
            crumbs = listOf("Home", "Menu", "Add Habit"),
            onBack = {},
            onCrumbClick = {},
        )
    }
}
