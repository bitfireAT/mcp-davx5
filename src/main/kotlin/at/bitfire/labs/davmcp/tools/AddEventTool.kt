package at.bitfire.labs.davmcp.tools

import at.bitfire.dav4jvm.ktor.DavCalendar
import io.ktor.client.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.*
import io.ktor.utils.io.*
import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put
import net.fortuna.ical4j.data.CalendarOutputter
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.ComponentContainer
import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.PropertyContainer
import net.fortuna.ical4j.model.component.CalendarComponent
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.*
import net.fortuna.ical4j.model.property.immutable.ImmutableCalScale
import net.fortuna.ical4j.model.property.immutable.ImmutableVersion
import okio.IOException
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class AddEventTool {

    @Serializable
    private data class EventRequest(
        val title: String,
        val startDateTime: String,
        val endDateTime: String,
        val description: String?
    )

    private data class EventData(
        val summary: String,
        val startDate: LocalDateTime,
        val endDate: LocalDateTime,
        val description: String?
    )

    fun tool() = Tool(
        name = "events.add",
        description = "Adds an event to the user's calendar",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("title", buildJsonObject {
                    put("type", "string")
                    put("description", "Title of the event")
                })
                put("startDateTime", buildJsonObject {
                    put("type", "string")
                    put("format", "date-time")
                    put("description", "Start date date/time (inclusive) of the event (\"YYYY-MM-DDThh:mm:ss\")")
                })
                put("endDateTime", buildJsonObject {
                    put("type", "string")
                    put("format", "date-time")
                    put("description", "End date or date/time (exclusive) of the event (\"YYYY-MM-DDThh:mm:ss\")")
                })
                put("description", buildJsonObject {
                    put("type", "string")
                    put("description", "Optional description of the event (plain text)")
                })
            },
            required = listOf("title", "startDateTime", "endDateTime")
        )
    )
    
    suspend fun handler(connection: ClientConnection, request: CallToolRequest): CallToolResult {
        try {
            val eventRequest = Json.decodeFromJsonElement<EventRequest>(
                request.arguments ?: throw IllegalArgumentException("Request arguments are required")
            )

            val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            val eventData = EventData(
                summary = eventRequest.title,
                startDate = LocalDateTime.parse(eventRequest.startDateTime, dateTimeFormatter),
                endDate = LocalDateTime.parse(eventRequest.endDateTime, dateTimeFormatter),
                description = eventRequest.description
            )

            val uid = UUID.randomUUID().toString()
            val iCal = generateICal(uid, eventData)
            uploadToCollection("$uid.ics", iCal)

            return CallToolResult(content = listOf(TextContent("Success: $iCal")))
        } catch (e: Exception) {
            e.printStackTrace(System.err)
            return CallToolResult(
                content = listOf(TextContent(e.message ?: e.javaClass.name)),
                isError = true
            )
        }
    }


    private fun generateICal(uid: String, eventData: EventData): ByteArray {
        val calendar = Calendar()
        calendar += ImmutableVersion.VERSION_2_0
        calendar += ImmutableCalScale.GREGORIAN
        calendar += ProdId("-//Bitfire Labs//DAV-MCP//EN")

        val event = VEvent()
        event += Uid(uid)
        event += DtStart(eventData.startDate)
        event += DtEnd(eventData.endDate)
        event += Summary(eventData.summary)

        if (eventData.description != null) {
            event += Description(eventData.description)
        }

        calendar.add<ComponentContainer<CalendarComponent>>(event)

        // write to String and return it
        val baos = ByteArrayOutputStream()
        CalendarOutputter(false).output(calendar, baos)
        return baos.toByteArray()
    }

    private operator fun PropertyContainer.plusAssign(property: Property) {
        add<PropertyContainer>(property)
    }


    private suspend fun uploadToCollection(memberName: String, iCalendar: ByteArray) {
        HttpClient {
            install(Auth) {
                basic {
                    credentials {
                        sendWithoutRequest { true }
                        BasicAuthCredentials(username = "test", password = "<no-password-here>")
                    }
                }
            }
            install(Logging) {
                logger = Logger.SIMPLE
            }
        }.use { client ->
            val calendarUrl = Url("https://nextcloud.davtest.dev001.net/remote.php/dav/calendars/test/mcp-test/")
            val url = URLBuilder(calendarUrl).appendPathSegments(memberName).build()
            System.err.println("Uploading $iCalendar to $url")

            val response = client.put {
                url(url)
                setBody(iCalendar)
            }
            if (!response.status.isSuccess())
                throw IOException("HTTP ${response.status.value} ${response.status.description}")

            /*val calendar = DavCalendar(client, url)
            calendar.put(
                provideBody = { ByteReadChannel(iCalendar) },
                mimeType = ContentType.parse("text/calendar; charset=utf-8")
            ) { response ->
                if (!response.status.isSuccess())
                    throw IOException("HTTP ${response.status.value} ${response.status.description}")
            }*/
        }
    }

}