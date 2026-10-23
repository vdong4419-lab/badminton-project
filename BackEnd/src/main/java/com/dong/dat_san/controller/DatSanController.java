package com.dong.dat_san.controller;

import com.dong.dat_san.dto.*;
import com.dong.dat_san.service.DatSanService;
import com.dong.dat_san.service.DichVuHetHanGiuCho;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dat-san")
public class DatSanController {
    private final DatSanService service;
    private final DichVuHetHanGiuCho expiry;

    public DatSanController(DatSanService service, DichVuHetHanGiuCho expiry) {
        this.service = service;
        this.expiry = expiry;
    }

    @PostMapping
    public ResponseEntity<DatSanDto> taoDatSan(@Valid @RequestBody YeuCauDatSan request, Principal principal) {
        expiry.quet();
        return ResponseEntity.status(201).body(service.taoDatSan(principal.getName(), request.getDanhSachLichSanIds()));
    }

    @GetMapping("/lich-su")
    public List<DatSanDto> xemLichSuDatSan(Principal principal) {
        expiry.quet();
        return service.xemLichSu(principal.getName());
    }

    @PostMapping("/bao-gia")
    public UuDaiDto baoGia(@Valid @RequestBody YeuCauDatSan request, Principal principal) {
        expiry.quet();
        return service.baoGia(principal.getName(),request.getDanhSachLichSanIds());
    }

    @GetMapping("/{id}")
    public DatSanDto chiTiet(@PathVariable Long id, Principal principal) {
        expiry.xuLy(id);
        return service.chiTietCuaToi(principal.getName(),id);
    }

    @PostMapping("/{id}/bao-chuyen-khoan")
    public DatSanDto baoChuyenKhoan(@PathVariable Long id, Principal principal) {
        expiry.xuLy(id);
        return service.baoChuyenKhoan(principal.getName(),id);
    }

    @PutMapping("/{id}/huy")
    public String huyDatSan(@PathVariable Long id, Principal principal,
                            @Valid @RequestBody(required = false) YeuCauHuyDatSan request) {
        expiry.xuLy(id);
        return service.huyDatSan(principal.getName(), id, request == null ? null : request.lyDoHuy());
    }
}
