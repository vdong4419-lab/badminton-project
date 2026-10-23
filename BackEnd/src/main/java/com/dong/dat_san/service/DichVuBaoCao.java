package com.dong.dat_san.service;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DichVuBaoCao {
    private final JdbcTemplate jdbc;
    private final Clock clock;
    public DichVuBaoCao(JdbcTemplate jdbc,Clock clock) { this.jdbc=jdbc; this.clock=clock; }
    public record NgayTien(LocalDate ngay,BigDecimal thu,BigDecimal hoan,BigDecimal rong) {}
    public record BaoCao(LocalDate tuNgay,LocalDate denNgay,BigDecimal tongThu,BigDecimal tongHoan,
                         BigDecimal thuRong,BigDecimal choHoanHienTai,List<NgayTien> theoNgay) {}

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly=true)
    public BaoCao tien(LocalDate from,LocalDate to) {
        if (from==null || to==null || to.isBefore(from) || java.time.temporal.ChronoUnit.DAYS.between(from,to)>365) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Khoảng báo cáo phải hợp lệ, tối đa 366 ngày.");
        }
        Map<LocalDate,BigDecimal> received=new HashMap<>(), refunded=new HashMap<>();
        jdbc.query("""
                SELECT DATE(thoi_diem_thanh_toan),SUM(so_tien) FROM thanh_toan
                WHERE trang_thai IN ('DA_THANH_TOAN','DA_HOAN_TIEN')
                AND thoi_diem_thanh_toan>=? AND thoi_diem_thanh_toan<? GROUP BY DATE(thoi_diem_thanh_toan)
                """,r -> { received.put(r.getDate(1).toLocalDate(),r.getBigDecimal(2)); },from.atStartOfDay(),to.plusDays(1).atStartOfDay());
        jdbc.query("""
                SELECT DATE(thoi_diem_hoan_tien),SUM(so_tien) FROM thanh_toan
                WHERE trang_thai='DA_HOAN_TIEN' AND thoi_diem_hoan_tien>=? AND thoi_diem_hoan_tien<?
                GROUP BY DATE(thoi_diem_hoan_tien)
                """,r -> { refunded.put(r.getDate(1).toLocalDate(),r.getBigDecimal(2)); },from.atStartOfDay(),to.plusDays(1).atStartOfDay());
        var days=from.datesUntil(to.plusDays(1)).map(day -> {
            var thu=received.getOrDefault(day,BigDecimal.ZERO); var hoan=refunded.getOrDefault(day,BigDecimal.ZERO);
            return new NgayTien(day,thu,hoan,thu.subtract(hoan));
        }).toList();
        var thu=days.stream().map(NgayTien::thu).reduce(BigDecimal.ZERO,BigDecimal::add);
        var hoan=days.stream().map(NgayTien::hoan).reduce(BigDecimal.ZERO,BigDecimal::add);
        var pending=jdbc.queryForObject("""
                SELECT COALESCE(SUM(t.so_tien),0) FROM thanh_toan t JOIN dat_san d ON d.id=t.dat_san_id
                WHERE d.trang_thai='DA_HUY' AND t.trang_thai='DA_THANH_TOAN'
                """,BigDecimal.class);
        return new BaoCao(from,to,thu,hoan,thu.subtract(hoan),pending,days);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly=true)
    public Map<String,Object> tongQuan(LocalDate day) {
        if (day==null) day=LocalDate.now(clock);
        var orders=jdbc.queryForMap("""
                SELECT COUNT(*) AS tongDon,COALESCE(SUM(d.trang_thai='CHO_XAC_NHAN'),0) AS choXacNhan,
                       COALESCE(SUM(d.trang_thai='DA_HUY'),0) AS daHuy
                FROM dat_san d WHERE EXISTS (SELECT 1 FROM chi_tiet_dat_san c JOIN lich_san l ON l.id=c.lich_san_id
                WHERE c.dat_san_id=d.id AND l.ngay_choi=?)
                """,day);
        var occupancy=jdbc.queryForMap("""
                SELECT COUNT(*) AS tongSuat,COALESCE(SUM(g.lich_san_id IS NOT NULL),0) AS daDat
                FROM lich_san l JOIN san s ON s.id=l.san_id LEFT JOIN giu_cho g ON g.lich_san_id=l.id
                WHERE l.ngay_choi=? AND s.dang_hoat_dong=TRUE AND (l.mo_dat=TRUE OR g.lich_san_id IS NOT NULL)
                """,day);
        return Map.of("ngay",day,"don",orders,"suat",occupancy,"tien",tien(day,day),
                "sanHoatDong",jdbc.queryForObject("SELECT COUNT(*) FROM san WHERE dang_hoat_dong=TRUE",Long.class));
    }

    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Transactional(readOnly=true)
    public Map<String,Object> thuCa() {
        String email=SecurityContextHolder.getContext().getAuthentication().getName();
        var today=LocalDate.now(clock);
        var rows=jdbc.queryForList("""
                SELECT t.id,t.dat_san_id AS donId,t.so_tien AS soTien,t.phuong_thuc AS phuongThuc,
                       t.ma_tham_chieu AS maThamChieu,t.thoi_diem_thanh_toan AS thoiDiemThanhToan
                FROM thanh_toan t JOIN nguoi_dung n ON n.id=t.nguoi_xac_nhan_id
                WHERE n.email=? AND t.trang_thai IN ('DA_THANH_TOAN','DA_HOAN_TIEN')
                AND t.thoi_diem_thanh_toan>=? AND t.thoi_diem_thanh_toan<? ORDER BY t.thoi_diem_thanh_toan DESC
                """,email,today.atStartOfDay(),today.plusDays(1).atStartOfDay());
        var total=rows.stream().map(row -> (BigDecimal)row.get("soTien")).reduce(BigDecimal.ZERO,BigDecimal::add);
        return Map.of("ngay",today,"tongThu",total,"giaoDich",rows);
    }
}
