package dev.favourdevlabs.cleanthes.security.session

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LastScreenHolderImpl
    @Inject
    constructor() : LastScreenHolder {
        @Volatile private var pending: Pair<String, Map<String, Any?>>? = null

        override fun capture(
            className: String,
            extras: Map<String, Any?>,
        ) {
            pending = className to extras
        }

        override fun consume(): Pair<String, Map<String, Any?>>? {
            val value = pending
            pending = null
            return value
        }
    }
