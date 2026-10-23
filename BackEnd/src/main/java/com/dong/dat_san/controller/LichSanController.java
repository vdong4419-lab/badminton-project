package com.dong.dat_san.controller;

import com.dong.dat_san.dto.LichSanDto;
import com.dong.dat_san.repository.KhoDuLieuLichSan;
import com.dong.dat_san.service.DichVuHetHanGiuCho;
import java.time.*;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lich-san")
public class LichSanController {
    private final KhoDuLieuLichSan lich;
    private final Clock clock;
    private final DichVuHetHanGiuCho expiry;

    public LichSanController(KhoDuLieuLichSan lich, Clock clock, DichVuHetHanGiuCho expiry) {
        this.lich = lich;
        this.clock = clock;
        this.expiry = expiry;
    }

    @GetMapping("/trong")
    public List<LichSanDto> timLichTrong(@RequestParam Long sanId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay) {
        expiry.quet();
        LocalDateTime now = LocalDateTime.now(clock);
        return lich.findLichTrong(sanId, ngay, now.toLocalDate(), now.toLocalTime()).stream()
                .map(LichSanDto::from).toList();
    }
}
