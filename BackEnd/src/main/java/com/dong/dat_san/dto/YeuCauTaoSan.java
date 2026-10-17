package com.dong.dat_san.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record YeuCauTaoSan(

        @NotBlank(message = "Tên sân không được để trống")
        @Size(max = 100, message = "Tên sân tối đa 100 ký tự")
        String tenSan,

        @NotNull(message = "Giá thuê không được để trống")
        @DecimalMin(value = "0.01", message = "Giá thuê phải lớn hơn 0")
        @Digits(integer = 10, fraction = 2,
                message = "Giá thuê tối đa 10 chữ số nguyên và 2 số thập phân")
        BigDecimal giaMoiGio

) {
}