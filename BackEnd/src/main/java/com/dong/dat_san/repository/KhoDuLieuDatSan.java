package com.dong.dat_san.repository;

import com.dong.dat_san.entity.ThucTheDatSan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KhoDuLieuDatSan extends JpaRepository<ThucTheDatSan, Long> {
    // Tìm danh sách đơn đặt sân theo ID người dùng
    List<ThucTheDatSan> findByNguoiDungId(Long nguoiDungId);
}