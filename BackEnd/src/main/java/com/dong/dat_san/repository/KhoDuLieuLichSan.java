package com.dong.dat_san.repository;

import com.dong.dat_san.entity.ThucTheLichSan;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface KhoDuLieuLichSan extends JpaRepository<ThucTheLichSan, Long> {
    @Query("""
            select l from ThucTheLichSan l join fetch l.san s join fetch l.khungGio k
            where s.id = :sanId and l.ngayChoi = :ngay and l.moDat = true and s.dangHoatDong = true
              and (l.ngayChoi > :today or (l.ngayChoi = :today and k.gioBatDau > :time))
              and not exists (select g.lichSanId from ThucTheGiuCho g where g.lichSanId = l.id)
            order by k.gioBatDau
            """)
    List<ThucTheLichSan> findLichTrong(@Param("sanId") Long sanId, @Param("ngay") LocalDate ngay,
                                      @Param("today") LocalDate today, @Param("time") LocalTime time);

    // Cùng thứ tự khóa cho mọi yêu cầu đặt nhiều suất để giảm deadlock.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from ThucTheLichSan l where l.id in :ids order by l.id")
    List<ThucTheLichSan> khoaLich(@Param("ids") List<Long> ids);
}
