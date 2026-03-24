package at.bitfire.labs.davmcp.di

import at.bitfire.labs.davmcp.tools.AddEventTool
import at.bitfire.labs.davmcp.tools.McpTool
import at.bitfire.labs.davmcp.tools.QueryEventsByTimeTool
import at.bitfire.labs.davmcp.tools.WhenIsNowTool
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
    fun provideQueryByTimeTool(queryEventsByTimeTool: QueryEventsByTimeTool): McpTool = queryEventsByTimeTool

    @Provides
    @IntoSet
    fun provideWhenIsNowTool(whenIsNowTool: WhenIsNowTool): McpTool = whenIsNowTool

}
