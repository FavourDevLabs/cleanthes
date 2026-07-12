package dev.favourdevlabs.cleanthes.security.session.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.favourdevlabs.cleanthes.security.session.LastScreenHolder
import dev.favourdevlabs.cleanthes.security.session.LastScreenHolderImpl
import dev.favourdevlabs.cleanthes.security.session.SessionManager
import dev.favourdevlabs.cleanthes.security.session.SessionManagerImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class SessionManagerModule {
    @Binds
    abstract fun bindSessionManager(impl: SessionManagerImpl): SessionManager

    @Binds
    abstract fun bindLastScreenHolder(impl: LastScreenHolderImpl): LastScreenHolder
}
