package com.jonipharju.less.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupTest {
    private val phone = launcherAppFixture(label = "Phone")
    private val messages = launcherAppFixture(label = "Messages")
    private val camera = launcherAppFixture(label = "Camera")
    private val browser = launcherAppFixture(label = "Browser")
    private val notes = launcherAppFixture(label = "Notes")
    private val installed = listOf(browser, camera, messages, notes, phone)

    private val answers =
        mapOf(
            EverydayIntent.Phone to phone.id,
            EverydayIntent.Messaging to messages.id,
            EverydayIntent.Camera to camera.id,
            EverydayIntent.Browser to browser.id,
        )

    @Test
    fun `Setup starts at the Theme picker, before it asks the user for anything`() {
        assertEquals(SetupStep.Theme, LauncherSettings().setupStep)
    }

    @Test
    fun `the steps run Theme, then default launcher, then Favorites`() {
        assertEquals(SetupStep.DefaultLauncher, SetupStep.Theme.next())
        assertEquals(SetupStep.Favorites, SetupStep.DefaultLauncher.next())
        assertEquals(SetupStep.Done, SetupStep.Favorites.next())
    }

    @Test
    fun `Setup stops once it is done`() {
        assertTrue(SetupStep.Theme.isRunning())
        assertTrue(SetupStep.Favorites.isRunning())
        assertFalse(SetupStep.Done.isRunning())
        assertEquals(SetupStep.Done, SetupStep.Done.next())
    }

    @Test
    fun `there is nowhere to back out to from the first step`() {
        assertNull(SetupStep.Theme.previous())
        assertEquals(SetupStep.Theme, SetupStep.DefaultLauncher.previous())
    }

    @Test
    fun `the Drawer asks for the Home Role while Less has never held it`() {
        assertTrue(LauncherSettings().asksForHomeRole(holdsHomeRole = false))
    }

    @Test
    fun `the Drawer does not ask while Less holds the Home Role`() {
        assertFalse(LauncherSettings().asksForHomeRole(holdsHomeRole = true))
    }

    @Test
    fun `having held the Home Role once, the Drawer never asks again`() {
        val settings = LauncherSettings(hasHeldHomeRole = true)

        assertFalse(settings.asksForHomeRole(holdsHomeRole = true))
        assertFalse(settings.asksForHomeRole(holdsHomeRole = false))
    }

    @Test
    fun `the proposed Favorites are the apps answering the everyday intents, in that order`() {
        assertEquals(
            listOf(phone, messages, camera, browser),
            installed.answering(answers::get),
        )
    }

    @Test
    fun `an app answering two intents is proposed once`() {
        val everything = answers.mapValues { phone.id }

        assertEquals(listOf(phone), installed.answering(everything::get))
    }

    @Test
    fun `an intent no app on the device answers is left out`() {
        val withoutCamera = answers - EverydayIntent.Camera

        assertEquals(listOf(phone, messages, browser), installed.answering(withoutCamera::get))
    }

    @Test
    fun `an answer naming an app that is not installed is left out`() {
        val uninstalled = launcherAppFixture(label = "Gone")
        val stale = answers + (EverydayIntent.Camera to uninstalled.id)

        assertEquals(listOf(phone, messages, browser), installed.answering(stale::get))
    }

    @Test
    fun `the picker lists the proposed apps first and the rest alphabetically`() {
        val proposed = installed.answering(answers::get)

        assertEquals(
            listOf(phone, messages, camera, browser, notes),
            installed.proposedFirst(proposed),
        )
    }

    @Test
    fun `nothing proposed still lists every app alphabetically`() {
        assertEquals(installed, installed.proposedFirst(emptyList()))
    }

    @Test
    fun `the chosen apps become Favorites numbered in the order they were listed`() {
        val listed = installed.proposedFirst(installed.answering(answers::get))

        assertEquals(
            listOf(
                Favorite(appId = phone.id, position = 0),
                Favorite(appId = camera.id, position = 1),
                Favorite(appId = notes.id, position = 2),
            ),
            listed.asFavorites(setOf(camera.id, notes.id, phone.id)),
        )
    }

    @Test
    fun `choosing nothing leaves Home without Favorites`() {
        assertEquals(emptyList<Favorite>(), installed.asFavorites(emptySet()))
    }
}
