package com.jonipharju.less.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class HiddenAppsTest {
    private val clock = launcherAppFixture(label = "Clock")
    private val camera = launcherAppFixture(label = "Camera")
    private val maps = launcherAppFixture(label = "Maps")
    private val installed = listOf(camera, clock, maps)

    @Test
    fun `the Drawer lists every app while none is hidden`() {
        assertEquals(installed, installed.withoutHidden(emptySet()))
    }

    @Test
    fun `a Hidden App is left out of the Drawer's listing`() {
        assertEquals(listOf(camera, maps), installed.withoutHidden(setOf(clock.id)))
    }

    @Test
    fun `hiding one app leaves the rest in their order`() {
        assertEquals(listOf(camera), installed.withoutHidden(setOf(clock.id, maps.id)))
    }

    @Test
    fun `an id no longer installed hides nothing`() {
        val uninstalled = launcherAppFixture(label = "Gone")

        assertEquals(installed, installed.withoutHidden(setOf(uninstalled.id)))
    }

    @Test
    fun `Hidden Apps are listed alphabetically for the screen that undoes them`() {
        assertEquals(listOf(camera, maps), installed.hiddenAmong(setOf(maps.id, camera.id)))
    }

    @Test
    fun `a Hidden App that is no longer installed is not listed`() {
        val uninstalled = launcherAppFixture(label = "Gone")

        assertEquals(listOf(clock), installed.hiddenAmong(setOf(clock.id, uninstalled.id)))
    }

    @Test
    fun `nothing hidden means nothing to list`() {
        assertEquals(emptyList<LauncherApp>(), installed.hiddenAmong(emptySet()))
    }
}
