package com.dong.dat_san.repository;

import com.dong.dat_san.entity.VaiTro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KhoDuLieuVaiTro extends JpaRepository<VaiTro, Long> {
    Optional<VaiTro> findByMa(String ma);
}