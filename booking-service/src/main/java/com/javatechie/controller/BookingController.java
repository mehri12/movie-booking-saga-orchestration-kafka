package com.javatechie.controller;

import com.javatechie.request.BookingRequest;
import com.javatechie.response.BookingResponse;
import com.javatechie.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/booking-service")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // Endpoint to handle seat booking requests
    @PostMapping("/bookSeat")
    public ResponseEntity<?> bookSeat(@RequestBody BookingRequest request) {
        BookingResponse response = bookingService.bookSeats(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/bookings/{reservationId}")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable String reservationId) {
        BookingResponse response = bookingService.getBooking(reservationId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }
}
