package fr.ontime.app.data.lda

import fr.ontime.domain.Mode
import fr.ontime.domain.Status
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LignesAzurTest {
    /** Tiny protobuf writer to build GTFS-RT fixtures. */
    private class Pb {
        private val out = ByteArrayOutputStream()
        private fun varint(value: Long) {
            var v = value
            while (v >= 0x80) { out.write(((v and 0x7F) or 0x80).toInt()); v = v ushr 7 }
            out.write(v.toInt())
        }
        fun int(field: Int, value: Long) = apply { varint((field shl 3).toLong()); varint(value) }
        fun bytes(field: Int, value: ByteArray) = apply { varint(((field shl 3) or 2).toLong()); varint(value.size.toLong()); out.write(value) }
        fun str(field: Int, value: String) = bytes(field, value.toByteArray())
        fun msg(field: Int, value: Pb) = bytes(field, value.build())
        fun build(): ByteArray = out.toByteArray()
    }

    private fun stop(seq: Int, id: String, time: Long, skipped: Boolean = false) = Pb().int(1, seq.toLong()).str(4, id)
        .msg(2, Pb().int(2, time)).msg(3, Pb().int(2, time)).apply { if (skipped) int(5, 1) }

    private fun trip(id: String, route: String, vararg stops: Pb, cancelled: Boolean = false): Pb {
        val descriptor = Pb().str(1, id).str(5, route).apply { if (cancelled) int(4, 3) }
        val update = Pb().msg(1, descriptor).apply { stops.forEach { msg(2, it) } }
        return Pb().str(1, "e-$id").msg(3, update)
    }

    private val stopsCsv = """
        stop_id,stop_code,stop_name,stop_lat,stop_lon,zone_id,location_type,parent_station,stop_timezone,wheelchair_boarding
        place_A,,Place Alpha,43.70,7.25,,1,,,
        1,A1,Place Alpha,43.70,7.25,,0,place_A,,1
        2,A2,Place Alpha,43.70,7.25,,0,place_A,,1
        place_B,,"Gare, Bêta",43.71,7.26,,1,,,
        3,B1,"Gare, Bêta",43.71,7.26,,0,place_B,,1
        place_T,,Terminus,43.72,7.27,,1,,,
        4,T1,Terminus,43.72,7.27,,0,place_T,,1
    """.trimIndent()

    private val feed = Pb()
        .msg(1, Pb().str(1, "2.0").int(3, 1_791_630_000))
        .msg(2, trip("t1", "12", stop(1, "1", 1_791_630_600), stop(2, "3", 1_791_631_200), stop(3, "4", 1_791_631_800)))
        .msg(2, trip("t2", "L1", stop(1, "2", 1_791_630_300), stop(2, "3", 1_791_630_900, skipped = true), stop(3, "4", 1_791_631_500)))
        .msg(2, trip("t3", "12", stop(1, "1", 1_791_630_400), stop(2, "3", 1_791_630_800), cancelled = true))
        .msg(2, trip("t4", "12", stop(1, "3", 1_791_630_100), stop(2, "1", 1_791_630_500)))
        .build()

    @Test
    fun `decodes trip updates with skipped stops and cancellations`() {
        val trips = assertNotNull(parseTripUpdates(feed))
        assertEquals(listOf("t1", "t2", "t3", "t4"), trips.map { it.tripId })
        assertEquals("L1", trips[1].routeId)
        assertTrue(trips[1].stops[1].skipped)
        assertTrue(trips[2].cancelled)
        assertEquals(1_791_630_600L, trips[0].stops[0].departure)
        assertNull(parseTripUpdates(byteArrayOf(0x0A, 0x7F)))
    }

    @Test
    fun `groups platforms by place and searches names without accents`() {
        val stops = assertNotNull(AzurStops.parse(stopsCsv))
        assertEquals("lda:place_A", stops.placeOf("2"))
        assertEquals(listOf("Gare, Bêta"), stops.search("gare beta").map { it.name })
        assertEquals("lda:place_A", stops.nearest(43.7001, 7.2501)?.first?.id)
        assertNull(stops.nearest(44.5, 8.0))
        assertNull(AzurStops.parse("nope"))
    }

    @Test
    fun `keeps vehicles going from origin to destination in that order`() {
        val dir = Files.createTempDirectory("lda").toFile()
        val zip = ByteArrayOutputStream().also { bytes ->
            ZipOutputStream(bytes).use { it.putNextEntry(ZipEntry("stops.txt")); it.write(stopsCsv.toByteArray()); it.closeEntry() }
        }.toByteArray()
        val clock = Clock.fixed(Instant.ofEpochSecond(1_791_630_000), ZoneOffset.UTC)
        val source = LignesAzurSource(dir, clock) { url -> if (url.endsWith("trip-updates")) feed else zip }

        val snapshot = source.fetchTrip("lda:place_A", "lda:place_B")
        assertEquals(Status.Available, snapshot.status)
        // t2 skips B (cancelled for this trip), t4 runs the other way.
        assertEquals(listOf("t2", "t3", "t1"), snapshot.departures.map { it.journeyId })
        assertEquals("Arrêt non desservi", snapshot.departures.first().disruption)
        assertTrue(snapshot.departures.first().cancelled)
        val bus = snapshot.departures.last()
        assertEquals(Mode.Bus, bus.mode)
        assertEquals("Terminus", bus.destination)
        assertEquals(Instant.ofEpochSecond(1_791_631_200), bus.arrivalAt)
        assertEquals("Course supprimée", snapshot.departures[1].disruption)
        assertEquals(listOf("Gare, Bêta", "Terminus"), source.directionsFrom("lda:place_A")?.map { it.name })
        assertEquals(listOf("t4"), source.fetchTrip("lda:place_B", "lda:place_A").departures.map { it.journeyId })
        assertEquals(Status.Empty, source.fetchTrip("lda:place_T", "lda:place_A").status)
    }

    @Test
    fun `reports an error without network`() {
        val source = LignesAzurSource(Files.createTempDirectory("lda").toFile(), Clock.systemUTC()) { null }
        assertEquals(Status.Error, source.fetchTrip("lda:place_A", "lda:place_B").status)
    }
}
