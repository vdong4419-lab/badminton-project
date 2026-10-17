package com.dong.dat_san.controller;

import java.util.List;
import java.util.Optional;
import java.net.URI;

import com.dong.dat_san.dto.YeuCauTaoSan;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.dong.dat_san.entity.San;
import com.dong.dat_san.repository.SanRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/san")
public class SanController {

    private final SanRepository sanRepository;

    public SanController(SanRepository sanRepository) {
        this.sanRepository = sanRepository;
    }

    // Lấy danh sách sân, có thể tìm theo tên
    @GetMapping
    public List<San> layDanhSachSan(
            @RequestParam(name = "ten", required = false) String ten) {

        if (ten == null || ten.isBlank()) {
            return sanRepository.findAll();
        }

        return sanRepository
                .findByTenSanContainingIgnoreCase(ten.trim());
    }

    // Lấy một sân theo ID
    @GetMapping("/{id}")
    public ResponseEntity<San> laySanTheoId(
            @PathVariable("id") Long id) {

        Optional<San> ketQua = sanRepository.findById(id);

        if (ketQua.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(ketQua.get());
    }
    @PostMapping
public ResponseEntity<San> taoSan(
        @Valid @RequestBody YeuCauTaoSan yeuCau) {

    San san = new San();
    san.setTenSan(yeuCau.tenSan().trim());
    san.setGiaMoiGio(yeuCau.giaMoiGio());

    try {
        San sanDaLuu = sanRepository.saveAndFlush(san);

        return ResponseEntity
                .created(URI.create("/api/san/" + sanDaLuu.getId()))
                .body(sanDaLuu);

    } catch (DataIntegrityViolationException e) {
        return ResponseEntity.status(409).build();
    }
}
}