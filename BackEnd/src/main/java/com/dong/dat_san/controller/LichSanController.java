package com.dong.dat_san.controller;

import com.dong.dat_san.entity.ThucTheLichSan;
import com.dong.dat_san.repository.KhoDuLieuLichSan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/lich-san")
public class LichSanController {

    @Autowired
    private KhoDuLieuLichSan khoDuLieuLichSan;

    @GetMapping("/trong")
    public ResponseEntity<List<ThucTheLichSan>> timLichTrong(
            @RequestParam Long sanId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay) {
        
        // Gọi repository lấy danh sách lịch trống theo sân và ngày
        List<ThucTheLichSan> danhSach = khoDuLieuLichSan.findLichTrongBySanAndNgay(sanId, ngay);
        return ResponseEntity.ok(danhSach);
    }
}