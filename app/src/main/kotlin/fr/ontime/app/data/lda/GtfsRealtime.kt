package fr.ontime.app.data.lda

/** One stop of a real-time trip; times are epoch seconds when published. */
data class RtStop(val stopId: String, val sequence: Int, val arrival: Long?, val departure: Long?, val skipped: Boolean)

data class RtTrip(val tripId: String, val routeId: String, val cancelled: Boolean, val stops: List<RtStop>)

/**
 * Minimal GTFS-Realtime TripUpdates decoder (protobuf wire format), limited to
 * the fields OnTime needs, so no protobuf library is shipped. Returns null on a
 * malformed feed.
 */
fun parseTripUpdates(bytes: ByteArray): List<RtTrip>? = runCatching {
    Proto(bytes).fields().filter { it.number == 2 }.mapNotNull { entity ->
        val tripUpdate = Proto(entity.bytes()).fields().firstOrNull { it.number == 3 } ?: return@mapNotNull null
        val fields = Proto(tripUpdate.bytes()).fields().toList()
        val descriptor = Proto(fields.first { it.number == 1 }.bytes()).fields().toList()
        val stops = fields.filter { it.number == 2 }.map { update ->
            val stu = Proto(update.bytes()).fields().toList()
            RtStop(
                stopId = stu.firstOrNull { it.number == 4 }?.string().orEmpty(),
                sequence = stu.firstOrNull { it.number == 1 }?.varint?.toInt() ?: -1,
                arrival = stu.firstOrNull { it.number == 2 }?.let { eventTime(it) },
                departure = stu.firstOrNull { it.number == 3 }?.let { eventTime(it) },
                skipped = stu.firstOrNull { it.number == 5 }?.varint == SKIPPED,
            )
        }
        RtTrip(
            tripId = descriptor.firstOrNull { it.number == 1 }?.string().orEmpty(),
            routeId = descriptor.firstOrNull { it.number == 5 }?.string().orEmpty(),
            cancelled = descriptor.firstOrNull { it.number == 4 }?.varint == CANCELED,
            stops = stops,
        )
    }.toList()
}.getOrNull()

private const val SKIPPED = 1L
private const val CANCELED = 3L

private fun eventTime(field: Proto.Field): Long? =
    Proto(field.bytes()).fields().firstOrNull { it.number == 2 }?.varint

/** Protobuf wire-format reader (varint, 64-bit, length-delimited, 32-bit). */
internal class Proto(private val data: ByteArray) {
    class Field(val number: Int, val varint: Long?, private val raw: ByteArray?) {
        fun bytes(): ByteArray = requireNotNull(raw)
        fun string(): String = String(bytes(), Charsets.UTF_8)
    }

    fun fields(): Sequence<Field> = sequence {
        var index = 0
        fun readVarint(): Long {
            var result = 0L
            var shift = 0
            while (true) {
                val byte = data[index++].toInt() and 0xFF
                result = result or ((byte and 0x7F).toLong() shl shift)
                if (byte < 0x80) return result
                shift += 7
                require(shift < 64)
            }
        }
        while (index < data.size) {
            val key = readVarint()
            val number = (key ushr 3).toInt()
            when ((key and 7).toInt()) {
                0 -> yield(Field(number, readVarint(), null))
                1 -> { index += 8; require(index <= data.size) }
                2 -> {
                    val length = readVarint().toInt()
                    require(length >= 0 && index + length <= data.size)
                    yield(Field(number, null, data.copyOfRange(index, index + length)))
                    index += length
                }
                5 -> { index += 4; require(index <= data.size) }
                else -> throw IllegalArgumentException("wire type")
            }
        }
    }
}
