package com.dong.dat_san.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "dat_san")
public class ThucTheDatSan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

   @Column(name = "ma_dat", nullable = false, unique = true, length = 36, columnDefinition = "CHAR(36)")
    private String maDat;

    @ManyToOne
    @JoinColumn(name = "nguoi_dung_id", nullable = false)
    private NguoiDung nguoiDung;

    @Column(name = "trang_thai", nullable = false, length = 30)
    private String trangThai = "CHO_XAC_NHAN";

    @Column(name = "tong_tien", nullable = false, precision = 12, scale = 2)
    private BigDecimal tongTien;

    @Column(name = "ghi_chu", length = 1000)
    private String ghiChu;

    @Column(name = "ly_do_huy", length = 500)
    private String lyDoHuy;

    @Column(name = "ngay_tao", nullable = false, updatable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();

    public ThucTheDatSan() {}

    // Getters
    public Long getId() { return id; }
    public String getMaDat() { return maDat; }
    public NguoiDung getNguoiDung() { return nguoiDung; }
    public String getTrangThai() { return trangThai; }
    public BigDecimal getTongTien() { return tongTien; }
    public String getGhiChu() { return ghiChu; }
    public String getLyDoHuy() { return lyDoHuy; }
    public LocalDateTime getNgayTao() { return ngayTao; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setMaDat(String maDat) { this.maDat = maDat; }
    public void setNguoiDung(NguoiDung nguoiDung) { this.nguoiDung = nguoiDung; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
    public void setTongTien(BigDecimal tongTien) { this.tongTien = tongTien; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
    public void setLyDoHuy(String lyDoHuy) { this.lyDoHuy = lyDoHuy; }
    public void setNgayTao(LocalDateTime ngayTao) { this.ngayTao = ngayTao; }
}