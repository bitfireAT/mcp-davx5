@file:UseSerializers(
    InstantSerializer::class,
    LocalDateSerializer::class
)

package at.bitfire.labs.davmcp.icalendar

import at.bitfire.labs.davmcp.json.InstantSerializer
import at.bitfire.labs.davmcp.json.LocalDateSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.*
import java.time.Instant
import java.time.LocalDate

@Serializable
data class SimpleEvent(
    val uid: String?,
    val title: String?,
    val startDateTime: Instant?,
    val startDate: LocalDate?,
    val endDateTime: Instant?,
    val endDate: LocalDate?,
    val location: String?,
    val description: String?,
    val consumesTime: Boolean = true,
    val iCalendar: String?,
)

fun JsonObjectBuilder.simpleEventSchema(
    includeRequired: Boolean = true,
    includeICalendar: Boolean = true
) {
    put("type", "object")
    put("properties", buildJsonObject {
        put("uid", buildJsonObject {
            put("type", "string")
            put("description", "UID of the event")
        })
        put("title", buildJsonObject {
            put("type", "string")
            put("description", "Event title (SUMMARY)")
        })
        put("startDateTime", buildJsonObject {
            put("type", "string")
            put("format", "date-time")
            put("description", "Start date-time of the event. (An event must have either startDateTime or startDate.)")
        })
        put("startDate", buildJsonObject {
            put("type", "string")
            put("format", "date")
            put("description", "Start date of the event. (An event must have either startDateTime or startDate.)")
        })
        put("endDateTime", buildJsonObject {
            put("type", "string")
            put("format", "date-time")
            put("description", "End date-time of the event. (An event must have either endDateTime or endDate.)")
        })
        put("endDate", buildJsonObject {
            put("type", "string")
            put("format", "date")
            put("description", "End date of the event. (An event must have either endDateTime or endDate.)")
        })
        put("location", buildJsonObject {
            put("type", "string")
            put("description", "Event location (LOCATION)")
        })
        put("description", buildJsonObject {
            put("type", "string")
            put("description", "Event description (DESCRIPTION)")
        })
        put("consumesTime", buildJsonObject {
            put("type", "boolean")
            put(
                "description",
                "Whether the event consumes time in the user's calendar (true) or it's shown as free time (false). (TRANSP:OPAQUE/TRANSPARENT)"
            )
            put("default", "true")
        })

        if (includeICalendar)
            put("iCalendar", buildJsonObject {
                put("type", "string")
                put("description", "Original iCalendar data as string")
            })
    })

    if (includeRequired)
        put("required", buildJsonArray {
            add("title")
        })
}