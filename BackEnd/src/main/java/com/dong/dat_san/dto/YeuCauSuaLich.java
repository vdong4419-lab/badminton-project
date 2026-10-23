package com.dong.dat_san.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record YeuCauSuaLich(
        @NotNull @DecimalMin("0") @Digits(integer=10,fraction=2) BigDecimal gia,
        @NotNull Boolean moDat) {}
