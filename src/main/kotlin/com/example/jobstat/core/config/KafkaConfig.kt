package com.example.jobstat.core.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.listener.ContainerProperties

@Configuration
class KafkaConfig {
    @Bean
    fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String?, String?>
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.consumerFactory = consumerFactory
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
        return factory


//        @Bean
//        public Map<String, Object> producerConfigs() {
//            Map<String, Object> props = new HashMap<>();
//            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
//            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
//            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
//            props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, enableIdempotence);
//            props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "transactional-id-" + UUID.randomUUID());
//            props.put(ProducerConfig.ACKS_CONFIG, "all");
//
//            return props;
//        }
    }
}
