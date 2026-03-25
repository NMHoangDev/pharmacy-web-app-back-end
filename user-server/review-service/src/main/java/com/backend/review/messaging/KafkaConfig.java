package com.backend.review.messaging;

import com.backend.common.model.EventEnvelope;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Minimal shared Kafka producer config and topic builders.
 * Each service can import this config or copy if custom needed.
 */
@Configuration
public class KafkaConfig {

    @Bean
    public ProducerFactory<String, EventEnvelope<?>> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, EventEnvelope<?>> kafkaTemplate(ProducerFactory<String, EventEnvelope<?>> pf) {
        return new KafkaTemplate<>(pf);
    }

    @Bean
    public NewTopic orderTopic() {
        return TopicBuilder.name(TopicNames.ORDER_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic inventoryTopic() {
        return TopicBuilder.name(TopicNames.INVENTORY_EVENTS).partitions(3).replicas(1).build();
    }
}
