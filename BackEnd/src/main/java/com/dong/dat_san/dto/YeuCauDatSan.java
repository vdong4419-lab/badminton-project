package com.dong.dat_san.dto;

import java.util.List;

public class YeuCauDatSan {
    private Long nguoiDungId;
    private List<Long> danhSachLichSanIds;

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