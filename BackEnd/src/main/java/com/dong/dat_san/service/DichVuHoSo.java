package com.dong.dat_san.service;

import com.dong.dat_san.dto.*;
import com.dong.dat_san.entity.NguoiDung;
import com.dong.dat_san.repository.KhoDuLieuNguoiDung;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DichVuHoSo {
    private final KhoDuLieuNguoiDung users;
    private final DichVuKhachQuen loyalty;
    private final PasswordEncoder encoder;
    private final PhienBanMatKhau versions;
    public DichVuHoSo(KhoDuLieuNguoiDung users, DichVuKhachQuen loyalty, PasswordEncoder encoder, PhienBanMatKhau versions) {
        this.users=users; this.loyalty=loyalty; this.encoder=encoder; this.versions=versions;
    }
    public record HoSo(Long id, String email, String hoTen, String soDienThoai, String vaiTro,
                       LocalDateTime ngayThamGia, DichVuKhachQuen.ThanhVien thanhVien) {}

    @Transactional(readOnly=true)
    public HoSo xem(String email) { return view(user(email)); }

    @Transactional
    public HoSo sua(String email, YeuCauHoSo request) {
        var user=locked(email);
        String phone=request.soDienThoai();
        phone=phone == null || phone.isBlank() ? null : phone;
        if (phone != null && users.existsBySoDienThoaiAndIdNot(phone,user.getId()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Số điện thoại đã được tài khoản khác sử dụng.");
        user.setHoTen(request.hoTen().trim());
        user.setSoDienThoai(phone);
        users.saveAndFlush(user);
        return view(user);
    }

    @Transactional
    public void doiMatKhau(String email, YeuCauDoiMatKhau request) {
        var user=locked(email);
        if (!encoder.matches(request.matKhauHienTai(),user.getMatKhauBam()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Mật khẩu hiện tại không chính xác.");
        if (request.matKhauMoi().getBytes(StandardCharsets.UTF_8).length>72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Mật khẩu mới tối đa 72 byte UTF-8.");
        if (encoder.matches(request.matKhauMoi(),user.getMatKhauBam()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Mật khẩu mới phải khác mật khẩu hiện tại.");
        user.setMatKhauBam(encoder.encode(request.matKhauMoi()));
        versions.thayDoi(user.getId());
    }

    private NguoiDung user(String email) {
        return users.findByEmail(email).filter(NguoiDung::isDangHoatDong)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Vui lòng đăng nhập lại."));
    }
    private NguoiDung locked(String email) {
        return users.khoaTheoEmail(email).filter(NguoiDung::isDangHoatDong)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Vui lòng đăng nhập lại."));
    }
    private HoSo view(NguoiDung user) {
        return new HoSo(user.getId(),user.getEmail(),user.getHoTen(),user.getSoDienThoai(),
                user.getVaiTro().getMa(),user.getNgayTao(),loyalty.thongTin(user));
    }
}
