package com.dong.dat_san.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record YeuCauSuaSan(
        @NotBlank @Size(max=100) String tenSan,
        @NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal giaMoiGio,
        @Size(max=1000) String moTa,
        @Size(max=500) @Pattern(regexp="^$|^https?://[^\\s]+$",message="Ảnh cần URL http hoặc https hợp lệ") String anhDaiDien,
        @NotNull Boolean dangHoatDong) {}
