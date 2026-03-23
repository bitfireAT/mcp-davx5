package at.bitfire.labs.davmcp.di

import at.bitfire.labs.davmcp.McpServer
import at.bitfire.labs.davmcp.tools.AddEventTool
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class, ToolsModule::class])
interface AppComponent {

    fun mcpServer(): McpServer

}
