package com.javatechie;

import com.javatechie.events.BookingCreatedEvent;
import com.javatechie.events.BookingPaymentEvent;
import com.javatechie.events.SeatReservedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.kafka.consumer.auto-offset-reset=earliest")
@Testcontainers
public class OrchestratorKafkaIntegrationTest {

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.0"));

    @Autowired
    private KafkaTemplate<String, Object> template;

    @Test
    void reservationCreee_produitUnOrdreReserverSieges() {
        String id = "it-" + UUID.randomUUID();

        template.send("movie-booking-events", id,
                new BookingCreatedEvent(id, "user1", "SHOW_101", List.of("A1", "A2"), 1000));

        String message = attendreMessage("seat-command-topic", id);
        assertThat(message).contains("RESERVE_SEATS").contains("SHOW_101");
    }

    @Test
    void siegesVerrouilles_produitUnOrdrePaiement() {
        String id = "it-" + UUID.randomUUID();

        template.send("seat-reserved-topic", id, new SeatReservedEvent(id, true, 1000));

        String message = attendreMessage("payment-command-topic", id);
        assertThat(message).contains("PROCESS_PAYMENT");
    }

    @Test
    void paiementRefuse_produitLiberationDesSiegesEtEchec() {
        String id = "it-" + UUID.randomUUID();

        template.send("payment-events", id, new BookingPaymentEvent(id, false, 2500));

        assertThat(attendreMessage("seat-command-topic", id)).contains("RELEASE_SEATS");
        assertThat(attendreMessage("booking-command-topic", id)).contains("FAIL_BOOKING");
    }

    // Lit un topic avec un consommateur de test jusqu'à trouver un message contenant l'identifiant
    private String attendreMessage(String topic, String bookingId) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.METADATA_MAX_AGE_CONFIG, 1000);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topic));
            long limite = System.currentTimeMillis() + 20_000;
            while (System.currentTimeMillis() < limite) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                    if (record.value().contains(bookingId)) {
                        return record.value();
                    }
                }
            }
        }
        throw new AssertionError("Aucun message pour " + bookingId + " sur le topic " + topic);
    }
}
