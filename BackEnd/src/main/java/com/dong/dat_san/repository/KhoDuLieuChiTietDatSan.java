package com.dong.dat_san.repository;

import com.dong.dat_san.entity.ChiTietDatSan;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KhoDuLieuChiTietDatSan extends JpaRepository<ChiTietDatSan, Long> {
    List<ChiTietDatSan> findByDatSanIdOrderByIdAsc(Long datSanId);
}
