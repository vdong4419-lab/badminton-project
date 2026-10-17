package com.dong.dat_san.repository;

import com.dong.dat_san.entity.ThucTheLichSan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface KhoDuLieuLichSan extends JpaRepository<ThucTheLichSan, Long> {
    
    @Query(value = "SELECT * FROM lich_san WHERE san_id = :sanId AND ngay = :ngay", nativeQuery = true)
    List<ThucTheLichSan> findLichTrongBySanAndNgay(@Param("sanId") Long sanId, @Param("ngay") LocalDate ngay);
}