package com.dong.dat_san.repository;

import com.dong.dat_san.entity.ThucTheDatSan;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface KhoDuLieuDatSan extends JpaRepository<ThucTheDatSan, Long> {
    List<ThucTheDatSan> findByNguoiDungIdOrderByNgayTaoDescIdDesc(Long nguoiDungId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from ThucTheDatSan d where d.id = :id")
    Optional<ThucTheDatSan> findLockedById(@Param("id") Long id);

    @Modifying
    @Query(value = """
            INSERT INTO lich_su_dat_san
            (dat_san_id, nguoi_thuc_hien_id, trang_thai_cu, trang_thai_moi, ghi_chu, ngay_tao)
            VALUES (:id, :userId, :oldStatus, :newStatus, :note, :now)
            """, nativeQuery = true)
    void ghiLichSu(@Param("id") Long id, @Param("userId") Long userId,
                   @Param("oldStatus") String oldStatus, @Param("newStatus") String newStatus,
                   @Param("note") String note, @Param("now") LocalDateTime now);

    @Query(value = "SELECT COUNT(*) FROM thanh_toan WHERE dat_san_id = :id AND trang_thai = 'DA_THANH_TOAN'", nativeQuery = true)
    long countDaThanhToan(@Param("id") Long id);
}
