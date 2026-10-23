package com.dong.dat_san.dto;

import java.time.LocalDateTime;
import java.util.List;

public record QuanTriDonDto(DatSanDto don, KhachDto khach, List<String> hanhDong, List<SuKienDto> lichSu) {
    public record KhachDto(Long id, String hoTen, String email, String soDienThoai) {}
    public record SuKienDto(Long id, String nguoiThucHien, String trangThaiCu, String trangThaiMoi,
                           String ghiChu, LocalDateTime ngayTao) {}
}
