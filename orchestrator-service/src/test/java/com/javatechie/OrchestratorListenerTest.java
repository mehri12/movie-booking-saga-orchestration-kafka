package com.javatechie;

import com.javatechie.events.BookingCreatedEvent;
import com.javatechie.events.BookingPaymentEvent;
import com.javatechie.events.SagaCommand;
import com.javatechie.events.SeatReservedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static com.javatechie.Common.KafkaConfigProperties.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class OrchestratorListenerTest {

    private OrchestratorProducer producer;
    private OrchestratorListener listener;

    @BeforeEach
    void setUp() {
        producer = mock(OrchestratorProducer.class);
        listener = new OrchestratorListener(producer);
    }

    @Test
    void reservationCreee_envoieOrdreReserverSieges() {
        listener.onBookingCreated(
                new BookingCreatedEvent("b1", "user1", "SHOW_101", List.of("A1", "A2"), 1000));

        ArgumentCaptor<SagaCommand> captor = ArgumentCaptor.forClass(SagaCommand.class);
        verify(producer).sendCommand(eq(SEAT_COMMAND_TOPIC), captor.capture());
        assertEquals("RESERVE_SEATS", captor.getValue().action());
        assertEquals("b1", captor.getValue().bookingId());
        assertEquals(List.of("A1", "A2"), captor.getValue().seatIds());
    }

    @Test
    void siegesVerrouilles_envoieOrdrePaiement() {
        listener.onSeatReserved(new SeatReservedEvent("b1", true, 1000));

        ArgumentCaptor<SagaCommand> captor = ArgumentCaptor.forClass(SagaCommand.class);
        verify(producer).sendCommand(eq(PAYMENT_COMMAND_TOPIC), captor.capture());
        assertEquals("PROCESS_PAYMENT", captor.getValue().action());
        assertEquals(1000, captor.getValue().amount());
    }

    @Test
    void siegesIndisponibles_echecSansPaiement() {
        listener.onSeatReserved(new SeatReservedEvent("b1", false, 1000));

        ArgumentCaptor<SagaCommand> captor = ArgumentCaptor.forClass(SagaCommand.class);
        verify(producer).sendCommand(eq(BOOKING_COMMAND_TOPIC), captor.capture());
        assertEquals("FAIL_BOOKING", captor.getValue().action());
        verify(producer, never()).sendCommand(eq(PAYMENT_COMMAND_TOPIC), any());
    }

    @Test
    void paiementAccepte_confirmeLaReservation() {
        listener.onPayementResult(new BookingPaymentEvent("b1", true, 1000));

        ArgumentCaptor<SagaCommand> captor = ArgumentCaptor.forClass(SagaCommand.class);
        verify(producer).sendCommand(eq(BOOKING_COMMAND_TOPIC), captor.capture());
        assertEquals("CONFIRM_BOOKING", captor.getValue().action());
    }

    @Test
    void paiementRefuse_libereLesSiegesEtEchoue() {
        listener.onPayementResult(new BookingPaymentEvent("b1", false, 2500));

        ArgumentCaptor<SagaCommand> seatCaptor = ArgumentCaptor.forClass(SagaCommand.class);
        verify(producer).sendCommand(eq(SEAT_COMMAND_TOPIC), seatCaptor.capture());
        assertEquals("RELEASE_SEATS", seatCaptor.getValue().action());

        ArgumentCaptor<SagaCommand> bookingCaptor = ArgumentCaptor.forClass(SagaCommand.class);
        verify(producer).sendCommand(eq(BOOKING_COMMAND_TOPIC), bookingCaptor.capture());
        assertEquals("FAIL_BOOKING", bookingCaptor.getValue().action());
    }
}
