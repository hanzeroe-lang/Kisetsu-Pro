package com.music.bitchord.data.event

import com.music.bitchord.data.event.SpotifyGreeting.Daypart
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.Date

/** The greeting switches on the hour, and the small hours get the moon. */
class SpotifyGreetingTest {

    private fun at(hour: Int): Date =
        Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.OCTOBER, 28, hour, 30, 0)
        }.time

    @Test
    fun morning() {
        assertEquals(Daypart.MORNING, SpotifyGreeting.daypartAt(at(5)))
        assertEquals(Daypart.MORNING, SpotifyGreeting.daypartAt(at(11)))
        assertEquals("Good morning 🎃", SpotifyGreeting.lineAt(at(8), "🎃"))
    }

    @Test
    fun afternoon() {
        assertEquals(Daypart.AFTERNOON, SpotifyGreeting.daypartAt(at(12)))
        assertEquals("Good afternoon 🎃", SpotifyGreeting.lineAt(at(16), "🎃"))
    }

    @Test
    fun night() {
        assertEquals(Daypart.EVENING, SpotifyGreeting.daypartAt(at(18)))
        assertEquals("Good night 🎃", SpotifyGreeting.lineAt(at(21), "🎃"))
    }

    @Test
    fun small_hours_offer_an_all_nighter_and_a_moon() {
        assertEquals(Daypart.NIGHT, SpotifyGreeting.daypartAt(at(23)))
        assertEquals(Daypart.NIGHT, SpotifyGreeting.daypartAt(at(0)))
        assertEquals(Daypart.NIGHT, SpotifyGreeting.daypartAt(at(4)))
        assertEquals("Fancy an all-nighter? 🌙", SpotifyGreeting.lineAt(at(3), "🎃"))
        assertEquals("Fancy an all-nighter? 🌙", SpotifyGreeting.lineAt(at(3), "🌸"))
    }
}
