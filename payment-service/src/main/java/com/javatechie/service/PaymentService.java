package com.javatechie.service;

import com.javatechie.PaymentEventsProducer;
import com.javatechie.events.BookingPaymentEvent;
import com.javatechie.events.SeatReservedEvent;
import com.javatechie.exception.PaymentServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;



@Service

public class PaymentService {

    private static final Logger log= LoggerFactory.getLogger(PaymentService.class);

    private final PaymentEventsProducer eventsProducer;

    public PaymentService(PaymentEventsProducer eventsProducer){
        this.eventsProducer=eventsProducer;
    }

    /*
    public PaymentService(PaymentEventsProducer eventsProducer){
        this.eventsProducer=eventsProducer;
    }

    public void processPayment(SeatReservedEvent event) {
        try {
            log.info("Processing payment for bookingId: {}", event.bookingId());

            if (event.amount() > 2000) {
                log.info("Payment amount exceeds limit for bookingId: {}", event.bookingId());
                eventsProducer.publishPaymentFailureEvent(event);
            } else {
                eventsProducer.publishPaymentSuccessEvent(event);
                log.info("✅ Payment successful for bookingId: {}", event.bookingId());
            }
        } catch (Exception e) {
            log.error("❌ Payment failed for bookingId: {}. Reason: {}", event.bookingId(), e.getMessage());
            throw new PaymentServiceException("Payment processing failed for bookingId: " + event.bookingId());
        }
    }

     */
    public void processPayment(SeatReservedEvent event) {
        try {
            log.info("Processing payment for bookingId: {}", event.bookingId());

            if (event.amount() > 2000) {
                log.info("Payment amount exceeds limit for bookingId: {}", event.bookingId());
                eventsProducer.publishPaymentFailureEvent(event);
            } else {
                eventsProducer.publishPaymentSuccessEvent(event);
                log.info("✅ Payment successful for bookingId: {}", event.bookingId());
            }
        } catch (Exception e) {
            log.error("❌ Payment failed for bookingId: {}. Reason: {}", event.bookingId(), e.getMessage());
            eventsProducer.publishPaymentFailureEvent(event);
        }
    }
}
