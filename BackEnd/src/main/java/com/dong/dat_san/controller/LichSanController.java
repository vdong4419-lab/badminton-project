package com.dong.dat_san.controller;

import com.dong.dat_san.dto.LichSanDto;
import com.dong.dat_san.repository.KhoDuLieuLichSan;
import java.time.*;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lich-san")
public class LichSanController {
    private final KhoDuLieuLichSan lich;
    private final Clock clock;

    public LichSanController(KhoDuLieuLichSan lich, Clock clock) {
        this.lich = lich;
        this.clock = clock;
    }

    @GetMapping("/trong")
    public List<LichSanDto> timLichTrong(@RequestParam Long sanId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay) {
        LocalDateTime now = LocalDateTime.now(clock);
        return lich.findLichTrong(sanId, ngay, now.toLocalDate(), now.toLocalTime()).stream()
                .map(LichSanDto::from).toList();
    }
}
