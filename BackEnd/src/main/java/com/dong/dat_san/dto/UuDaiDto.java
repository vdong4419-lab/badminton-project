package com.dong.dat_san.dto;

import java.math.BigDecimal;

public record UuDaiDto(BigDecimal tamTinh, int tyLeGiam, BigDecimal soTienGiam,
                       BigDecimal tongTien, String hangThanhVien) {}
