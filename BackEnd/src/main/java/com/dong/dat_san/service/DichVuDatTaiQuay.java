package com.dong.dat_san.service;

import com.dong.dat_san.dto.*;
import com.dong.dat_san.repository.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
public class DichVuDatTaiQuay {
    private final KhoDuLieuNguoiDung users;
    private final KhoDuLieuDatSan orders;
    private final DatSanService booking;
    private final DichVuQuanTriDon operations;
    private final QuyenVanHanh access;
    private final JdbcTemplate jdbc;
    private final NhatKyQuanTri audit;
    public DichVuDatTaiQuay(KhoDuLieuNguoiDung users,KhoDuLieuDatSan orders,DatSanService booking,
            DichVuQuanTriDon operations,QuyenVanHanh access,JdbcTemplate jdbc,NhatKyQuanTri audit) {
        this.users=users;this.orders=orders;this.booking=booking;this.operations=operations;
        this.access=access;this.jdbc=jdbc;this.audit=audit;
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> timKhach(String term) {
        if (term==null || term.trim().length()<3 || term.length()>100) return List.of();
        String value="%"+term.trim().replace("!","!!").replace("%","!%").replace("_","!_")+"%";
        return jdbc.queryForList("""
                SELECT n.id,n.ho_ten AS hoTen,n.email,n.so_dien_thoai AS soDienThoai
                FROM nguoi_dung n JOIN vai_tro v ON v.id=n.vai_tro_id
                WHERE n.dang_hoat_dong=TRUE AND v.ma='CUSTOMER'
                AND (n.ho_ten LIKE ? ESCAPE '!' OR n.email LIKE ? ESCAPE '!' OR n.so_dien_thoai LIKE ? ESCAPE '!') ORDER BY n.ho_ten LIMIT 10
                """,value,value,value);
    }
    @Transactional
    public QuanTriDonDto tao(YeuCauDatTaiQuay request) {
        var customer=users.findById(request.nguoiDungId())
                .filter(n -> n.isDangHoatDong() && "CUSTOMER".equals(n.getVaiTro().getMa()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,"Chọn khách hàng đang hoạt động."));
        if (access.laNhanVien()) {
            String marks=String.join(",",Collections.nCopies(request.danhSachLichSanIds().size(),"?"));
            var params=new ArrayList<Object>(request.danhSachLichSanIds()); params.add(access.homNay());
            Long otherDays=jdbc.queryForObject("SELECT COUNT(*) FROM lich_san WHERE id IN ("+marks+") AND ngay_choi<>?",Long.class,params.toArray());
            if (otherDays>0) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Nhân viên chỉ đặt tại quầy cho hôm nay.");
        }
        String operator=SecurityContextHolder.getContext().getAuthentication().getName();
        var result=booking.taoDatSan(customer.getEmail(),request.danhSachLichSanIds());
        var actor=users.findByEmail(operator).orElseThrow();
        // Giao dịch vẫn chưa commit; ghi đúng người tạo đơn tại quầy thay cho luồng tự đặt.
        jdbc.update("""
                UPDATE lich_su_dat_san SET nguoi_thuc_hien_id=?,ghi_chu='Tạo đơn tại quầy cho khách'
                WHERE dat_san_id=? AND trang_thai_cu IS NULL
                """,actor.getId(),result.id());
        orders.findById(result.id()).orElseThrow().setGhiChu("Đặt tại quầy");
        var confirmed=operations.xacNhan(result.id(),operator);
        audit.ghi("DAT_TAI_QUAY","DON:"+result.id(),"Khách hàng #"+customer.getId());
        return confirmed;
    }
}
