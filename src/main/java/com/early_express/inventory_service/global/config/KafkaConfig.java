package com.early_express.inventory_service.global.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public RecordMessageConverter converter() {
        return new StringJsonMessageConverter();
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // 기본 토픽 생성 예제
    // 이벤트 토픽 - 도메인 이벤트 발행용
    @Bean
    public NewTopic applicationEventsTopic() {
        return TopicBuilder.name(applicationName + "-events")
                .partitions(3)
                .replicas(3)
                .config("min.insync.replicas", "2")
                .build();
    }

    // 커맨드 토픽 - 명령 처리용
    @Bean
    public NewTopic applicationCommandsTopic() {
        return TopicBuilder.name(applicationName + "-commands")
                .partitions(3)
                .replicas(3)
                .config("min.insync.replicas", "2")
                .build();
    }

    // DLT(Dead Letter Topic) - 처리 실패한 메시지 보관
    @Bean
    public NewTopic applicationDeadLetterTopic() {
        return TopicBuilder.name(applicationName + "-dlt")
                .partitions(1)
                .replicas(3)
                .build();
    }
}