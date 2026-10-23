package com.dong.dat_san.dto;

import jakarta.validation.constraints.*;

public record YeuCauHoanTien(
        @NotBlank(message = "Cần mã giao dịch hoặc biên nhận hoàn tiền")
        @Size(max = 100) String maBienNhan,
        @NotBlank(message = "Cần ghi chú hoàn tiền")
        @Size(max = 500) String ghiChu) {}
