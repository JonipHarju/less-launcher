package com.jonipharju.less.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AutoLaunchSearchTest {
    private val outlook = launcherAppFixture("Outlook")
    private val output = launcherAppFixture("Output")
    private val camera = launcherAppFixture("Camera")

    @Test
    fun `a unique match of three or more characters launches that app`() {
        assertEquals(
            outlook,
            appToLaunchForTypedQuery(
                query = "out",
                ranked = listOf(outlook),
                previousQuery = "ou",
            ),
        )
    }

    @Test
    fun `a unique match of one or two characters launches nothing`() {
        assertNull(
            appToLaunchForTypedQuery(
                query = "o",
                ranked = listOf(outlook),
                previousQuery = "",
            ),
        )
        assertNull(
            appToLaunchForTypedQuery(
                query = "ou",
                ranked = listOf(outlook),
                previousQuery = "o",
            ),
        )
    }

    @Test
    fun `two or more matches launch nothing`() {
        assertNull(
            appToLaunchForTypedQuery(
                query = "out",
                ranked = listOf(outlook, output),
                previousQuery = "ou",
            ),
        )
    }

    @Test
    fun `deleting down to a unique match launches nothing`() {
        assertNull(
            appToLaunchForTypedQuery(
                query = "out",
                ranked = listOf(outlook),
                previousQuery = "outl",
            ),
        )
    }

    @Test
    fun `the same query against the same results does not launch again`() {
        assertNull(
            appToLaunchForTypedQuery(
                query = "out",
                ranked = listOf(outlook),
                previousQuery = "out",
            ),
        )
    }

    @Test
    fun `a query that already launched does not launch again as it grows`() {
        assertNull(
            appToLaunchForTypedQuery(
                query = "outlook",
                ranked = listOf(outlook),
                previousQuery = "outloo",
                launchedQuery = "out",
            ),
        )
    }

    @Test
    fun `a different unique match after a cleared query can launch`() {
        assertEquals(
            camera,
            appToLaunchForTypedQuery(
                query = "cam",
                ranked = listOf(camera),
                previousQuery = "ca",
                launchedQuery = null,
            ),
        )
    }

    @Test
    fun `no matches launch nothing`() {
        assertNull(
            appToLaunchForTypedQuery(
                query = "xyz",
                ranked = emptyList(),
                previousQuery = "xy",
            ),
        )
    }

    @Test
    fun `a Hidden App does not count toward the unique match`() {
        val clock = launcherAppFixture("Clock")
        val clockRadio = launcherAppFixture("Clock Radio")
        val ranked =
            listOf(clock, clockRadio)
                .withoutHidden(setOf(clock.id))
                .rankedFor("clo")

        assertEquals(
            clockRadio,
            appToLaunchForTypedQuery(
                query = "clo",
                ranked = ranked,
                previousQuery = "cl",
            ),
        )
    }

    @Test
    fun `a Favorite's custom label is a unique match like any other`() {
        val messages = launcherAppFixture("Messages")
        val ranked =
            listOf(messages, launcherAppFixture("Maps"))
                .rankedFor("tex", mapOf(messages.id to "Texts"))

        assertEquals(
            messages,
            appToLaunchForTypedQuery(
                query = "tex",
                ranked = ranked,
                previousQuery = "te",
            ),
        )
    }
}
