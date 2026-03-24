package at.bitfire.labs.davmcp.icalendar

import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.data.CalendarOutputter
import net.fortuna.ical4j.model.*
import net.fortuna.ical4j.model.component.CalendarComponent
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.*
import net.fortuna.ical4j.model.property.immutable.ImmutableVersion
import java.io.StringReader
import java.io.StringWriter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.Temporal
import kotlin.jvm.optionals.getOrNull

class SimpleEventConverter {

    fun fromICalendar(fileName: String?, iCalendar: String): SimpleEvent? {
        val calendar = CalendarBuilder().build(StringReader(iCalendar))
        val vEvent = calendar.getComponent<VEvent>(Component.VEVENT).getOrNull() ?: return null

        val dtStart: Temporal? = vEvent.getDateTimeStart<Temporal>()?.date
        val dtEnd: Temporal? = vEvent.getEndDate<Temporal>(true)?.getOrNull()?.date

        return SimpleEvent(
            title = vEvent.summary?.value,
            startDateTime = dtStart?.instantIfDateTime(),
            startDate = dtStart as? LocalDate,
            endDateTime = dtEnd?.instantIfDateTime(),
            endDate = dtEnd as? LocalDate,
            location = vEvent.location?.value,
            description = vEvent.description?.value,
            iCalendar = iCalendar
        )
    }

    fun toICalendar(event: SimpleEvent, uid: String): String {
        if (event.iCalendar != null)
            return event.iCalendar

        val calendar = Calendar().apply {
            this += ImmutableVersion.VERSION_2_0
            this += mcpProdId
        }
        val vEvent = VEvent()
        calendar.add<ComponentContainer<CalendarComponent>>(vEvent)

        // Random UID
        vEvent += Uid(uid)

        // Handle start date/time
        if (event.startDateTime != null)
            vEvent += DtStart(event.startDateTime)
        else if (event.startDate != null)
            vEvent += DtStart(event.startDate)

        // Handle end date/time
        if (event.endDateTime != null)
            vEvent += DtEnd(event.endDateTime)
        else if (event.endDate != null)
            vEvent += DtEnd(event.endDate)

        // Add other properties
        if (event.title != null)
            vEvent += Summary(event.title)
        if (event.location != null)
            vEvent += Location(event.location)
        if (event.description != null)
            vEvent += Description(event.description)

        // Convert calendar to iCalendar string
        val writer = StringWriter()
        CalendarOutputter(false).output(calendar, writer)
        return writer.toString()
    }

    private fun Temporal?.instantIfDateTime(): Instant? =
        if (TemporalAdapter.isDateTimePrecision(this))
            TemporalAdapter.toLocalTime(this, ZoneOffset.UTC).toInstant()
        else
            null

    private operator fun PropertyContainer.plusAssign(property: Property) {
        add<PropertyContainer>(property)
    }

}