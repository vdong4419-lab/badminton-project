package com.dong.dat_san.controller;

import com.dong.dat_san.dto.*;
import com.dong.dat_san.service.*;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quan-tri/don-dat")
public class QuanTriDonController {
    private final DichVuQuanTriDon service;
    private final DichVuHetHanGiuCho expiry;
    public QuanTriDonController(DichVuQuanTriDon service, DichVuHetHanGiuCho expiry) {
        this.service=service; this.expiry=expiry;
    }
    @GetMapping
    public Map<String, Object> danhSach(@RequestParam(required = false) String trangThai,
            @RequestParam(defaultValue = "false") boolean choHoanTien, @RequestParam(defaultValue = "0") int page,
            @RequestParam(required=false) @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate ngay,
            @RequestParam(defaultValue="") String tim) {
        expiry.quet();
        return service.danhSach(trangThai, choHoanTien, page, ngay, tim);
    }
    @GetMapping("/{id}")
    public QuanTriDonDto chiTiet(@PathVariable Long id) {
        expiry.xuLy(id);
        return service.chiTiet(id);
    }
    @PutMapping("/{id}/xac-nhan")
    public QuanTriDonDto xacNhan(@PathVariable Long id, Principal principal) {
        expiry.xuLy(id);
        return service.xacNhan(id, principal.getName());
    }
    @PostMapping("/{id}/thanh-toan")
    public QuanTriDonDto thanhToan(@PathVariable Long id, Principal principal, @Valid @RequestBody YeuCauThanhToan request) {
        expiry.xuLy(id);
        return service.thanhToan(id, principal.getName(), request);
    }
    @PutMapping("/{id}/huy")
    public QuanTriDonDto huy(@PathVariable Long id, Principal principal, @Valid @RequestBody YeuCauHuyDatSan request) {
        expiry.xuLy(id);
        return service.huy(id, principal.getName(), request.lyDoHuy());
    }
    @PostMapping("/{id}/hoan-tien")
    public QuanTriDonDto hoanTien(@PathVariable Long id, Principal principal, @Valid @RequestBody YeuCauHoanTien request) {
        return service.hoanTien(id, principal.getName(), request);
    }
    @PutMapping("/{id}/hoan-thanh")
    public QuanTriDonDto hoanThanh(@PathVariable Long id, Principal principal) {
        return service.hoanThanh(id, principal.getName());
    }
    @PutMapping("/{id}/check-in")
    public QuanTriDonDto checkIn(@PathVariable Long id,Principal principal) { return service.checkIn(id,principal.getName()); }
}
