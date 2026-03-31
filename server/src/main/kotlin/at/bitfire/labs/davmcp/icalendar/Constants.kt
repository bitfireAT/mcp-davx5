package at.bitfire.labs.davmcp.icalendar

import io.ktor.http.*
import net.fortuna.ical4j.model.property.ProdId

val iCalendarContentType = ContentType.parse("text/calendar")
val mcpProdId = ProdId("3DAV-MCP (bitfire.at labs)")
