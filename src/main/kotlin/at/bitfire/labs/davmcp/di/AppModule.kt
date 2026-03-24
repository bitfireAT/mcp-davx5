package at.bitfire.labs.davmcp.di

import at.bitfire.labs.davmcp.DavConfig
import at.bitfire.labs.davmcp.HttpClientBuilder
import at.bitfire.labs.davmcp.icalendar.SimpleEventConverter
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
object AppModule {

    @Provides
    @Singleton
    fun provideDavConfig(): DavConfig = DavConfig.fromEnvironment()

    @Provides
    @Singleton
    fun provideHttpClientBuilder(config: DavConfig): HttpClientBuilder = HttpClientBuilder(config)

    @Provides
    @Singleton
    fun provideSimpleEventConverter(): SimpleEventConverter = SimpleEventConverter()

}
