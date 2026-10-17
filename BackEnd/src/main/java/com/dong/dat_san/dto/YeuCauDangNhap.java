package com.dong.dat_san.dto;

import jakarta.validation.constraints.*;

public class YeuCauDangNhap {
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 254)
    private String email;
    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(max = 72)
    private String matKhau;

    // Getters và Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMatKhau() {
        return matKhau;
    }

    public void setMatKhau(String matKhau) {
        this.matKhau = matKhau;
    }
}
