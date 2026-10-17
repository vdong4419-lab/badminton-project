package com.dong.dat_san.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "lich_san")
public class ThucTheLichSan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "san_id", nullable = false)
    private San san;

    @ManyToOne
    @JoinColumn(name = "khung_gio_id", nullable = false)
    private ThucTheKhungGio khungGio;

    @Column(name = "ngay_choi", nullable = false)
    private LocalDate ngayChoi;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal gia;

    @Column(name = "mo_dat", nullable = false)
    private boolean moDat = true;

    public ThucTheLichSan() {}

    public Long getId() {
        return id;
    }

    public San getSan() {
        return san;
    }

    public ThucTheKhungGio getKhungGio() {
        return khungGio;
    }

    public LocalDate getNgayChoi() {
        return ngayChoi;
    }

    public BigDecimal getGia() {
        return gia;
    }

    public boolean isMoDat() {
        return moDat;
    }

    public void setSan(San san) {
        this.san = san;
    }

    public void setKhungGio(ThucTheKhungGio khungGio) {
        this.khungGio = khungGio;
    }

    public void setNgayChoi(LocalDate ngayChoi) {
        this.ngayChoi = ngayChoi;
    }

    public void setGia(BigDecimal gia) {
        this.gia = gia;
    }

    public void setMoDat(boolean moDat) {
        this.moDat = moDat;
    }
}