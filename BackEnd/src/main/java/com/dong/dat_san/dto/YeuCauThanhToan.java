package com.dong.dat_san.dto;

import jakarta.validation.constraints.*;

public record YeuCauThanhToan(
        @NotBlank @Pattern(regexp = "TIEN_MAT|CHUYEN_KHOAN", message = "Phương thức phải là tiền mặt hoặc chuyển khoản")
        String phuongThuc,
        @Size(max = 100, message = "Mã tham chiếu tối đa 100 ký tự") String maThamChieu) {}
