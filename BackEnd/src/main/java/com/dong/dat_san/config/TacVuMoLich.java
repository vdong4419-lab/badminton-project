package com.dong.dat_san.config;

import com.dong.dat_san.service.DichVuLichSan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.schedules.enabled", havingValue = "true", matchIfMissing = true)
public class TacVuMoLich {
    private final DichVuLichSan schedules;

    public TacVuMoLich(DichVuLichSan schedules) { this.schedules = schedules; }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void capNhatLich() { schedules.moLich(); }
}
