package at.bitfire.labs.davmcp.di

import at.bitfire.labs.davmcp.HttpClientBuilder
import at.bitfire.labs.davmcp.ServerConfig
import at.bitfire.labs.davmcp.db.Database
import at.bitfire.labs.davmcp.icalendar.SimpleEventConverter
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(config: ServerConfig): Database =
        Database(config.databaseDriver())

    @Provides
    @Singleton
    fun provideServerConfig(): ServerConfig = ServerConfig()

    @Provides
    @Singleton
    fun provideHttpClientBuilder(): HttpClientBuilder = HttpClientBuilder()

    @Provides
    @Singleton
    fun provideSimpleEventConverter(): SimpleEventConverter = SimpleEventConverter()

}
