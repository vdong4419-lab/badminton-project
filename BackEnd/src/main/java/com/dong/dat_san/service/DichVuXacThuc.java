package com.dong.dat_san.service;

import com.dong.dat_san.dto.YeuCauDangKy;
import com.dong.dat_san.entity.NguoiDung;
import com.dong.dat_san.entity.VaiTro;
import com.dong.dat_san.repository.KhoDuLieuNguoiDung;
import com.dong.dat_san.repository.KhoDuLieuVaiTro;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class DichVuXacThuc {

    @Autowired
    private KhoDuLieuNguoiDung khoDuLieuNguoiDung;

    @Autowired
    private KhoDuLieuVaiTro khoDuLieuVaiTro;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public String dangKyTaiKhoan(YeuCauDangKy yeuCau) {
        // Kiểm tra email đã tồn tại chưa
        if (khoDuLieuNguoiDung.findByEmail(yeuCau.getEmail()).isPresent()) {
            throw new RuntimeException("Email này đã được sử dụng!");
        }

        // Mặc định lấy vai trò CUSTOMER
        VaiTro vaiTroCustomer = khoDuLieuVaiTro.findByMa("CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vai trò mặc định trong hệ thống!"));

        NguoiDung nguoiDung = new NguoiDung();
        nguoiDung.setHoTen(yeuCau.getHoTen());
        nguoiDung.setEmail(yeuCau.getEmail());
        nguoiDung.setSoDienThoai(yeuCau.getSoDienThoai());
        // Mã hóa mật khẩu bằng BCrypt
        nguoiDung.setMatKhauBam(passwordEncoder.encode(yeuCau.getMatKhau()));
        nguoiDung.setVaiTro(vaiTroCustomer);

        khoDuLieuNguoiDung.save(nguoiDung);

        return "Đăng ký tài khoản thành công cho email: " + yeuCau.getEmail();
    }
}