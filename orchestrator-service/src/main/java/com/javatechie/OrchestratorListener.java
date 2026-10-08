package com.javatechie;

import com.javatechie.events.BookingCreatedEvent;
import com.javatechie.events.BookingPaymentEvent;
import com.javatechie.events.SagaCommand;
import com.javatechie.events.SeatReservedEvent;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import static com.javatechie.Common.KafkaConfigProperties.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrchestratorListener {

    private final OrchestratorProducer producer;

    // ÉTAPE 1 : une réservation vient d'être créée -> ordre à Seat de verrouiller
    @KafkaListener(topics = MOVIE_BOOKING_EVENTS_TOPIC, groupId = ORCHESTRATOR_GROUP)
    public void onBookingCreated(BookingCreatedEvent event){
        log.info("orchestrator <- réservation crée (bookingId={})",event.bookingId());

        producer.sendCommand(SEAT_COMMAND_TOPIC,new SagaCommand(event.bookingId(),
                "RESERVE_SEATS",event.showId(),event.seatIds(),event.amount()));
    }

    // ÉTAPE 2 : réponse de Seat
    @KafkaListener(topics = SEAT_RESERVED_TOPIC, groupId = ORCHESTRATOR_GROUP)
    public void onSeatReserved(SeatReservedEvent event){
        log.info("orchestrator <- réponse de seat (bookingId={}, reserved={})",
                event.bookingId(),event.reserved());

        if(event.reserved()){
            // sièges verrouillés -> ordre à Payment de payer
            producer.sendCommand(PAYMENT_COMMAND_TOPIC,
                    new SagaCommand(event.bookingId(), "PROCESS_PAYMENT",
                    null, null, event.amount()));
        }else {
            // sièges indisponibles -> la réservation échoue
            producer.sendCommand("BOOKING_COMMAND_TOPIC",
                    new SagaCommand(event.bookingId(), "FAIL_BOOKING",
                            null,null,  0));
        }
    }
    // ÉTAPE 3 : réponse de Payment
    @KafkaListener(topics = PAYMENT_EVENTS_TOPIC, groupId = ORCHESTRATOR_GROUP)
    public void onPayementResult(BookingPaymentEvent event){
        log.info("orchestrator<- reponse de payement (bookinId={},payementCompleted={})",
                event.bookingId(),event.paymentCompleted());

        if(event.paymentCompleted()){
            producer.sendCommand(BOOKING_COMMAND_TOPIC,
                    new SagaCommand(event.bookingId(), "CONFIRM_BOOKING",
                            null,null,event.amount()));
        }else{
            // paiement refusé -> compensation : libérer les sièges + échec
            producer.sendCommand(SEAT_COMMAND_TOPIC,
                    new SagaCommand(event.bookingId(), "RELEASE_SEATS",
                            null, null, 0));
            producer.sendCommand(BOOKING_COMMAND_TOPIC,
                    new SagaCommand(event.bookingId(), "FAIL_BOOKING",
                            null, null, 0));
        }
    }
}
