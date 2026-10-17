package com.dong.dat_san.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "chi_tiet_dat_san")
public class ChiTietDatSan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "dat_san_id")
    private ThucTheDatSan datSan;
    @ManyToOne(optional = false)
    @JoinColumn(name = "lich_san_id")
    private ThucTheLichSan lichSan;
    @Column(name = "don_gia", nullable = false, precision = 12, scale = 2)
    private BigDecimal donGia;

    protected ChiTietDatSan() {}
    public ChiTietDatSan(ThucTheDatSan datSan, ThucTheLichSan lichSan) {
        this.datSan = datSan;
        this.lichSan = lichSan;
        this.donGia = lichSan.getGia();
    }
    public Long getId() { return id; }
    public ThucTheLichSan getLichSan() { return lichSan; }
    public BigDecimal getDonGia() { return donGia; }
}
