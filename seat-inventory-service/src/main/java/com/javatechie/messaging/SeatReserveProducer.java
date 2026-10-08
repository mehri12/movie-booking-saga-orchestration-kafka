package com.javatechie.messaging;

import com.javatechie.common.KafkaConfigProperties;
import com.javatechie.events.BookingCreatedEvent;
import com.javatechie.events.SeatReservedEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import static com.javatechie.common.KafkaConfigProperties.SEAT_RESERVED_TOPIC;

@Component

public class SeatReserveProducer {
    private static final Logger log= LoggerFactory.getLogger(SeatReserveProducer.class);
    private KafkaTemplate<String, SeatReservedEvent> template;

    public SeatReserveProducer(KafkaTemplate<String, SeatReservedEvent> template) {
        this.template = template;
    }

    public void publishSeatReserveEvents(SeatReservedEvent reservedEvent) {
        try {
            log.info("SeatReserveProducer:: Publishing seatReserved event for bookingId {}", reservedEvent.bookingId());
            template.send(SEAT_RESERVED_TOPIC,reservedEvent.bookingId(), reservedEvent);
        } catch (Exception e) {
            log.error("SeatReserveProducer:: Error while publishing seatReserved event for bookingId {}: {}", reservedEvent.bookingId(), e.getMessage());
        }
    }
}
