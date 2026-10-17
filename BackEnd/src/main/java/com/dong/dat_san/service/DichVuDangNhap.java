package com.dong.dat_san.service;

import com.dong.dat_san.config.JwtTokenProvider;
import com.dong.dat_san.entity.NguoiDung;
import com.dong.dat_san.repository.KhoDuLieuNguoiDung;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class DichVuDangNhap {

    @Autowired
    private KhoDuLieuNguoiDung khoDuLieuNguoiDung;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    public String dangNhap(String email, String matKhau) {
        NguoiDung nguoiDung = khoDuLieuNguoiDung.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại trong hệ thống!"));

        // Kiểm tra mật khẩu mã hóa
        if (!passwordEncoder.matches(matKhau, nguoiDung.getMatKhauBam())) {
            throw new RuntimeException("Mật khẩu không chính xác!");
        }

        // Sinh token trả về cho client
        return jwtTokenProvider.generateToken(nguoiDung.getEmail());
    }
}