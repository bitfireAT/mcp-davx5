package at.bitfire.labs.davmcp.di

import at.bitfire.labs.davmcp.tools.AddEventTool
import at.bitfire.labs.davmcp.tools.McpTool
import at.bitfire.labs.davmcp.tools.QueryByTimeTool
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
    fun provideQueryByTimeTool(queryByTimeTool: QueryByTimeTool): McpTool = queryByTimeTool

}
