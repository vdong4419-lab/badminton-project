package com.dong.dat_san.controller;

import com.dong.dat_san.dto.YeuCauTaoSan;
import com.dong.dat_san.entity.San;
import com.dong.dat_san.repository.SanRepository;
import com.dong.dat_san.service.DichVuLichSan;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/san")
public class SanController {
    private final SanRepository courts;
    private final DichVuLichSan schedules;

    public SanController(SanRepository courts, DichVuLichSan schedules) {
        this.courts = courts;
        this.schedules = schedules;
    }

    @GetMapping
    public List<San> layDanhSachSan(@RequestParam(name = "ten", required = false) String ten) {
        var results = ten == null || ten.isBlank() ? courts.findAll()
                : courts.findByTenSanContainingIgnoreCase(ten.trim());
        return results.stream().filter(San::isDangHoatDong).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<San> laySanTheoId(@PathVariable Long id) {
        return courts.findById(id).filter(San::isDangHoatDong)
                .map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<San> taoSan(@Valid @RequestBody YeuCauTaoSan request) {
        var court = schedules.taoSan(request);
        return ResponseEntity.created(URI.create("/api/san/" + court.getId())).body(court);
    }
}