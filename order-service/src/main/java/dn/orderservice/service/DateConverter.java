package dn.orderservice.service;

import dn.orderservice.entity.OutboxEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class DateConverter {

    private static final String DATE_FORMAT = "yyyy-Mm-dd || HH:mm:ss";




    public String dateFormat(OutboxEntity outboxEntity){
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT));
        outboxEntity.setProcessedAt(date);
        return outboxEntity.getProcessedAt();
    }
}
