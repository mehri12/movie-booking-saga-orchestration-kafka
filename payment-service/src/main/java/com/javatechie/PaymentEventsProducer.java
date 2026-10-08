package com.javatechie;

import com.javatechie.common.KafkaConfigProperties;
import com.javatechie.events.BookingPaymentEvent;
import com.javatechie.events.SeatReservedEvent;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component

public class PaymentEventsProducer {
    private final static Logger log= LoggerFactory.getLogger(PaymentEventsProducer.class);
    private KafkaTemplate<String,Object> template;

    public PaymentEventsProducer(KafkaTemplate<String, Object> template) {
        this.template = template;
    }

    public void publishPaymentSuccessEvent(SeatReservedEvent event){
        log.info("Publishing payment success event ...");
        BookingPaymentEvent paymentEvent=new BookingPaymentEvent(event.bookingId(),true,event.amount());
        template
                .send(KafkaConfigProperties.PAYMENT_EVENTS_TOPIC, event.bookingId(),paymentEvent);

    }

    public void publishPaymentFailureEvent(SeatReservedEvent event){
        log.info("Publishing payment failure event ...");
        BookingPaymentEvent paymentEvent=new BookingPaymentEvent(event.bookingId(),false,event.amount());
        template
                .send(KafkaConfigProperties.PAYMENT_EVENTS_TOPIC, event.bookingId(),paymentEvent);

    }
}
