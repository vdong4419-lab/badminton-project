package com.dong.dat_san.service;

import com.dong.dat_san.dto.SanCongKhaiDto;
import com.dong.dat_san.entity.San;
import com.dong.dat_san.repository.SanRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly=true)
public class DichVuSanCongKhai {
    private final SanRepository courts;
    private final NamedParameterJdbcTemplate jdbc;
    public DichVuSanCongKhai(SanRepository courts,JdbcTemplate jdbc) {
        this.courts=courts;this.jdbc=new NamedParameterJdbcTemplate(jdbc);
    }
    public List<SanCongKhaiDto> danhSach(String name) {
        var results=name==null || name.isBlank() ? courts.findAll() : courts.findByTenSanContainingIgnoreCase(name.trim());
        return tongHop(results.stream().filter(San::isDangHoatDong).toList());
    }
    public SanCongKhaiDto chiTiet(Long id) {
        var court=courts.findById(id).filter(San::isDangHoatDong)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy sân đang hoạt động."));
        return tongHop(List.of(court)).get(0);
    }
    private record Diem(Long id,BigDecimal trungBinh,long soLuong) {}
    private List<SanCongKhaiDto> tongHop(List<San> courtList) {
        if (courtList.isEmpty()) return List.of();
        var params=Map.of("ids",courtList.stream().map(San::getId).toList());
        Map<Long,Diem> scores=new HashMap<>();
        jdbc.query("SELECT san_id,AVG(diem) AS trung_binh,COUNT(*) AS so_luong FROM danh_gia_san WHERE san_id IN (:ids) GROUP BY san_id",
                params,(r,i) -> new Diem(r.getLong("san_id"),r.getBigDecimal("trung_binh").setScale(1,RoundingMode.HALF_UP),r.getLong("so_luong")))
                .forEach(score -> scores.put(score.id(),score));
        Map<Long,SanCongKhaiDto.NhanXet> latest=new HashMap<>();
        jdbc.query("""
                SELECT san_id,ho_ten,diem,binh_luan,ngay_tao FROM (
                    SELECT g.san_id,n.ho_ten,g.diem,LEFT(g.binh_luan,240) AS binh_luan,g.ngay_tao,
                           ROW_NUMBER() OVER (PARTITION BY g.san_id ORDER BY g.ngay_tao DESC,g.id DESC) AS thu_tu
                    FROM danh_gia_san g JOIN dat_san d ON d.id=g.dat_san_id
                    JOIN nguoi_dung n ON n.id=d.nguoi_dung_id WHERE g.san_id IN (:ids)
                ) ranked WHERE thu_tu=1
                """,params,(org.springframework.jdbc.core.RowCallbackHandler) r -> latest.put(r.getLong("san_id"),
                        new SanCongKhaiDto.NhanXet(r.getString("ho_ten"),r.getInt("diem"),r.getString("binh_luan"),r.getTimestamp("ngay_tao").toLocalDateTime())));
        return courtList.stream().map(c -> {
            var score=scores.getOrDefault(c.getId(),new Diem(c.getId(),BigDecimal.ZERO,0));
            return new SanCongKhaiDto(c.getId(),c.getTenSan(),c.getGiaMoiGio(),c.getMoTa(),c.getAnhDaiDien(),
                    c.isDangHoatDong(),score.trungBinh(),score.soLuong(),latest.get(c.getId()));
        }).toList();
    }
}
