package com.dong.dat_san.repository;

import java.util.List;

import com.dong.dat_san.entity.San;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SanRepository extends JpaRepository<San, Long> {

    List<San> findByTenSanContainingIgnoreCase(String ten);
}