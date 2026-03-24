package at.bitfire.labs.davmcp.icalendar

import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Component
import net.fortuna.ical4j.model.TemporalAdapter
import net.fortuna.ical4j.model.component.VEvent
import java.io.StringReader
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.Temporal
import kotlin.jvm.optionals.getOrNull

class SimpleEventConverter {

    fun convert(fileName: String?, iCalendar: String): SimpleEvent? {
        val calendar = CalendarBuilder().build(StringReader(iCalendar))
        val vEvent = calendar.getComponent<VEvent>(Component.VEVENT).getOrNull() ?: return null

        val dtStart: Temporal? = vEvent.getDateTimeStart<Temporal>()?.date
        val dtEnd: Temporal? = vEvent.getEndDate<Temporal>(true)?.getOrNull()?.date

        return SimpleEvent(
            fileName = fileName,
            iCalendar = iCalendar,
            title = vEvent.summary?.value,
            startDateTime = dtStart?.instantIfDateTime(),
            startDate = dtStart as? LocalDate,
            endDateTime = dtEnd?.instantIfDateTime(),
            endDate = dtEnd as? LocalDate,
            location = vEvent.location?.value,
            description = vEvent.description?.value
        )
    }

    private fun Temporal?.instantIfDateTime(): Instant? =
        if (TemporalAdapter.isDateTimePrecision(this))
            TemporalAdapter.toLocalTime(this, ZoneOffset.UTC).toInstant()
        else
            null

}