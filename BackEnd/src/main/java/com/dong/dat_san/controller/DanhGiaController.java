package com.dong.dat_san.controller;
import com.dong.dat_san.dto.YeuCauDanhGia;
import com.dong.dat_san.service.DichVuDanhGia;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

@RestController
public class DanhGiaController {
    private final DichVuDanhGia service;
    public DanhGiaController(DichVuDanhGia service) { this.service=service; }
    @GetMapping("/api/san/{id}/danh-gia") public Object danhSach(@PathVariable Long id,@RequestParam(defaultValue="0") int page) { return service.congKhai(id,page); }
    @GetMapping("/api/dat-san/{id}/danh-gia") public Object cuaDon(@PathVariable Long id,Principal p) { return service.cuaDon(p.getName(),id); }
    @PostMapping("/api/dat-san/{id}/danh-gia") @ResponseStatus(HttpStatus.CREATED)
    public Object gui(@PathVariable Long id,Principal p,@Valid @RequestBody YeuCauDanhGia request) { return service.gui(p.getName(),id,request); }
}
