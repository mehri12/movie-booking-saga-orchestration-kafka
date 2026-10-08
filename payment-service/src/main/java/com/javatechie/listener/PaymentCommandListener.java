package com.javatechie.listener;

import com.javatechie.events.SagaCommand;
import com.javatechie.events.SeatReservedEvent;
import com.javatechie.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import static com.javatechie.common.KafkaConfigProperties.PAYMENT_COMMAND_TOPIC;
import static com.javatechie.common.KafkaConfigProperties.PAYMENT_EVENT_GROUP;
@Service
public class PaymentCommandListener {

    private static final Logger log =LoggerFactory.getLogger(PaymentCommandListener.class);
    private final PaymentService paymentService;

    public PaymentCommandListener(PaymentService paymentService){
        this.paymentService=paymentService;
    }

    @KafkaListener(topics = PAYMENT_COMMAND_TOPIC, groupId = PAYMENT_EVENT_GROUP)
    public void consumeCommand(SagaCommand command) {
        log.info("PaymentCommandListener:: ordre {} reçu pour bookingId {}",
                command.action(), command.bookingId());

        if ("PROCESS_PAYMENT".equals(command.action())) {
            var event = new SeatReservedEvent(command.bookingId(), true, command.amount());
            paymentService.processPayment(event);
        } else {
            log.warn("PaymentCommandListener:: ordre inconnu : {}", command.action());
        }
    }
}
