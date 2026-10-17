package com.dong.dat_san.controller;

import com.dong.dat_san.config.JwtTokenProvider;
import com.dong.dat_san.dto.PhanHoiDangNhapDto;
import com.dong.dat_san.dto.YeuCauDangNhap;
import com.dong.dat_san.entity.NguoiDung;
import com.dong.dat_san.repository.KhoDuLieuNguoiDung;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/xac-thuc")
public class XacThucController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private KhoDuLieuNguoiDung khoDuLieuNguoiDung;

    @PostMapping("/dang-nhap")
    public ResponseEntity<?> dangNhap(@RequestBody YeuCauDangNhap yeuCau) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(yeuCau.getEmail(), yeuCau.getMatKhau())
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            // Truyền username (String) vào token provider
            String jwt = jwtTokenProvider.generateToken(userDetails.getUsername());

            NguoiDung nguoiDung = khoDuLieuNguoiDung.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            // Lấy tên vai trò an toàn từ entity VaiTro
            String tenVaiTro = nguoiDung.getVaiTro() != null ? nguoiDung.getVaiTro().toString() : "CUSTOMER";

            PhanHoiDangNhapDto response = new PhanHoiDangNhapDto(
                jwt, 
                nguoiDung.getEmail(), 
                nguoiDung.getHoTen(), 
                tenVaiTro
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Email hoặc mật khẩu không chính xác!");
        }
    }
}