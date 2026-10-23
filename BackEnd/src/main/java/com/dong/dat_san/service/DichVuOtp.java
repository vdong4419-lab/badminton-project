package com.dong.dat_san.service;

import com.dong.dat_san.entity.NguoiDung;
import com.dong.dat_san.repository.KhoDuLieuNguoiDung;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.*;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DichVuOtp {
    public static final String VERIFY="XAC_MINH_EMAIL", RESET="DAT_LAI_MAT_KHAU";
    private final JdbcTemplate jdbc;
    private final KhoDuLieuNguoiDung users;
    private final PasswordEncoder encoder;
    private final DichVuGuiMail mail;
    private final PhienBanMatKhau versions;
    private final Clock clock;
    private final TransactionTemplate tx;
    private final SecureRandom random=new SecureRandom();
    public DichVuOtp(JdbcTemplate jdbc,KhoDuLieuNguoiDung users,PasswordEncoder encoder,DichVuGuiMail mail,
                     PhienBanMatKhau versions,Clock clock,PlatformTransactionManager manager) {
        this.jdbc=jdbc;this.users=users;this.encoder=encoder;this.mail=mail;this.versions=versions;this.clock=clock;
        tx=new TransactionTemplate(manager);tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
    public static String email(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    public boolean daXacMinh(Long userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM xac_minh_email WHERE nguoi_dung_id=? AND xac_minh_luc IS NULL",Long.class,userId)==0;
    }
    @Transactional
    public void dangKyMoi(NguoiDung user) {
        jdbc.update("INSERT INTO xac_minh_email(nguoi_dung_id,xac_minh_luc) VALUES (?,NULL)",user.getId());
        gui(user,VERIFY);
    }
    @Transactional
    public void guiLai(String email,String purpose) {
        var user=users.khoaTheoEmail(email(email)).filter(NguoiDung::isDangHoatDong).orElse(null);
        if (user==null || (VERIFY.equals(purpose) && daXacMinh(user.getId()))) return;
        gui(user,purpose);
    }
    private void gui(NguoiDung user,String purpose) {
        var now=LocalDateTime.now(clock);
        var previous=jdbc.query("SELECT gui_luc FROM ma_otp WHERE nguoi_dung_id=? AND muc_dich=? FOR UPDATE",
                (r,i) -> r.getTimestamp(1).toLocalDateTime(),user.getId(),purpose);
        if (!previous.isEmpty() && previous.get(0).plusSeconds(60).isAfter(now)) return;
        String code=String.format(Locale.ROOT,"%06d",random.nextInt(1_000_000));
        jdbc.update("""
                INSERT INTO ma_otp(nguoi_dung_id,muc_dich,ma_bam,het_han,gui_luc,so_lan_sai,da_dung) VALUES (?,?,?,?,?,0,FALSE)
                ON DUPLICATE KEY UPDATE ma_bam=VALUES(ma_bam),het_han=VALUES(het_han),gui_luc=VALUES(gui_luc),so_lan_sai=0,da_dung=FALSE
                """,user.getId(),purpose,encoder.encode(code),now.plusMinutes(10),now);
        mail.guiOtp(user.getEmail(),code,purpose);
    }
    public void xacNhan(String email,String code,String newPassword) {
        if (newPassword!=null && newPassword.getBytes(StandardCharsets.UTF_8).length>72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Mật khẩu mới tối đa 72 byte UTF-8.");
        String purpose=newPassword==null ? VERIFY : RESET;
        boolean accepted=Boolean.TRUE.equals(tx.execute(status -> {
            var user=users.khoaTheoEmail(email(email)).filter(NguoiDung::isDangHoatDong).orElse(null);
            if (user==null) return false;
            var rows=jdbc.query("SELECT ma_bam,het_han,da_dung,so_lan_sai FROM ma_otp WHERE nguoi_dung_id=? AND muc_dich=? FOR UPDATE",
                    (r,i) -> new MaOtp(r.getString("ma_bam"),r.getTimestamp("het_han").toLocalDateTime(),r.getBoolean("da_dung"),r.getInt("so_lan_sai")),user.getId(),purpose);
            if (rows.isEmpty()) return false;
            var row=rows.get(0);
            if (row.daDung() || row.soLanSai()>=5 || !row.hetHan().isAfter(LocalDateTime.now(clock))) return false;
            if (!encoder.matches(code,row.maBam())) {
                jdbc.update("UPDATE ma_otp SET so_lan_sai=so_lan_sai+1 WHERE nguoi_dung_id=? AND muc_dich=?",user.getId(),purpose);
                return false;
            }
            jdbc.update("UPDATE ma_otp SET da_dung=TRUE WHERE nguoi_dung_id=? AND muc_dich=?",user.getId(),purpose);
            if (newPassword==null) {
                jdbc.update("UPDATE xac_minh_email SET xac_minh_luc=? WHERE nguoi_dung_id=?",LocalDateTime.now(clock),user.getId());
            } else {
                user.setMatKhauBam(encoder.encode(newPassword));versions.thayDoi(user.getId());
            }
            return true;
        }));
        if (!accepted) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"OTP không hợp lệ, đã dùng, hết hạn hoặc đã nhập sai quá 5 lần.");
    }
    private record MaOtp(String maBam,LocalDateTime hetHan,boolean daDung,int soLanSai) {}
}
