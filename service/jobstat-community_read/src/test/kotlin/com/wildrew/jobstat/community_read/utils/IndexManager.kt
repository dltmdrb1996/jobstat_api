package com.wildrew.jobstat.community_read.utils

import java.util.concurrent.ConcurrentHashMap

class IndexManager<K, V> {
    private val index = ConcurrentHashMap<K, V>()

    fun put(
        key: K,
        value: V,
    ) {
        index[key] = value
    }

    fun get(key: K): V? = index[key]

    fun remove(key: K) {
        index.remove(key)
    }

    fun clear() {
        index.clear()
    }

    fun containsKey(key: K): Boolean = index.containsKey(key)
}
