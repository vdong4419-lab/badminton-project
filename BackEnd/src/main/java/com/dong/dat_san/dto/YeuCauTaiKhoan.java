package com.dong.dat_san.dto;

import jakarta.validation.constraints.*;
public record YeuCauTaiKhoan(
        @NotBlank @Size(max=100) String hoTen,
        @NotBlank @Email @Size(max=254) String email,
        @Pattern(regexp="^$|^\\+?[0-9]{9,15}$") String soDienThoai,
        @NotBlank @Pattern(regexp="ADMIN|STAFF|CUSTOMER") String vaiTro,
        @NotNull Boolean dangHoatDong,
        @Size(min=8,max=72) String matKhau) {
    @Override public String toString() { return "YeuCauTaiKhoan[thongTinDaAn]"; }
}
