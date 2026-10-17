package com.dong.dat_san.service;

import com.dong.dat_san.repository.KhoDuLieuNguoiDung;
import java.util.Locale;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DichVuChiTietNguoiDung implements UserDetailsService {
    private final KhoDuLieuNguoiDung users;

    public DichVuChiTietNguoiDung(KhoDuLieuNguoiDung users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        var user = users.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản."));
        return User.withUsername(user.getEmail()).password(user.getMatKhauBam())
                .roles(user.getVaiTro().getMa()).disabled(!user.isDangHoatDong()).build();
    }
}
