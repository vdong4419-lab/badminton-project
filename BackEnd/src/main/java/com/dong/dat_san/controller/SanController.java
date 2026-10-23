package com.dong.dat_san.controller;

import com.dong.dat_san.dto.YeuCauTaoSan;
import com.dong.dat_san.dto.SanCongKhaiDto;
import com.dong.dat_san.service.DichVuSanCongKhai;
import com.dong.dat_san.entity.San;
import com.dong.dat_san.service.DichVuLichSan;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/san")
public class SanController {
    private final DichVuLichSan schedules;
    private final com.dong.dat_san.service.DichVuQuanLySan quanLySan;
    private final DichVuSanCongKhai publicCourts;

    public SanController(DichVuLichSan schedules, com.dong.dat_san.service.DichVuQuanLySan quanLySan, DichVuSanCongKhai publicCourts) {
        this.schedules = schedules;
        this.quanLySan = quanLySan;
        this.publicCourts=publicCourts;
    }

    @GetMapping
    public List<SanCongKhaiDto> layDanhSachSan(@RequestParam(name = "ten", required = false) String ten) {
        return publicCourts.danhSach(ten);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SanCongKhaiDto> laySanTheoId(@PathVariable Long id) {
        return ResponseEntity.ok(publicCourts.chiTiet(id));
    }

    @PostMapping
    public ResponseEntity<San> taoSan(@Valid @RequestBody YeuCauTaoSan request) {
        var court = schedules.taoSan(request);
        return ResponseEntity.created(URI.create("/api/san/" + court.getId())).body(court);
    }

    @PutMapping("/{id}")
    public San suaSan(@PathVariable Long id,@Valid @RequestBody com.dong.dat_san.dto.YeuCauSuaSan input) {
        return quanLySan.luu(id,input);
    }
}
