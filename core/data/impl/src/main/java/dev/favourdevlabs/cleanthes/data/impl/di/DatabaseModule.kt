package dev.favourdevlabs.cleanthes.data.impl.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.favourdevlabs.cleanthes.data.impl.db.CitadelDatabaseSwitchboard
import javax.inject.Singleton

/**
 * CitadelDatabaseSwitchboard is @Inject-constructed directly (see its
 * @Singleton @Inject constructor), so it needs no @Provides here —
 * Hilt builds it from the constructor. This module is kept as the
 * single place documenting the DB DI story for the module.
 *
 * CitadelDao/AuditLogDao are NOT provided as standalone bindings anymore:
 * which DB file they resolve to depends on the active session profile,
 * which isnt known at graph-construction time. Repositories must
 * inject CitadelDatabaseSwitchboard directly and call .citadelDao() /
 * .auditLogDao() per-operation instead of holding a cached DAO reference.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule
