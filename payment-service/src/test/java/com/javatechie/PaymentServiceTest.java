package com.javatechie;

import com.javatechie.PaymentEventsProducer;
import com.javatechie.events.SeatReservedEvent;
import com.javatechie.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class PaymentServiceTest {

    private PaymentEventsProducer producer;
    private PaymentService service;

    @BeforeEach
    void setUp() {
        producer = mock(PaymentEventsProducer.class);
        service = new PaymentService(producer);
    }

    @Test
    void montantSousLeSeuil_paiementAccepte() {
        var event = new SeatReservedEvent("b1", true, 1000);

        service.processPayment(event);

        verify(producer).publishPaymentSuccessEvent(event);
        verify(producer, never()).publishPaymentFailureEvent(any());
    }

    @Test
    void montantAuDessusDuSeuil_paiementRefuse() {
        var event = new SeatReservedEvent("b1", true, 2500);

        service.processPayment(event);

        verify(producer).publishPaymentFailureEvent(event);
        verify(producer, never()).publishPaymentSuccessEvent(any());
    }

    @Test
    void montantExactementAuSeuil_paiementAccepte() {
        var event = new SeatReservedEvent("b1", true, 2000);

        service.processPayment(event);

        verify(producer).publishPaymentSuccessEvent(event);
    }

    @Test
    void erreurPendantLePaiement_publieUnEchec() {
        var event = new SeatReservedEvent("b1", true, 1000);
        doThrow(new RuntimeException("boom")).when(producer).publishPaymentSuccessEvent(any());

        service.processPayment(event);

        verify(producer).publishPaymentFailureEvent(event);
    }
}
