package at.bitfire.labs.davmcp.di

import at.bitfire.labs.davmcp.DavConfig
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
object AppModule {

    @Provides
    @Singleton
    fun provideDavConfig(): DavConfig = DavConfig.fromEnvironment()

}
