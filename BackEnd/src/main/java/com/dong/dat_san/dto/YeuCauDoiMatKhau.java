package com.dong.dat_san.dto;

import jakarta.validation.constraints.*;

public record YeuCauDoiMatKhau(
        @NotBlank(message = "Cần nhập mật khẩu hiện tại") @Size(max = 72) String matKhauHienTai,
        @NotBlank(message = "Cần nhập mật khẩu mới")
        @Size(min = 8, max = 72, message = "Mật khẩu mới phải có 8–72 ký tự") String matKhauMoi) {
    @Override public String toString() { return "YeuCauDoiMatKhau[REDACTED]"; }
}
