package com.dong.dat_san.service;

import com.dong.dat_san.dto.YeuCauTaiKhoan;
import com.dong.dat_san.entity.NguoiDung;
import com.dong.dat_san.repository.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class DichVuTaiKhoan {
    private final JdbcTemplate jdbc;
    private final KhoDuLieuNguoiDung users;
    private final KhoDuLieuVaiTro roles;
    private final PasswordEncoder passwords;
    private final NhatKyQuanTri audit;
    public DichVuTaiKhoan(JdbcTemplate jdbc, KhoDuLieuNguoiDung users, KhoDuLieuVaiTro roles,
            PasswordEncoder passwords, NhatKyQuanTri audit) {
        this.jdbc=jdbc; this.users=users; this.roles=roles; this.passwords=passwords; this.audit=audit;
    }
    @Transactional(readOnly=true)
    public Map<String,Object> danhSach(String search,int page) {
        if (page<0 || search.length()>100) throw error(400,"Bộ lọc không hợp lệ.");
        String term="%"+search.trim()+"%";
        String where=" WHERE n.ho_ten LIKE ? OR n.email LIKE ? OR n.so_dien_thoai LIKE ? ";
        var rows=jdbc.queryForList("""
                SELECT n.id,n.ho_ten AS hoTen,n.email,n.so_dien_thoai AS soDienThoai,
                       v.ma AS vaiTro,n.dang_hoat_dong AS dangHoatDong
                FROM nguoi_dung n JOIN vai_tro v ON v.id=n.vai_tro_id
                """+where+" ORDER BY n.id DESC LIMIT 20 OFFSET ?",term,term,term,page*20L);
        long total=jdbc.queryForObject("SELECT COUNT(*) FROM nguoi_dung n"+where,Long.class,term,term,term);
        return Map.of("items",rows,"totalElements",total,"totalPages",(total+19)/20,"page",page);
    }
    @Transactional
    public Map<String,Object> luu(Long id,YeuCauTaiKhoan input) {
        // Mọi thay đổi tài khoản đi qua cùng khóa vai trò ADMIN để bảo vệ admin cuối cùng.
        jdbc.queryForObject("SELECT id FROM vai_tro WHERE ma='ADMIN' FOR UPDATE",Long.class);
        String actorEmail=SecurityContextHolder.getContext().getAuthentication().getName();
        var actor=users.findByEmail(actorEmail).filter(n -> n.isDangHoatDong() && "ADMIN".equals(n.getVaiTro().getMa()))
                .orElseThrow(() -> error(403,"Bạn không còn quyền quản lý tài khoản."));
        var user=id==null ? new NguoiDung() : users.khoa(id).orElseThrow(() -> error(404,"Không tìm thấy tài khoản."));
        String email=input.email().trim().toLowerCase(Locale.ROOT);
        if (id!=null) {
            if (!user.getEmail().equalsIgnoreCase(email)) throw error(400,"Email đăng nhập không được đổi tại màn hình quản trị.");
            boolean removingAdmin="ADMIN".equals(user.getVaiTro().getMa()) && (!input.dangHoatDong() || !"ADMIN".equals(input.vaiTro()));
            if (id.equals(actor.getId()) && (!input.dangHoatDong() || !input.vaiTro().equals(user.getVaiTro().getMa()))) {
                throw error(409,"Không tự khóa hoặc thay đổi quyền tài khoản đang đăng nhập.");
            }
            if (removingAdmin && jdbc.queryForObject("""
                    SELECT COUNT(*) FROM nguoi_dung n JOIN vai_tro v ON v.id=n.vai_tro_id
                    WHERE v.ma='ADMIN' AND n.dang_hoat_dong=TRUE
                    """,Long.class)<=1) throw error(409,"Phải giữ ít nhất một admin đang hoạt động.");
            if (input.matKhau()!=null) throw error(400,"Màn hình này không thay đổi mật khẩu tài khoản hiện có.");
        } else {
            if (input.matKhau()==null || input.matKhau().isBlank() || input.matKhau().getBytes(StandardCharsets.UTF_8).length>72) {
                throw error(400,"Tài khoản mới cần mật khẩu từ 8 ký tự và tối đa 72 byte UTF-8.");
            }
            user.setEmail(email); user.setMatKhauBam(passwords.encode(input.matKhau()));
        }
        user.setHoTen(input.hoTen().trim());
        user.setSoDienThoai(input.soDienThoai()==null || input.soDienThoai().isBlank()?null:input.soDienThoai().trim());
        user.setVaiTro(roles.findByMa(input.vaiTro()).orElseThrow(() -> error(400,"Vai trò không tồn tại.")));
        user.setDangHoatDong(input.dangHoatDong());
        users.saveAndFlush(user);
        audit.ghi(id==null?"TAO_TAI_KHOAN":"SUA_TAI_KHOAN","NGUOI_DUNG:"+user.getId(),
                "Email: "+email+" | Quyền: "+input.vaiTro()+" | Hoạt động: "+input.dangHoatDong());
        return Map.of("id",user.getId(),"email",user.getEmail(),"vaiTro",user.getVaiTro().getMa());
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> nhatKy(int page) {
        if (page<0) throw error(400,"Trang không hợp lệ.");
        return jdbc.queryForList("""
                SELECT h.id,n.ho_ten AS nguoiThucHien,h.hanh_dong AS hanhDong,h.doi_tuong AS doiTuong,
                       h.noi_dung AS noiDung,h.ngay_tao AS ngayTao
                FROM nhat_ky_quan_tri h JOIN nguoi_dung n ON n.id=h.nguoi_thuc_hien_id
                ORDER BY h.id DESC LIMIT 20 OFFSET ?
                """,page*20L);
    }
    private static ResponseStatusException error(int code,String message) { return new ResponseStatusException(HttpStatus.valueOf(code),message); }
}
