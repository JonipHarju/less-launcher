package com.jonipharju.less.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class AppRankingTest {
    @Test
    fun `matches are ordered by match class`() {
        val apps =
            apps(
                "MyCPTool",
                "CP Tools",
                "My CP Tool",
                "CP",
                "City Parking",
            )

        assertEquals(
            listOf("CP", "CP Tools", "My CP Tool", "MyCPTool", "City Parking"),
            apps.rankedFor("cp").labels(),
        )
    }

    @Test
    fun `ties retain alphabetical app order`() {
        val apps = apps("Clockwork", "Clock", "Clock Radio")

        assertEquals(
            listOf("Clock", "Clock Radio", "Clockwork"),
            apps.rankedFor("clo").labels(),
        )
    }

    @Test
    fun `matching ignores case and diacritics`() {
        val apps = apps("Cafe Racer", "Cafeteria", "Café")

        assertEquals(
            listOf("Café", "Cafe Racer", "Cafeteria"),
            apps.rankedFor("CAFÉ").labels(),
        )
    }

    @Test
    fun `empty query returns every app alphabetically`() {
        val apps = apps("Zebra", "Clock", "Alarm")

        assertEquals(listOf("Alarm", "Clock", "Zebra"), apps.rankedFor("").labels())
    }

    @Test
    fun `query without matches returns an empty list`() {
        assertEquals(emptyList<LauncherApp>(), apps("Clock", "Camera").rankedFor("weather"))
    }

    @Test
    fun `initials only match multi-word names`() {
        val apps = apps("Google Maps", "Giraffe", "Great Music Player")

        assertEquals(
            listOf("Google Maps", "Great Music Player"),
            apps.rankedFor("gm").labels(),
        )
    }

    private fun apps(vararg labels: String) = labels.map(::launcherAppFixture)

    private fun List<LauncherApp>.labels() = map(LauncherApp::label)
}
