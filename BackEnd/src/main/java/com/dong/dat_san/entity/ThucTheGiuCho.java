package com.dong.dat_san.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "giu_cho")
public class ThucTheGiuCho {

    @Id
    @Column(name = "lich_san_id")
    private Long lichSanId;

    @Column(name = "chi_tiet_dat_san_id", nullable = false, unique = true)
    private Long chiTietDatSanId;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();

    public ThucTheGiuCho() {}

    public ThucTheGiuCho(Long lichSanId, Long chiTietDatSanId) {
        this.lichSanId = lichSanId;
        this.chiTietDatSanId = chiTietDatSanId;
    }

    public Long getLichSanId() {
        return lichSanId;
    }

    public void setLichSanId(Long lichSanId) {
        this.lichSanId = lichSanId;
    }

    public Long getChiTietDatSanId() {
        return chiTietDatSanId;
    }

    public void setChiTietDatSanId(Long chiTietDatSanId) {
        this.chiTietDatSanId = chiTietDatSanId;
    }
}