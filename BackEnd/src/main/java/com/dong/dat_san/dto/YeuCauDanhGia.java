package com.dong.dat_san.dto;
import jakarta.validation.constraints.*;
public record YeuCauDanhGia(@NotNull @Positive Long sanId,
        @NotNull @Min(1) @Max(5) Integer diem,
        @NotBlank @Size(min=10,max=2000,message="Bình luận cần từ 10 đến 2000 ký tự") String binhLuan) {}
