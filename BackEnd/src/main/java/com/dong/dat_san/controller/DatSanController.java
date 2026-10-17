package com.dong.dat_san.controller;

import com.dong.dat_san.dto.YeuCauDatSan;
import com.dong.dat_san.entity.NguoiDung;
import com.dong.dat_san.entity.ThucTheDatSan;
import com.dong.dat_san.repository.KhoDuLieuDatSan;
import com.dong.dat_san.repository.KhoDuLieuNguoiDung;
import com.dong.dat_san.service.DatSanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dat-san")
public class DatSanController {

    @Autowired
    private DatSanService datSanService;

    @Autowired
    private KhoDuLieuNguoiDung khoDuLieuNguoiDung;

    @Autowired
    private KhoDuLieuDatSan khoDuLieuDatSan;

        @PostMapping
        public ResponseEntity<String> taoDatSan(@RequestBody YeuCauDatSan yeuCau, Principal principal) {
            try {
                // Lấy email từ token JWT thông qua Principal
                String email = principal.getName();
                NguoiDung nguoiDung = khoDuLieuNguoiDung.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

                // Sử dụng ID chuẩn từ hệ thống xác thực
                String thongBao = datSanService.taoDatSan(nguoiDung.getId(), yeuCau.getDanhSachLichSanIds());
                return ResponseEntity.ok(thongBao);
            } catch (RuntimeException e) {
                return ResponseEntity.badRequest().body("Lỗi đặt sân: " + e.getMessage());
            }
        }

    @GetMapping("/lich-su")
    public ResponseEntity<?> xemLichSuDatSan(Principal principal) {
        String email = principal.getName();
        
        NguoiDung nguoiDung = khoDuLieuNguoiDung.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        List<ThucTheDatSan> danhSachDon = khoDuLieuDatSan.findByNguoiDungId(nguoiDung.getId());
        return ResponseEntity.ok(danhSachDon);
    }

    @PutMapping("/{id}/huy")
    public ResponseEntity<String> huyDatSan(@PathVariable Long id, Principal principal, @RequestBody(required = false) Map<String, String> body) {
        try {
            String email = principal.getName();
            NguoiDung nguoiDung = khoDuLieuNguoiDung.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            String lyDoHuy = body != null ? body.getOrDefault("lyDoHuy", "Không có lý do") : "Không có lý do";

            String thongBao = datSanService.huyDatSan(nguoiDung.getId(), id, lyDoHuy);
            return ResponseEntity.ok(thongBao);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Lỗi hủy đơn: " + e.getMessage());
        }
    }
}