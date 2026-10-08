package com.javatechie.events;

import java.util.List;

public record SagaCommand(String bookingId,
                          String action,
                          String showId,
                          List<String> seatIds,
                          long amount) {
}
