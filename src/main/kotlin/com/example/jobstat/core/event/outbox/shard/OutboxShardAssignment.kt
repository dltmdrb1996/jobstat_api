package com.example.jobstat.core.event.outbox.shard

data class OutboxShardAssignment(
    val shards: List<Long> = emptyList()
) {
    companion object {
        fun of(appId: String, appIds: List<String>, shardCount: Long): OutboxShardAssignment {
            val shards = assign(appId, appIds, shardCount)
            return OutboxShardAssignment(shards)
        }

        private fun assign(appId: String, appIds: List<String>, shardCount: Long): List<Long> {
            val appIndex = appIds.indexOf(appId)
            if (appIndex == -1) {
                return emptyList()
            }

            val start = appIndex * shardCount / appIds.size
            val end = (appIndex + 1) * shardCount / appIds.size - 1

            return (start..end).toList()
        }
    }
}