package com.javatechie.Common;

public class KafkaConfigProperties {


    // Réponses et événements (existants)
    public static final String MOVIE_BOOKING_EVENTS_TOPIC = "movie-booking-events";
    public static final String SEAT_RESERVED_TOPIC = "seat-reserved-topic";
    public static final String PAYMENT_EVENTS_TOPIC = "payment-events";

    // Commandes (nouveaux)
    public static final String SEAT_COMMAND_TOPIC = "seat-command-topic";
    public static final String PAYMENT_COMMAND_TOPIC = "payment-command-topic";
    public static final String BOOKING_COMMAND_TOPIC = "booking-command-topic";

    public static final String ORCHESTRATOR_GROUP = "orchestrator-group";
}
