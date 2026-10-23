package com.dong.dat_san.repository;

import java.util.List;

import com.dong.dat_san.entity.San;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface SanRepository extends JpaRepository<San, Long> {

    List<San> findByTenSanContainingIgnoreCase(String ten);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from San s where s.id=:id")
    Optional<San> khoa(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from San s where s.id in (select l.san.id from ThucTheLichSan l where l.id in :ids) order by s.id")
    List<San> khoaTheoLich(@Param("ids") List<Long> ids);
}
