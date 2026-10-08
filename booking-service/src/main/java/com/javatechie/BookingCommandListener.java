package com.javatechie;

import com.javatechie.events.SagaCommand;
import com.javatechie.service.BookingService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.javatechie.common.KafkaConfigProperties.BOOKING_COMMAND_TOPIC;
import static com.javatechie.common.KafkaConfigProperties.MOVIE_BOOKING_GROUP;


@Component
public class BookingCommandListener {
    private final static Logger log= LoggerFactory.getLogger(BookingCommandListener.class);
    private final BookingService bookingService;

    public BookingCommandListener(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @KafkaListener(topics = BOOKING_COMMAND_TOPIC, groupId = MOVIE_BOOKING_GROUP)
    public void consumeCommand(SagaCommand command) {
        log.info("BookingCommandListener:: ordre {} reçu pour bookingId {}",
                command.action(), command.bookingId());

        switch (command.action()) {
            case "CONFIRM_BOOKING" -> bookingService.confirmBooking(command.bookingId());
            case "FAIL_BOOKING" -> bookingService.handleBookingOnSeatReservationFailure(command.bookingId());
            default -> log.warn("BookingCommandListener:: ordre inconnu : {}", command.action());
        }
    }
}
