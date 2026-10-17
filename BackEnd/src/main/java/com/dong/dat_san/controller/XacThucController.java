package com.dong.dat_san.controller;

import com.dong.dat_san.config.JwtTokenProvider;
import com.dong.dat_san.dto.*;
import com.dong.dat_san.repository.KhoDuLieuNguoiDung;
import com.dong.dat_san.service.DichVuXacThuc;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/xac-thuc")
public class XacThucController {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokens;
    private final KhoDuLieuNguoiDung users;
    private final DichVuXacThuc registration;

    public XacThucController(AuthenticationManager authenticationManager, JwtTokenProvider tokens,
                            KhoDuLieuNguoiDung users, DichVuXacThuc registration) {
        this.authenticationManager = authenticationManager;
        this.tokens = tokens;
        this.users = users;
        this.registration = registration;
    }

    @PostMapping("/dang-ky")
    public ResponseEntity<String> dangKy(@Valid @RequestBody YeuCauDangKy request) {
        return ResponseEntity.status(201).body(registration.dangKyTaiKhoan(request));
    }

    @PostMapping("/dang-nhap")
    public PhanHoiDangNhapDto dangNhap(@Valid @RequestBody YeuCauDangNhap request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getMatKhau()));
        var user = users.findByEmail(authentication.getName()).orElseThrow();
        return new PhanHoiDangNhapDto(tokens.generateToken(user.getEmail()), user.getEmail(),
                user.getHoTen(), user.getVaiTro().getMa());
    }
}
