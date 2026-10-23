package com.dong.dat_san.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "thanh_toan")
public class ThanhToan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "dat_san_id", nullable = false, unique = true)
    private Long datSanId;
    @Column(name = "so_tien", nullable = false, precision = 12, scale = 2)
    private BigDecimal soTien;
    @Column(name = "phuong_thuc", nullable = false, length = 30)
    private String phuongThuc;
    @Column(name = "trang_thai", nullable = false, length = 30)
    private String trangThai;
    @Column(name = "ma_tham_chieu", length = 100)
    private String maThamChieu;
    @Column(name = "nguoi_xac_nhan_id")
    private Long nguoiXacNhanId;
    @Column(name = "thoi_diem_thanh_toan")
    private LocalDateTime thoiDiemThanhToan;
    @Column(name = "thoi_diem_hoan_tien")
    private LocalDateTime thoiDiemHoanTien;
    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao;

    public Long getId() { return id; }
    public Long getDatSanId() { return datSanId; }
    public BigDecimal getSoTien() { return soTien; }
    public String getPhuongThuc() { return phuongThuc; }
    public String getTrangThai() { return trangThai; }
    public String getMaThamChieu() { return maThamChieu; }
    public LocalDateTime getThoiDiemThanhToan() { return thoiDiemThanhToan; }
    public LocalDateTime getThoiDiemHoanTien() { return thoiDiemHoanTien; }
    public void setDatSanId(Long value) { datSanId = value; }
    public void setSoTien(BigDecimal value) { soTien = value; }
    public void setPhuongThuc(String value) { phuongThuc = value; }
    public void setTrangThai(String value) { trangThai = value; }
    public void setMaThamChieu(String value) { maThamChieu = value; }
    public void setNguoiXacNhanId(Long value) { nguoiXacNhanId = value; }
    public void setThoiDiemThanhToan(LocalDateTime value) { thoiDiemThanhToan = value; }
    public void setThoiDiemHoanTien(LocalDateTime value) { thoiDiemHoanTien = value; }
    public void setNgayTao(LocalDateTime value) { ngayTao = value; }
}
