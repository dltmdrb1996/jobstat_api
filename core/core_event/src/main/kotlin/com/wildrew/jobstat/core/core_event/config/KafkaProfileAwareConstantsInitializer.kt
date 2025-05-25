//package com.wildrew.jobstat.core.core_event.config
//
//import com.wildrew.jobstat.core.core_event.model.EventType
//import org.slf4j.LoggerFactory
//
//class KafkaProfileAwareConstantsInitializer(
//    private val activeProfile: String,
//) {
//    private val log by lazy { LoggerFactory.getLogger(this::class.java) }
//
//    fun initializeConstants() {
//        log.info("Initializing Kafka constants for profile: $activeProfile (via CoreEventAutoConfiguration)")
//        EventType.Topic.initialize(activeProfile)
//        log.info("Initialized EventType.Topic.COMMUNITY_READ: ${EventType.Topic.communityRead}")
//        log.info("Initialized EventType.Topic.COMMUNITY_COMMAND: ${EventType.Topic.communityCommand}")
//        log.info("Initialized EventType.Topic.NOTIFICATION: ${EventType.Topic.notification}")
//    }
//}
