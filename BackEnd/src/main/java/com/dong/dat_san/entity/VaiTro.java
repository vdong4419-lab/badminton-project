package com.dong.dat_san.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "vai_tro")
public class VaiTro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String ma;

    @Column(nullable = false, length = 100)
    private String ten;

    public VaiTro() {}

    public Long getId() { return id; }
    public String getMa() { return ma; }
    public String getTen() { return ten; }
}