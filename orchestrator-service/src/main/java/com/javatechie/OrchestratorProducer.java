package com.javatechie;

import com.javatechie.events.SagaCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrchestratorProducer {
    private final KafkaTemplate<String,Object> template;

    public void sendCommand(String topic, SagaCommand command){
        log.info("orchestrator -> envoi de {} vers {} (booKingId={})",
                command.action(),topic,command.bookingId());

        template.send(topic,command.bookingId(),command);
    }
}
