package com.dong.dat_san.dto;

import com.dong.dat_san.entity.ThucTheLichSan;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record LichSanDto(Long id, LocalDate ngayChoi, BigDecimal gia, boolean moDat,
                         SanDto san, KhungGioDto khungGio) {
    public record SanDto(Long id, String tenSan) {}
    public record KhungGioDto(LocalTime gioBatDau, LocalTime gioKetThuc) {}
    public static LichSanDto from(ThucTheLichSan lich) {
        return new LichSanDto(lich.getId(), lich.getNgayChoi(), lich.getGia(), lich.isMoDat(),
                new SanDto(lich.getSan().getId(), lich.getSan().getTenSan()),
                new KhungGioDto(lich.getKhungGio().getGioBatDau(), lich.getKhungGio().getGioKetThuc()));
    }
}
