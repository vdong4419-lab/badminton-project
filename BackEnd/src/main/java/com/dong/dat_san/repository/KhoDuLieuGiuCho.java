package com.dong.dat_san.repository;

import com.dong.dat_san.entity.ThucTheGiuCho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KhoDuLieuGiuCho extends JpaRepository<ThucTheGiuCho, Long> {
    void deleteByChiTietDatSanId(Long chiTietDatSanId);
}