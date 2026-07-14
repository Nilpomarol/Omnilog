package com.nilpo.contenttracker.core.repository

class DeletionRecoveryStore {
    private var nextToken = 1L
    private val pending = mutableMapOf<Long, DeletionRecovery>()

    fun put(recovery: DeletionRecovery): Long {
        val token = nextToken++
        pending[token] = recovery
        return token
    }

    fun take(token: Long): DeletionRecovery? = pending.remove(token)

    fun discard(token: Long) {
        pending.remove(token)
    }
}
