package com.dong.dat_san.dto;
import jakarta.validation.constraints.*;
public record YeuCauDatLaiMatKhau(@NotBlank @Email @Size(max=254) String email,
        @NotBlank @Pattern(regexp="[0-9]{6}",message="OTP phải gồm 6 chữ số") String otp,
        @NotBlank @Size(min=8,max=72,message="Mật khẩu mới phải có 8–72 ký tự") String matKhauMoi) {
    @Override public String toString() { return "YeuCauDatLaiMatKhau[REDACTED]"; }
}
