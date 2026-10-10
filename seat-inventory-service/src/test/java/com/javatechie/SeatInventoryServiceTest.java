package com.javatechie;

import com.javatechie.entity.SeatInventory;
import com.javatechie.events.BookingCreatedEvent;
import com.javatechie.events.SeatReservedEvent;
import com.javatechie.messaging.SeatReserveProducer;
import com.javatechie.repository.SeatInventoryRepository;
import com.javatechie.service.SeatInventoryService;
import com.javatechie.utils.enums.SeatStatus;
import lombok.Builder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
@Builder
public class SeatInventoryServiceTest {

    private SeatInventoryRepository repository;
    private SeatReserveProducer producer;
    private SeatInventoryService service;

    @BeforeEach
    void setUp() {
        repository = mock(SeatInventoryRepository.class);
        producer = mock(SeatReserveProducer.class);
        service = new SeatInventoryService(repository, producer);
    }

    private SeatInventory seat(String number, SeatStatus status) {
        SeatInventory seat = new SeatInventory();
        seat.setShowId("SHOW_101");
        seat.setSeatNumber(number);
        seat.setStatus(status);
        return seat;
    }

    @Test
    void tousLesSiegesLibres_sontVerrouillesEtSuccesPublie() {
        var a1 = seat("A1", SeatStatus.AVAILABLE);
        var a2 = seat("A2", SeatStatus.AVAILABLE);
        when(repository.findByShowIdAndSeatNumberIn("SHOW_101", List.of("A1", "A2")))
                .thenReturn(List.of(a1, a2));

        service.handleBooking(new BookingCreatedEvent("b1", "user1", "SHOW_101", List.of("A1", "A2"), 1000));

        assertEquals(SeatStatus.LOCKED, a1.getStatus());
        assertEquals("b1", a1.getCurrentBookingId());
        assertEquals(SeatStatus.LOCKED, a2.getStatus());
        verify(repository).saveAll(any());
        verify(producer).publishSeatReserveEvents(new SeatReservedEvent("b1", true, 1000));
    }

    @Test
    void unSiegeDejaPris_rienNestModifieEtEchecPublie() {
        var a1 = seat("A1", SeatStatus.AVAILABLE);
        var a3 = seat("A3", SeatStatus.LOCKED);
        when(repository.findByShowIdAndSeatNumberIn("SHOW_101", List.of("A1", "A3")))
                .thenReturn(List.of(a1, a3));

        service.handleBooking(new BookingCreatedEvent("b2", "user2", "SHOW_101", List.of("A1", "A3"), 500));

        assertEquals(SeatStatus.AVAILABLE, a1.getStatus());
        verify(repository, never()).saveAll(any());
        verify(producer).publishSeatReserveEvents(new SeatReservedEvent("b2", false, 500));
    }

    @Test
    void liberationDesSieges_remetAvailableSansPublierDEvenement() {
        var a1 = seat("A1", SeatStatus.LOCKED);
        a1.setCurrentBookingId("b3");
        when(repository.findByCurrentBookingId("b3")).thenReturn(List.of(a1));

        service.releaseSeatsOnPaymentFailure("b3");

        assertEquals(SeatStatus.AVAILABLE, a1.getStatus());
        assertNull(a1.getCurrentBookingId());
        verify(repository).saveAll(any());
        verify(producer, never()).publishSeatReserveEvents(any());
    }
}
