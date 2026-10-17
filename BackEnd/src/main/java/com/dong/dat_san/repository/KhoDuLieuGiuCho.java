package com.dong.dat_san.repository;

import com.dong.dat_san.entity.ThucTheGiuCho;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface KhoDuLieuGiuCho extends JpaRepository<ThucTheGiuCho, Long> {
    boolean existsByLichSanIdIn(List<Long> ids);

    // INSERT bắt buộc: save/merge với ID gán sẵn có thể ghi đè người đang giữ suất.
    @Modifying
    @Query(value = """
            INSERT INTO giu_cho (lich_san_id, chi_tiet_dat_san_id, ngay_tao)
            VALUES (:lichId, :chiTietId, :now)
            """, nativeQuery = true)
    void chiemSuat(@Param("lichId") Long lichId, @Param("chiTietId") Long chiTietId,
                   @Param("now") LocalDateTime now);

    @Modifying
    @Query(value = """
            DELETE FROM giu_cho WHERE chi_tiet_dat_san_id IN
            (SELECT id FROM chi_tiet_dat_san WHERE dat_san_id = :id)
            """, nativeQuery = true)
    void giaiPhongDon(@Param("id") Long id);
}
