package at.bitfire.labs.davmcp.icalendar

import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.data.CalendarOutputter
import net.fortuna.ical4j.model.*
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.component.CalendarComponent
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.*
import net.fortuna.ical4j.model.property.immutable.ImmutableTransp
import net.fortuna.ical4j.model.property.immutable.ImmutableVersion
import java.io.StringReader
import java.io.StringWriter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.Temporal
import java.util.*
import kotlin.jvm.optionals.getOrNull

class SimpleEventConverter {

    fun fromICalendar(iCalendar: String): SimpleEvent? {
        val calendar = CalendarBuilder().build(StringReader(iCalendar))
        val vEvent = calendar.getComponent<VEvent>(Component.VEVENT).getOrNull() ?: return null

        val dtStart: Temporal? = vEvent.getDateTimeStart<Temporal>()?.date
        val dtEnd: Temporal? = vEvent.getEndDate<Temporal>(true)?.getOrNull()?.date

        return SimpleEvent(
            uid = vEvent.uid.getOrNull()?.value,
            title = vEvent.summary?.value,
            startDateTime = dtStart?.instantIfDateTime(),
            startDate = dtStart as? LocalDate,
            endDateTime = dtEnd?.instantIfDateTime(),
            endDate = dtEnd as? LocalDate,
            location = vEvent.location?.value,
            description = vEvent.description?.value,
            consumesTime = vEvent.timeTransparency == ImmutableTransp.OPAQUE,
            iCalendar = iCalendar
        )
    }

    fun toICalendar(
        eventData: SimpleEvent,
        originalICalendar: String? = null,
        removeFieldsFromOriginal: List<String> = emptyList()
    ): String {
        if (eventData.iCalendar != null)
            return eventData.iCalendar

        val calendar = if (originalICalendar != null) {
            CalendarBuilder().build(StringReader(originalICalendar))
        } else Calendar().apply {
            this += ImmutableVersion.VERSION_2_0
            this += mcpProdId
            add<ComponentContainer<CalendarComponent>>(VEvent())
        }
        val vEvent = calendar.getComponent<VEvent>(Component.VEVENT).get()

        // remove requested fields
        for (field in removeFieldsFromOriginal)
            for (propertyName in fieldToPropertyNames(field))
                vEvent.removeAll<PropertyContainer>(propertyName)

        // random UID
        vEvent += Uid(eventData.uid ?: UUID.randomUUID().toString())

        // handle start date/time
        if (eventData.startDateTime != null)
            vEvent += DtStart(eventData.startDateTime)
        else if (eventData.startDate != null)
            vEvent += DtStart(eventData.startDate)

        // handle end date/time
        if (eventData.endDateTime != null)
            vEvent += DtEnd(eventData.endDateTime)
        else if (eventData.endDate != null)
            vEvent += DtEnd(eventData.endDate)

        // add/replace other properties
        if (eventData.title != null)
            vEvent += Summary(eventData.title)
        if (eventData.location != null)
            vEvent += Location(eventData.location)
        if (eventData.description != null)
            vEvent += Description(eventData.description)
        if (eventData.consumesTime)
            vEvent += ImmutableTransp.OPAQUE
        else
            vEvent += ImmutableTransp.TRANSPARENT

        // convert calendar to iCalendar string
        val writer = StringWriter()
        CalendarOutputter(false).output(calendar, writer)
        return writer.toString()
    }

    private fun fieldToPropertyNames(field: String): Set<String> =
        when (field) {
            "uid" -> setOf(Property.UID)
            "title" -> setOf(Property.SUMMARY)
            "startDateTime", "startDate" -> setOf(Property.DTSTART)
            "endDateTime", "endDate" -> setOf(Property.DTEND, Property.SUMMARY)
            "location" -> setOf(Property.LOCATION)
            "description" -> setOf(Property.DESCRIPTION)
            "consumesTime" -> setOf(Property.TRANSP)
            else -> emptySet()
        }

    private fun Temporal?.instantIfDateTime(): Instant? =
        if (TemporalAdapter.isDateTimePrecision(this))
            TemporalAdapter.toLocalTime(this, ZoneOffset.UTC).toInstant()
        else
            null

    private operator fun PropertyContainer.plusAssign(property: Property) {
        replace<PropertyContainer>(property)
    }

}