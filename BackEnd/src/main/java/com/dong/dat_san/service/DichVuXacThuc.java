package com.dong.dat_san.service;

import com.dong.dat_san.dto.YeuCauDangKy;
import com.dong.dat_san.entity.NguoiDung;
import com.dong.dat_san.entity.VaiTro;
import com.dong.dat_san.repository.KhoDuLieuNguoiDung;
import com.dong.dat_san.repository.KhoDuLieuVaiTro;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class DichVuXacThuc {

    @Autowired
    private KhoDuLieuNguoiDung khoDuLieuNguoiDung;

    @Autowired
    private KhoDuLieuVaiTro khoDuLieuVaiTro;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired private DichVuOtp otp;

    @Transactional
    public String dangKyTaiKhoan(YeuCauDangKy yeuCau) {
        String email = yeuCau.getEmail().trim().toLowerCase(Locale.ROOT);
        if (yeuCau.getMatKhau().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu tối đa 72 byte UTF-8.");
        }
        // Kiểm tra email đã tồn tại chưa
        if (khoDuLieuNguoiDung.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email này đã được sử dụng!");
        }

        // Mặc định lấy vai trò CUSTOMER
        VaiTro vaiTroCustomer = khoDuLieuVaiTro.findByMa("CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vai trò mặc định trong hệ thống!"));

        NguoiDung nguoiDung = new NguoiDung();
        nguoiDung.setHoTen(yeuCau.getHoTen().trim());
        nguoiDung.setEmail(email);
        String phone = yeuCau.getSoDienThoai();
        nguoiDung.setSoDienThoai(phone == null || phone.isBlank() ? null : phone.trim());
        // Mã hóa mật khẩu bằng BCrypt
        nguoiDung.setMatKhauBam(passwordEncoder.encode(yeuCau.getMatKhau()));
        nguoiDung.setVaiTro(vaiTroCustomer);

        khoDuLieuNguoiDung.saveAndFlush(nguoiDung);
        otp.dangKyMoi(nguoiDung);

        return "Đã gửi mã OTP xác minh email. Vui lòng kiểm tra hộp thư để hoàn tất đăng ký.";
    }
}
