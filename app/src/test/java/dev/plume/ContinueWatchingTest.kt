package dev.plume

import org.junit.Assert.assertEquals
import org.junit.Test

class ContinueWatchingTest {
    @Test fun newestFirstAndThreePerAppAhead() {
        // Netflix reports times for ten shows; Stremio's channel rows all get the time it was last opened.
        val netflix = (1..10).map { 100L - it to "netflix/$it" }
        val stremio = (1..4).map { 95L to "stremio/$it" }
        val nuvio = (1..2).map { 0L to "nuvio/$it" } // never opened from Plume
        val shown = newestPerApp(netflix + stremio + nuvio, 3) { it.substringBefore('/') }
        // Each app's three newest first, then the rest, newest first.
        assertEquals(
            listOf("netflix/1", "netflix/2", "netflix/3", "stremio/1", "stremio/2", "stremio/3", "nuvio/1", "nuvio/2",
                "netflix/4", "netflix/5", "stremio/4") + (6..10).map { "netflix/$it" },
            shown,
        )
    }

    @Test fun oneAppIsNotCapped() {
        val stremio = (1..10).map { 100L - it to "stremio/$it" }
        assertEquals(stremio.map { it.second }, newestPerApp(stremio, 3) { it.substringBefore('/') })
    }

    @Test fun busyAppCantPushOthersBack() {
        // Four newer Stremio shows, then one Netflix show: Netflix moves up to fourth.
        val items = (1..4).map { 100L - it to "stremio/$it" } + (90L to "netflix/1")
        assertEquals(listOf("stremio/1", "stremio/2", "stremio/3", "netflix/1", "stremio/4"), newestPerApp(items, 3) { it.substringBefore('/') })
    }
}
