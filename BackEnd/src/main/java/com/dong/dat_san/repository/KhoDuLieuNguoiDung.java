package com.dong.dat_san.repository;

import com.dong.dat_san.entity.NguoiDung;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KhoDuLieuNguoiDung extends JpaRepository<NguoiDung, Long> {
    Optional<NguoiDung> findByEmail(String email);
}