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

    @Test
    fun `a custom label matches alongside the real app name`() {
        val messages = launcherAppFixture("Messages")
        val apps = listOf(messages, launcherAppFixture("Maps"))
        val customLabels = mapOf(messages.id to "Texts")

        assertEquals(listOf("Messages"), apps.rankedFor("texts", customLabels).labels())
        assertEquals(listOf("Messages"), apps.rankedFor("messages", customLabels).labels())
    }

    @Test
    fun `the stronger of an app's two names decides its rank`() {
        val settings = launcherAppFixture("Settings")
        val sets = launcherAppFixture("Sets")
        val apps = listOf(settings, sets)

        // "Settings" only starts with the query, where its custom label matches it outright.
        assertEquals(
            listOf("Settings", "Sets"),
            apps.rankedFor("set", mapOf(settings.id to "Set")).labels(),
        )
    }

    @Test
    fun `a custom label on one app does not match another`() {
        val messages = launcherAppFixture("Messages")
        val apps = listOf(messages, launcherAppFixture("Maps"))

        assertEquals(emptyList<String>(), apps.rankedFor("texts", mapOf(messages.id to "Notes")).labels())
    }

    private fun apps(vararg labels: String) = labels.map(::launcherAppFixture)

    private fun List<LauncherApp>.labels() = map(LauncherApp::label)
}
