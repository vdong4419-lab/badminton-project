package com.dong.dat_san.service;

import com.dong.dat_san.dto.*;
import com.dong.dat_san.entity.*;
import com.dong.dat_san.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DichVuQuanLySan {
    private final SanRepository courts;
    private final KhoDuLieuLichSan slots;
    private final DichVuLichSan schedules;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final NhatKyQuanTri audit;
    private final QuyenVanHanh access;
    public DichVuQuanLySan(SanRepository courts, KhoDuLieuLichSan slots, DichVuLichSan schedules,
            JdbcTemplate jdbc, Clock clock, NhatKyQuanTri audit, QuyenVanHanh access) {
        this.courts=courts; this.slots=slots; this.schedules=schedules; this.jdbc=jdbc;
        this.clock=clock; this.audit=audit; this.access=access;
    }
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly=true)
    public List<San> danhSach() { return courts.findAll(org.springframework.data.domain.Sort.by("id")); }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public San luu(Long id, YeuCauSuaSan input) {
        San court=id == null ? new San() : courts.khoa(id).orElseThrow(() -> error(404,"Không tìm thấy sân."));
        if (id != null && !input.dangHoatDong()) {
            var occupied=jdbc.queryForList("""
                    SELECT g.lich_san_id FROM giu_cho g JOIN lich_san l ON l.id=g.lich_san_id
                    JOIN khung_gio k ON k.id=l.khung_gio_id
                    WHERE l.san_id=? AND TIMESTAMP(l.ngay_choi,k.gio_ket_thuc)>? FOR UPDATE
                    """,Long.class,id,LocalDateTime.now(clock));
            if (!occupied.isEmpty()) throw error(409,"Sân còn lịch đã đặt chưa kết thúc. Hãy xử lý các đơn trước khi tạm ngừng sân.");
        }
        court.setTenSan(input.tenSan().trim()); court.setGiaMoiGio(input.giaMoiGio());
        court.setMoTa(input.moTa()); court.setAnhDaiDien(input.anhDaiDien());
        court.setDangHoatDong(input.dangHoatDong()); courts.saveAndFlush(court);
        schedules.moLich();
        audit.ghi(id == null ? "TAO_SAN" : "SUA_SAN","SAN:"+court.getId(),
                "Tên: "+court.getTenSan()+" | Giá mặc định/giờ: "+court.getGiaMoiGio()+" | Mở: "+court.isDangHoatDong());
        return court;
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Transactional(readOnly=true)
    public List<Map<String,Object>> lich(LocalDate day) {
        day=access.ngayDuocXem(day);
        if (day == null) day=LocalDate.now(clock);
        return jdbc.queryForList("""
                SELECT l.id, l.san_id AS sanId, s.ten_san AS tenSan, s.dang_hoat_dong AS sanHoatDong,
                       l.ngay_choi AS ngayChoi, k.gio_bat_dau AS gioBatDau, k.gio_ket_thuc AS gioKetThuc,
                       l.gia, l.mo_dat AS moDat, d.id AS donId, d.trang_thai AS trangThaiDon,
                       n.ho_ten AS khachHang
                FROM lich_san l JOIN san s ON s.id=l.san_id JOIN khung_gio k ON k.id=l.khung_gio_id
                LEFT JOIN giu_cho g ON g.lich_san_id=l.id
                LEFT JOIN chi_tiet_dat_san c ON c.id=g.chi_tiet_dat_san_id
                LEFT JOIN dat_san d ON d.id=c.dat_san_id LEFT JOIN nguoi_dung n ON n.id=d.nguoi_dung_id
                WHERE l.ngay_choi=? ORDER BY k.gio_bat_dau,s.id
                """,day);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void suaLich(Long id, YeuCauSuaLich input) {
        Long courtId=jdbc.query("SELECT san_id FROM lich_san WHERE id=?",(r,i)->r.getLong(1),id).stream()
                .findFirst().orElseThrow(() -> error(404,"Không tìm thấy lịch."));
        courts.khoa(courtId).orElseThrow();
        var item=slots.khoaLich(List.of(id)).get(0);
        if (!ChinhSachDatSan.batDau(item).isAfter(LocalDateTime.now(clock))) throw error(409,"Không sửa suất đã bắt đầu.");
        if (!jdbc.queryForList("SELECT lich_san_id FROM giu_cho WHERE lich_san_id=? FOR UPDATE",Long.class,id).isEmpty()) {
            throw error(409,"Không sửa giá hoặc đóng suất đang có người đặt.");
        }
        item.setGia(input.gia()); item.setMoDat(input.moDat());
        audit.ghi("SUA_LICH","LICH:"+id,"Giá: "+input.gia()+" | Mở đặt: "+input.moDat());
    }
    private static ResponseStatusException error(int status,String text) {
        return new ResponseStatusException(HttpStatus.valueOf(status),text);
    }
}
