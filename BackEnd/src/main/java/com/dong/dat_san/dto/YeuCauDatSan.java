package com.dong.dat_san.dto;

import java.util.List;
import jakarta.validation.constraints.*;

public class YeuCauDatSan {
    private Long nguoiDungId;
    @NotEmpty(message = "Vui lòng chọn ít nhất một suất lịch")
    @Size(max = 48, message = "Mỗi đơn tối đa 48 suất lịch")
    private List<@NotNull @Positive Long> danhSachLichSanIds;

    public YeuCauDatSan() {}

    public Long getNguoiDungId() {
        return nguoiDungId;
    }

    public void setNguoiDungId(Long nguoiDungId) {
        this.nguoiDungId = nguoiDungId;
    }

    public List<Long> getDanhSachLichSanIds() {
        return danhSachLichSanIds;
    }

    public void setDanhSachLichSanIds(List<Long> danhSachLichSanIds) {
        this.danhSachLichSanIds = danhSachLichSanIds;
    }
}
