package at.bitfire.labs.davmcp.di

import at.bitfire.labs.davmcp.tools.*
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet

@Module
object ToolsModule {

    @Provides
    @IntoSet
    fun provideAddEventTool(addEventTool: AddEventTool): McpTool = addEventTool

    @Provides
    @IntoSet
    fun provideDeleteEventTool(deleteEventTool: DeleteEventTool): McpTool = deleteEventTool

    @Provides
    @IntoSet
    fun provideQueryByTimeTool(queryEventsByTimeTool: QueryEventsByTimeTool): McpTool = queryEventsByTimeTool

    @Provides
    @IntoSet
    fun provideUpdateEventTool(updateEventTool: UpdateEventTool): McpTool = updateEventTool

    @Provides
    @IntoSet
    fun provideWhenIsNowTool(whenIsNowTool: WhenIsNowTool): McpTool = whenIsNowTool

}
