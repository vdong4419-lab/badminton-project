package com.dong.dat_san.config;

import com.dong.dat_san.service.DichVuHetHanGiuCho;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.schedules.enabled", havingValue = "true", matchIfMissing = true)
public class TacVuHetHanGiuCho {
    private final DichVuHetHanGiuCho expiry;
    public TacVuHetHanGiuCho(DichVuHetHanGiuCho expiry) { this.expiry = expiry; }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(fixedDelayString = "${app.booking.expiry-scan-ms:30000}")
    public void quet() { expiry.quet(); }
}
