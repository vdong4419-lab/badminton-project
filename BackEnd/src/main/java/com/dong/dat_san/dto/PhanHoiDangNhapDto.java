package com.dong.dat_san.dto;

public class PhanHoiDangNhapDto {
    private String token;
    private String email;
    private String hoTen;
    private String vaiTro;

    public PhanHoiDangNhapDto(String token, String email, String hoTen, String vaiTro) {
        this.token = token;
        this.email = email;
        this.hoTen = hoTen;
        this.vaiTro = vaiTro;
    }

    // Getters
    public String getToken() { return token; }
    public String getEmail() { return email; }
    public String getHoTen() { return hoTen; }
    public String getVaiTro() { return vaiTro; }
}