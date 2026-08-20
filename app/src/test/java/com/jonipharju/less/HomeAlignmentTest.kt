package com.jonipharju.less

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jonipharju.less.launcher.AppIcon
import com.jonipharju.less.launcher.FakeLauncherRepository
import com.jonipharju.less.launcher.Favorite
import com.jonipharju.less.launcher.HomeAlignment
import com.jonipharju.less.launcher.IconMode
import com.jonipharju.less.launcher.launcherAppFixture
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Where a Favorite's icon and label sit under Home Alignment. Positions, not a screenshot:
 * staggered icons are a layout bug, not a Theme.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class HomeAlignmentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `centred Home lines icons up regardless of label length`() {
        homeShowing("Maps", "A very long Favorite name", alignment = HomeAlignment.Centred)

        assertEquals(labelLeft("Maps"), labelLeft("A very long Favorite name"))
    }

    @Test
    fun `centred Home with icons off centres labels and reserves no icon space`() {
        homeShowing(
            "Maps",
            "A very long Favorite name",
            alignment = HomeAlignment.Centred,
            iconMode = IconMode.Off,
        )

        val short = compose.onNodeWithText("Maps").getUnclippedBoundsInRoot()
        val long = compose.onNodeWithText("A very long Favorite name").getUnclippedBoundsInRoot()
        // Independently centred: the short label starts to the right of the long one, and
        // their midpoints agree, so nothing is holding space for a missing icon.
        assertTrue("Expected ${short.left} to sit right of ${long.left}", short.left > long.left)
        assertEquals(
            (short.left.value + short.right.value) / 2f,
            (long.left.value + long.right.value) / 2f,
            1f,
        )
    }

    @Test
    fun `left alignment lines icons up from the start`() {
        homeShowing("Maps", "A very long Favorite name", alignment = HomeAlignment.Left)

        assertEquals(labelLeft("Maps"), labelLeft("A very long Favorite name"))
    }

    @Test
    fun `a Tombstone under centred Home lines up with the labels around it`() {
        val repository =
            homeShowing("Maps", "Calendar", "Clock", alignment = HomeAlignment.Centred)
        val calendar = repository.installedApps.value.first { it.label == "Calendar" }
        compose.runOnIdle { repository.makeUnavailable(calendar.id) }

        assertEquals(labelLeft("Maps"), labelLeft("Calendar"))
        assertEquals(labelLeft("Calendar"), labelLeft("Clock"))
    }

    private fun labelLeft(label: String): Dp = compose.onNodeWithText(label).getUnclippedBoundsInRoot().left

    private fun homeShowing(
        vararg labels: String,
        alignment: HomeAlignment,
        iconMode: IconMode = IconMode.Original,
    ): FakeLauncherRepository {
        val repository = FakeLauncherRepository()
        val icon = AppIcon(original = ImageBitmap(48, 48), themeable = null)
        runBlocking {
            labels.forEachIndexed { position, label ->
                val app = launcherAppFixture(label).copy(icon = icon)
                repository.install(app)
                repository.chooseFavorite(Favorite(app.id, position = position))
            }
            repository.updateSettings {
                it.copy(homeAlignment = alignment, iconModeOverride = iconMode)
            }
        }
        compose.setContent {
            Home(
                repository = repository,
                timeText = "14:35",
                dateText = "Tuesday, August 18, 2026",
                onOpenClock = {},
                onOpenCalendar = {},
                onOpenDrawer = {},
            )
        }
        return repository
    }
}
