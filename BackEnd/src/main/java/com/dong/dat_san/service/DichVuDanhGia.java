package com.dong.dat_san.service;

import com.dong.dat_san.dto.YeuCauDanhGia;
import com.dong.dat_san.entity.ThucTheDatSan;
import com.dong.dat_san.repository.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DichVuDanhGia {
    private final JdbcTemplate jdbc;
    private final KhoDuLieuDatSan orders;
    private final KhoDuLieuChiTietDatSan details;
    private final KhoDuLieuThanhToan payments;
    private final SanRepository courts;
    private final ChinhSachDatSan policy;
    public DichVuDanhGia(JdbcTemplate jdbc,KhoDuLieuDatSan orders,KhoDuLieuChiTietDatSan details,
            KhoDuLieuThanhToan payments,SanRepository courts,ChinhSachDatSan policy) {
        this.jdbc=jdbc;this.orders=orders;this.details=details;this.payments=payments;this.courts=courts;this.policy=policy;
    }
    public record DanhGia(Long id,int diem,String binhLuan,String khachHang,LocalDateTime ngayTao) {}
    public record SanDanhGia(Long sanId,String tenSan,boolean coTheDanhGia,DanhGia daDanhGia) {}
    private ThucTheDatSan don(String email,Long id,boolean lock) {
        var booking=(lock ? orders.findLockedById(id) : orders.findById(id)).orElseThrow(() -> error(HttpStatus.NOT_FOUND,"Không tìm thấy đơn."));
        if (!booking.getNguoiDung().getEmail().equals(email)) throw error(HttpStatus.FORBIDDEN,"Chỉ đánh giá đơn của chính bạn.");
        return booking;
    }
    private boolean hopLe(ThucTheDatSan booking) {
        var items=details.findByDatSanIdOrderByIdAsc(booking.getId());
        return "DA_HOAN_THANH".equals(booking.getTrangThai()) && !items.isEmpty()
                && ChinhSachDatSan.daThu(payments.findByDatSanId(booking.getId()).orElse(null))
                && items.stream().allMatch(d -> !d.getLichSan().getNgayChoi().atTime(d.getLichSan().getKhungGio().getGioKetThuc()).isAfter(policy.now()));
    }
    @Transactional(readOnly=true)
    public List<SanDanhGia> cuaDon(String email,Long id) {
        var booking=don(email,id,false);boolean allowed=hopLe(booking);
        var courtNames=new LinkedHashMap<Long,String>();
        details.findByDatSanIdOrderByIdAsc(id).forEach(d -> courtNames.put(d.getLichSan().getSan().getId(),d.getLichSan().getSan().getTenSan()));
        return courtNames.entrySet().stream().map(c -> {
            var existing=jdbc.query("SELECT id,diem,binh_luan,ngay_tao FROM danh_gia_san WHERE dat_san_id=? AND san_id=?",
                    (r,i) -> new DanhGia(r.getLong("id"),r.getInt("diem"),r.getString("binh_luan"),booking.getNguoiDung().getHoTen(),r.getTimestamp("ngay_tao").toLocalDateTime()),id,c.getKey()).stream().findFirst().orElse(null);
            return new SanDanhGia(c.getKey(),c.getValue(),allowed && existing==null,existing);
        }).toList();
    }
    @Transactional
    public List<SanDanhGia> gui(String email,Long id,YeuCauDanhGia request) {
        var booking=don(email,id,true);
        if (!hopLe(booking)) throw error(HttpStatus.CONFLICT,"Chỉ đánh giá sau khi đã chơi xong, thanh toán và đơn được hoàn thành.");
        if (details.findByDatSanIdOrderByIdAsc(id).stream().noneMatch(d -> d.getLichSan().getSan().getId().equals(request.sanId())))
            throw error(HttpStatus.BAD_REQUEST,"Sân không thuộc đơn này.");
        var previous=jdbc.queryForList("SELECT id FROM danh_gia_san WHERE dat_san_id=? AND san_id=? FOR UPDATE",id,request.sanId());
        if (!previous.isEmpty()) throw error(HttpStatus.CONFLICT,"Bạn đã đánh giá sân này trong đơn.");
        String comment=request.binhLuan().trim();
        if (comment.length()<10) throw error(HttpStatus.BAD_REQUEST,"Bình luận cần ít nhất 10 ký tự sau khi bỏ khoảng trắng.");
        jdbc.update("INSERT INTO danh_gia_san(dat_san_id,san_id,diem,binh_luan,ngay_tao) VALUES (?,?,?,?,?)",id,request.sanId(),request.diem(),comment,policy.now());
        return cuaDon(email,id);
    }
    @Transactional(readOnly=true)
    public Map<String,Object> congKhai(Long sanId,int page) {
        if (page<0 || page>10000) throw error(HttpStatus.BAD_REQUEST,"Trang không hợp lệ.");
        if (!courts.existsById(sanId)) throw error(HttpStatus.NOT_FOUND,"Không tìm thấy sân.");
        var items=jdbc.query("""
                SELECT g.id,g.diem,g.binh_luan,g.ngay_tao,n.ho_ten FROM danh_gia_san g
                JOIN dat_san d ON d.id=g.dat_san_id JOIN nguoi_dung n ON n.id=d.nguoi_dung_id
                WHERE g.san_id=? ORDER BY g.ngay_tao DESC,g.id DESC LIMIT 10 OFFSET ?
                """,(r,i) -> new DanhGia(r.getLong("id"),r.getInt("diem"),r.getString("binh_luan"),r.getString("ho_ten"),r.getTimestamp("ngay_tao").toLocalDateTime()),sanId,page*10);
        long total=jdbc.queryForObject("SELECT COUNT(*) FROM danh_gia_san WHERE san_id=?",Long.class,sanId);
        var average=jdbc.queryForObject("SELECT COALESCE(AVG(diem),0) FROM danh_gia_san WHERE san_id=?",java.math.BigDecimal.class,sanId);
        return Map.of("items",items,"tongDanhGia",total,"diemTrungBinh",average,"page",page,"totalPages",(total+9)/10);
    }
    private static ResponseStatusException error(HttpStatus status,String message) { return new ResponseStatusException(status,message); }
}
