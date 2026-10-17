package com.dong.dat_san.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "san")
public class San {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ten_san", nullable = false)
    private String tenSan;

    @Column(name = "gia_moi_gio", nullable = false,
            precision = 12, scale = 2)
    private BigDecimal giaMoiGio;

    @Column(name = "mo_ta", length = 1000)
    private String moTa;

    @Column(name = "anh_dai_dien", length = 500)
    private String anhDaiDien;

    @Column(name = "dang_hoat_dong", nullable = false)
    private boolean dangHoatDong = true;

    public San() {
    }

    public Long getId() {
        return id;
    }

    public String getTenSan() {
        return tenSan;
    }

    public BigDecimal getGiaMoiGio() {
        return giaMoiGio;
    }

    public boolean isDangHoatDong() {
        return dangHoatDong;
    }
    public String getMoTa() { return moTa; }
    public String getAnhDaiDien() { return anhDaiDien; }
    public void setTenSan(String tenSan) {
    this.tenSan = tenSan;
}

public void setGiaMoiGio(BigDecimal giaMoiGio) {
    this.giaMoiGio = giaMoiGio;
}
}
