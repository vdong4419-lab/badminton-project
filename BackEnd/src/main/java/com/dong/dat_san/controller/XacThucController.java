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
import java.security.Principal;
import java.util.Map;
import com.dong.dat_san.service.DichVuOtp;
import com.dong.dat_san.service.GioiHanYeuCau;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/xac-thuc")
public class XacThucController {
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(XacThucController.class);
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokens;
    private final KhoDuLieuNguoiDung users;
    private final DichVuXacThuc registration;
    private final com.dong.dat_san.service.PhienBanMatKhau versions;
    private final DichVuOtp otp;
    private final GioiHanYeuCau limits;

    public XacThucController(AuthenticationManager authenticationManager, JwtTokenProvider tokens,
                            KhoDuLieuNguoiDung users, DichVuXacThuc registration, com.dong.dat_san.service.PhienBanMatKhau versions,
                            DichVuOtp otp, GioiHanYeuCau limits) {
        this.authenticationManager = authenticationManager;
        this.tokens = tokens;
        this.users = users;
        this.registration = registration;
        this.versions = versions;
        this.otp=otp;this.limits=limits;
    }

    @PostMapping("/dang-ky")
    public ResponseEntity<String> dangKy(@Valid @RequestBody YeuCauDangKy request,HttpServletRequest http) {
        limits.kiemTra("REGISTER_IP:"+http.getRemoteAddr(),20,3600);
        return ResponseEntity.status(201).body(registration.dangKyTaiKhoan(request));
    }

    @GetMapping("/toi")
    public Map<String, String> toi(Principal principal) {
        var user = users.findByEmail(principal.getName()).orElseThrow();
        return Map.of("email", user.getEmail(), "hoTen", user.getHoTen(), "vaiTro", user.getVaiTro().getMa());
    }

    @PostMapping("/dang-nhap")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public PhanHoiDangNhapDto dangNhap(@Valid @RequestBody YeuCauDangNhap request,HttpServletRequest http) {
        String email=DichVuOtp.email(request.getEmail());
        limits.kiemTra("LOGIN_IP:"+http.getRemoteAddr(),50,900);
        limits.kiemTra("LOGIN_EMAIL:"+email,5,900);
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getMatKhau()));
        var user = users.findByEmail(authentication.getName()).orElseThrow();
        if (!otp.daXacMinh(user.getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Vui lòng xác minh email bằng OTP trước khi đăng nhập.");
        limits.xoa("LOGIN_EMAIL:"+email);
        return new PhanHoiDangNhapDto(tokens.generateToken(user.getEmail(),versions.hienTai(user.getEmail())), user.getEmail(),
                user.getHoTen(), user.getVaiTro().getMa());
    }

    @PostMapping("/gui-lai-otp")
    public Map<String,String> guiLai(@Valid @RequestBody YeuCauEmail request,HttpServletRequest http) {
        gioiHanGui(request.email(),http);otp.guiLai(request.email(),DichVuOtp.VERIFY);
        return Map.of("message","Nếu tài khoản cần xác minh, mã OTP đã được gửi. Vui lòng kiểm tra hộp thư và thư rác.");
    }
    @PostMapping("/quen-mat-khau")
    public Map<String,String> quenMatKhau(@Valid @RequestBody YeuCauEmail request,HttpServletRequest http) {
        gioiHanGui(request.email(),http);
        try { otp.guiLai(request.email(),DichVuOtp.RESET); }
        catch (ResponseStatusException error) {
            if (error.getStatusCode().value()!=503) throw error;
            // Keep the same public response for an unknown email and a delivery failure.
            log.warn("Chưa gửi được email khôi phục mật khẩu. Kiểm tra cấu hình Gmail SMTP trên máy chủ.");
        }
        return Map.of("message","Nếu email có tài khoản đang hoạt động, mã đặt lại mật khẩu đã được gửi.");
    }
    @PostMapping("/xac-minh-email")
    public Map<String,String> xacMinh(@Valid @RequestBody YeuCauOtp request,HttpServletRequest http) {
        gioiHanNhap(request.email(),http);otp.xacNhan(request.email(),request.otp(),null);
        return Map.of("message","Đã xác minh email. Bạn có thể đăng nhập.");
    }
    @PostMapping("/dat-lai-mat-khau")
    public Map<String,String> datLai(@Valid @RequestBody YeuCauDatLaiMatKhau request,HttpServletRequest http) {
        gioiHanNhap(request.email(),http);otp.xacNhan(request.email(),request.otp(),request.matKhauMoi());
        limits.xoa("LOGIN_EMAIL:"+DichVuOtp.email(request.email()));
        return Map.of("message","Đã đặt lại mật khẩu và thu hồi các phiên cũ. Vui lòng đăng nhập lại.");
    }
    private void gioiHanGui(String email,HttpServletRequest http) {
        limits.kiemTra("OTP_SEND_IP:"+http.getRemoteAddr(),20,3600);
        limits.kiemTra("OTP_SEND_EMAIL:"+DichVuOtp.email(email),5,3600);
    }
    private void gioiHanNhap(String email,HttpServletRequest http) {
        limits.kiemTra("OTP_CHECK_IP:"+http.getRemoteAddr(),50,900);
        limits.kiemTra("OTP_CHECK_EMAIL:"+DichVuOtp.email(email),20,900);
    }
}
