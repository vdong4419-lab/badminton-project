package com.dong.dat_san.repository;

import com.dong.dat_san.entity.NguoiDung;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KhoDuLieuNguoiDung extends JpaRepository<NguoiDung, Long> {
    Optional<NguoiDung> findByEmail(String email);
    boolean existsBySoDienThoaiAndIdNot(String soDienThoai, Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from NguoiDung n where n.email=:email")
    Optional<NguoiDung> khoaTheoEmail(@Param("email") String email);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from NguoiDung n where n.id=:id")
    Optional<NguoiDung> khoa(@Param("id") Long id);
}
