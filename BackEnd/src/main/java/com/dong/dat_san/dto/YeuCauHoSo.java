package com.dong.dat_san.dto;

import jakarta.validation.constraints.*;

public record YeuCauHoSo(
        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 100, message = "Họ tên tối đa 100 ký tự") String hoTen,
        @Pattern(regexp = "^$|^\\+?[0-9]{9,15}$", message = "Số điện thoại phải gồm 9–15 chữ số, có thể bắt đầu bằng +") String soDienThoai) {}
