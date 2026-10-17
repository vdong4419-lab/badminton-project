package com.dong.dat_san.dto;

import jakarta.validation.constraints.Size;

public record YeuCauHuyDatSan(@Size(max = 500, message = "Lý do hủy tối đa 500 ký tự") String lyDoHuy) {}
