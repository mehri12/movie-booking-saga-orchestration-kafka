package com.javatechie.listener;

import com.javatechie.events.BookingCreatedEvent;
import com.javatechie.events.SagaCommand;
import com.javatechie.service.SeatInventoryService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.javatechie.common.KafkaConfigProperties.SEAT_COMMAND_TOPIC;
import static com.javatechie.common.KafkaConfigProperties.SEAT_EVENT_GROUP;


@Component
public class SeatCommandListener {
    private static final Logger log= LoggerFactory.getLogger(SeatCommandListener.class);
    private final SeatInventoryService seatInventoryService;

    public SeatCommandListener(SeatInventoryService seatInventoryService) {
        this.seatInventoryService = seatInventoryService;
    }

    @KafkaListener(topics = SEAT_COMMAND_TOPIC, groupId = SEAT_EVENT_GROUP)
    public void consumeCommand(SagaCommand command) {
        log.info("SeatCommandListener:: ordre {} reçu pour bookingId {}",
                command.action(), command.bookingId());

        switch (command.action()) {
            case "RESERVE_SEATS" -> {
                var event = new BookingCreatedEvent(command.bookingId(), null,
                        command.showId(), command.seatIds(), command.amount());
                seatInventoryService.handleBooking(event);
            }
            case "RELEASE_SEATS" ->
                    seatInventoryService.releaseSeatsOnPaymentFailure(command.bookingId());
            default -> log.warn("SeatCommandListener:: ordre inconnu : {}", command.action());
        }
    }
}
