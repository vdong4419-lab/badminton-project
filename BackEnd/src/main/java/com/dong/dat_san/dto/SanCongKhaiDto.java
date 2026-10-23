package com.dong.dat_san.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SanCongKhaiDto(Long id,String tenSan,BigDecimal giaMoiGio,String moTa,String anhDaiDien,
        boolean dangHoatDong,BigDecimal diemTrungBinh,long tongDanhGia,NhanXet danhGiaMoiNhat) {
    public record NhanXet(String khachHang,int diem,String binhLuan,LocalDateTime ngayTao) {}
}
