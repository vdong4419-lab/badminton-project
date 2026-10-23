package com.dong.dat_san.dto;
import jakarta.validation.constraints.*;
import java.util.List;
public record YeuCauDatTaiQuay(@NotNull @Positive Long nguoiDungId,
        @NotEmpty @Size(max=48) List<@NotNull @Positive Long> danhSachLichSanIds) {}
