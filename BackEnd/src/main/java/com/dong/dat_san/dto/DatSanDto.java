package com.dong.dat_san.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DatSanDto(Long id, String maDat, String trangThai, BigDecimal tongTien,
                        String lyDoHuy, LocalDateTime ngayTao, LocalDateTime ngayHuy,
                        boolean coTheHuy, List<ChiTietDto> chiTietDatSans) {
    public record ChiTietDto(Long id, BigDecimal donGia, LichSanDto lichSan) {}
}
