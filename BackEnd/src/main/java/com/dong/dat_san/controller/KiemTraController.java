package com.dong.dat_san.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kiem-tra")
public class KiemTraController {

    @GetMapping
    public ResponseEntity<String> kiemTraHeThong() {
        return ResponseEntity.ok("Hệ thống đặt sân cầu lông đang hoạt động tốt!");
    }
}