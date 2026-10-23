package com.dong.dat_san.service;

import com.dong.dat_san.repository.KhoDuLieuNguoiDung;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DichVuThongBao {
    // Booking history is the durable event source, written in the same transaction as each change.
    private static final String EVENTS="""
            (h.trang_thai_cu IS NULL OR h.trang_thai_cu<>h.trang_thai_moi
             OR (h.trang_thai_cu=h.trang_thai_moi AND (h.ghi_chu LIKE 'THU_TIEN |%' OR h.ghi_chu LIKE 'HOAN_TIEN |%')))
            """;
    private final JdbcTemplate jdbc;
    private final KhoDuLieuNguoiDung users;
    private final Clock clock;
    public DichVuThongBao(JdbcTemplate jdbc,KhoDuLieuNguoiDung users,Clock clock) { this.jdbc=jdbc;this.users=users;this.clock=clock; }
    public record ThongBao(Long id,Long donId,String tieuDe,String noiDung,LocalDateTime ngayTao,boolean daDoc) {}
    @Transactional(readOnly=true)
    public Map<String,Object> danhSach(String email,int page) {
        if (page<0 || page>10000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Trang không hợp lệ.");
        Long userId=users.findByEmail(email).orElseThrow().getId();
        String from=" FROM lich_su_dat_san h JOIN dat_san d ON d.id=h.dat_san_id LEFT JOIN thong_bao_da_doc r ON r.lich_su_id=h.id WHERE d.nguoi_dung_id=? AND "+EVENTS;
        var items=jdbc.query("SELECT h.id,h.dat_san_id,h.trang_thai_cu,h.trang_thai_moi,h.ghi_chu,h.ngay_tao,r.lich_su_id AS read_id"+from+" ORDER BY h.ngay_tao DESC,h.id DESC LIMIT 20 OFFSET ?",(row,index) -> {
            String type=row.getString("trang_thai_moi"),old=row.getString("trang_thai_cu"),note=Objects.toString(row.getString("ghi_chu"),"");
            String title;
            if (Objects.equals(type,old) && note.startsWith("THU_TIEN |")) title="Sân đã xác nhận nhận tiền";
            else if (Objects.equals(type,old) && note.startsWith("HOAN_TIEN |")) title="Sân đã xác nhận hoàn tiền";
            else title=switch(type) {
                case "CHO_XAC_NHAN" -> "Đơn đang chờ sân xác nhận";
                case "DA_XAC_NHAN" -> "Sân đã xác nhận lịch đặt";
                case "DA_HOAN_THANH" -> "Đã hoàn thành buổi chơi";
                case "DA_HUY" -> note.startsWith("Hệ thống tự hủy:") ? "Đơn đã hết hạn giữ suất" : "Đơn đặt sân đã hủy";
                default -> "Cập nhật đơn đặt sân";
            };
            String detail="Đơn #"+row.getLong("dat_san_id")+". "+("DA_HOAN_THANH".equals(type) ? "Bạn có thể đánh giá sân và chia sẻ trải nghiệm." : "Mở đơn để xem thông tin mới nhất.");
            return new ThongBao(row.getLong("id"),row.getLong("dat_san_id"),title,detail,row.getTimestamp("ngay_tao").toLocalDateTime(),row.getObject("read_id")!=null);
        },userId,page*20);
        long total=jdbc.queryForObject("SELECT COUNT(*)"+from,Long.class,userId);
        long unread=jdbc.queryForObject("SELECT COUNT(*)"+from+" AND r.lich_su_id IS NULL",Long.class,userId);
        return Map.of("items",items,"totalUnread",unread,"page",page,"totalPages",(total+19)/20,"totalElements",total);
    }
    @Transactional
    public void doc(String email,Long id) {
        long userId=users.findByEmail(email).orElseThrow().getId();
        long owned=jdbc.queryForObject("SELECT COUNT(*) FROM lich_su_dat_san h JOIN dat_san d ON d.id=h.dat_san_id WHERE h.id=? AND d.nguoi_dung_id=? AND "+EVENTS,Long.class,id,userId);
        if (owned==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy thông báo của bạn.");
        jdbc.update("INSERT IGNORE INTO thong_bao_da_doc(lich_su_id,nguoi_dung_id,doc_luc) VALUES (?,?,?)",id,userId,LocalDateTime.now(clock));
    }
    @Transactional
    public void docTatCa(String email) {
        long userId=users.findByEmail(email).orElseThrow().getId();
        jdbc.update("INSERT IGNORE INTO thong_bao_da_doc(lich_su_id,nguoi_dung_id,doc_luc) SELECT h.id,d.nguoi_dung_id,? FROM lich_su_dat_san h JOIN dat_san d ON d.id=h.dat_san_id WHERE d.nguoi_dung_id=? AND "+EVENTS,LocalDateTime.now(clock),userId);
    }
}
