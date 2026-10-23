package com.dong.dat_san.repository;

import com.dong.dat_san.entity.ThucTheDatSan;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface KhoDuLieuDatSan extends JpaRepository<ThucTheDatSan, Long> {
    List<ThucTheDatSan> findByNguoiDungIdOrderByNgayTaoDescIdDesc(Long nguoiDungId);

    @Query("""
            select d from ThucTheDatSan d
            where (:status is null or d.trangThai = :status)
              and (:day is null or exists (select c.id from ChiTietDatSan c where c.datSan.id=d.id and c.lichSan.ngayChoi=:day))
              and (:search = '' or lower(d.maDat) like lower(concat('%',:search,'%'))
                   or lower(d.nguoiDung.hoTen) like lower(concat('%',:search,'%'))
                   or d.nguoiDung.soDienThoai like concat('%',:search,'%'))
              and (:refund = false or (d.trangThai = 'DA_HUY' and exists
                   (select t.id from ThanhToan t where t.datSanId = d.id and t.trangThai = 'DA_THANH_TOAN')))
            """)
    Page<ThucTheDatSan> timQuanTri(@Param("status") String status, @Param("refund") boolean refund,
            @Param("day") java.time.LocalDate day, @Param("search") String search, Pageable pageable);

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
