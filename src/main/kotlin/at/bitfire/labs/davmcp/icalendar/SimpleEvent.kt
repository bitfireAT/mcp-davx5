@file:UseSerializers(
    InstantSerializer::class,
    LocalDateSerializer::class
)

package at.bitfire.labs.davmcp.icalendar

import at.bitfire.labs.davmcp.json.InstantSerializer
import at.bitfire.labs.davmcp.json.LocalDateSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.LocalDate

@Serializable
data class SimpleEvent(
    val fileName: String?,
    val iCalendar: String,
    val title: String?,
    val startDateTime: Instant?,
    val startDate: LocalDate?,
    val endDateTime: Instant?,
    val endDate: LocalDate?,
    val location: String?,
    val description: String?
)

fun JsonObjectBuilder.simpleEventSchema() {
    put("type", "object")
    put("properties", buildJsonObject {
        put("fileName", buildJsonObject {
            put("type", "string")
            put("description", "Name of the calendar file")
        })
        put("iCalendar", buildJsonObject {
            put("type", "string")
            put("description", "Original iCalendar data as string")
        })
        put("title", buildJsonObject {
            put("type", "string")
            put("description", "Event title (SUMMARY)")
        })
        put("startDateTime", buildJsonObject {
            put("type", "string")
            put("format", "date-time")
            put("description", "Start date-time of the event")
        })
        put("startDate", buildJsonObject {
            put("type", "string")
            put("format", "date")
            put("description", "Start date of the event")
        })
        put("endDateTime", buildJsonObject {
            put("type", "string")
            put("format", "date-time")
            put("description", "End date-time of the event")
        })
        put("endDate", buildJsonObject {
            put("type", "string")
            put("format", "date")
            put("description", "End date of the event")
        })
        put("location", buildJsonObject {
            put("type", "string")
            put("description", "Event location (LOCATION)")
        })
        put("description", buildJsonObject {
            put("type", "string")
            put("description", "Event description (DESCRIPTION)")
        })
    })
    //put("required", json.encodeToJsonElement(listOf("fileName", "iCal")))
}