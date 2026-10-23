package com.dong.dat_san.repository;

import com.dong.dat_san.entity.ThanhToan;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface KhoDuLieuThanhToan extends JpaRepository<ThanhToan, Long> {
    Optional<ThanhToan> findByDatSanId(Long datSanId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from ThanhToan t where t.datSanId = :id")
    Optional<ThanhToan> findLockedByDatSanId(@Param("id") Long id);
}
