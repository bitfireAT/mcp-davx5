package at.bitfire.labs.davmcp.di

import at.bitfire.labs.davmcp.tools.AddEventTool
import at.bitfire.labs.davmcp.tools.McpTool
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet

@Module
object ToolsModule {

    @Provides
    @IntoSet
    fun provideAddEventTool(addEventTool: AddEventTool): McpTool = addEventTool

}
