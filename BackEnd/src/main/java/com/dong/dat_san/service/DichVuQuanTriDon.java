package com.dong.dat_san.service;

import com.dong.dat_san.dto.*;
import com.dong.dat_san.entity.*;
import com.dong.dat_san.repository.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
public class DichVuQuanTriDon {
    private final KhoDuLieuDatSan orders;
    private final KhoDuLieuChiTietDatSan details;
    private final KhoDuLieuThanhToan payments;
    private final KhoDuLieuNguoiDung users;
    private final KhoDuLieuLichSan slots;
    private final KhoDuLieuGiuCho holds;
    private final DatSanService views;
    private final ChinhSachDatSan policy;
    private final JdbcTemplate jdbc;
    private final QuyenVanHanh access;

    public DichVuQuanTriDon(KhoDuLieuDatSan orders, KhoDuLieuChiTietDatSan details,
            KhoDuLieuThanhToan payments, KhoDuLieuNguoiDung users, KhoDuLieuLichSan slots,
            KhoDuLieuGiuCho holds, DatSanService views, ChinhSachDatSan policy, JdbcTemplate jdbc, QuyenVanHanh access) {
        this.orders=orders; this.details=details; this.payments=payments; this.users=users;
        this.slots=slots; this.holds=holds; this.views=views; this.policy=policy; this.jdbc=jdbc;
        this.access=access;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> danhSach(String status, boolean refund, int page, java.time.LocalDate day, String search) {
        if (page < 0 || (status != null && !Set.of("CHO_XAC_NHAN", "DA_XAC_NHAN", "DA_HOAN_THANH", "DA_HUY").contains(status))) {
            throw error(HttpStatus.BAD_REQUEST, "Bộ lọc hoặc trang không hợp lệ.");
        }
        if (search == null) search="";
        if (search.length()>100) throw error(HttpStatus.BAD_REQUEST,"Từ khóa quá dài.");
        var result=orders.timQuanTri(status, refund, access.ngayDuocXem(day), search.trim(), PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "ngayTao", "id")));
        return Map.of("items", result.getContent().stream().map(d -> view(d, false)).toList(),
                "page", result.getNumber(), "totalPages", result.getTotalPages(), "totalElements", result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public QuanTriDonDto chiTiet(Long id) {
        var order=orders.findById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Không tìm thấy đơn."));
        access.kiemTraDon(order);
        return view(order, true);
    }

    @Transactional
    public QuanTriDonDto xacNhan(Long id, String email) {
        var booking=locked(id);
        var items=details.findByDatSanIdOrderByIdAsc(id);
        var payment=payments.findLockedByDatSanId(id).orElse(null);
        if (!"CHO_XAC_NHAN".equals(booking.getTrangThai()) || policy.hetHan(booking, items, payment)
                || !policy.chuaBatDau(items)) throw error(HttpStatus.CONFLICT, "Đơn không còn chờ xác nhận, đã hết hạn hoặc đã đến giờ chơi.");
        // Xác nhận chỉ khi mọi suất còn do chính đơn này giữ.
        long held=jdbc.queryForObject("""
                SELECT COUNT(*) FROM giu_cho g JOIN chi_tiet_dat_san c ON c.id=g.chi_tiet_dat_san_id
                WHERE c.dat_san_id=?
                """, Long.class, id);
        if (held != items.size()) throw error(HttpStatus.CONFLICT, "Đơn không giữ đủ các suất lịch.");
        booking.setTrangThai("DA_XAC_NHAN");
        orders.ghiLichSu(id, actor(email).getId(), "CHO_XAC_NHAN", "DA_XAC_NHAN", "Xác nhận đặt sân", policy.now());
        return view(booking, true);
    }

    @Transactional
    public QuanTriDonDto thanhToan(Long id, String email, YeuCauThanhToan request) {
        var booking=locked(id);
        if (!"DA_XAC_NHAN".equals(booking.getTrangThai())) throw error(HttpStatus.CONFLICT, "Chỉ thu tiền cho đơn đã xác nhận.");
        var payment=payments.findLockedByDatSanId(id).orElseGet(ThanhToan::new);
        if (payment.getTrangThai() != null && !"CHUA_THANH_TOAN".equals(payment.getTrangThai())) {
            throw error(HttpStatus.CONFLICT, "Đơn đã được thu tiền hoặc hoàn tiền.");
        }
        String reference=request.maThamChieu() == null ? null : request.maThamChieu().trim();
        if ("CHUYEN_KHOAN".equals(request.phuongThuc()) && (reference == null || reference.isBlank())) {
            throw error(HttpStatus.BAD_REQUEST, "Chuyển khoản cần mã giao dịch để đối chiếu.");
        }
        var admin=actor(email);
        payment.setDatSanId(id);
        payment.setSoTien(booking.getTongTien());
        payment.setPhuongThuc(request.phuongThuc());
        payment.setMaThamChieu(reference == null || reference.isBlank() ? null : reference);
        payment.setTrangThai("DA_THANH_TOAN");
        payment.setNguoiXacNhanId(admin.getId());
        payment.setThoiDiemThanhToan(policy.now());
        if (payment.getId() == null) payment.setNgayTao(policy.now());
        payments.saveAndFlush(payment);
        orders.ghiLichSu(id, admin.getId(), booking.getTrangThai(), booking.getTrangThai(),
                "THU_TIEN | " + payment.getSoTien().toPlainString() + " VND | " + request.phuongThuc()
                        + " | Mã: " + Objects.toString(reference, "tiền mặt"), policy.now());
        return view(booking, true);
    }

    @Transactional
    public QuanTriDonDto huy(Long id, String email, String reason) {
        var booking=locked(id);
        var items=details.findByDatSanIdOrderByIdAsc(id);
        var payment=payments.findLockedByDatSanId(id).orElse(null);
        if (!policy.coTheHuy(booking, items, payment)) throw error(HttpStatus.CONFLICT, "Chỉ hủy đơn còn hiệu lực và chưa bắt đầu.");
        if (access.laNhanVien() && ChinhSachDatSan.daThu(payment)) throw error(HttpStatus.FORBIDDEN,"Đơn đã thu tiền cần admin xử lý hủy và hoàn tiền.");
        String note=reason == null ? "" : reason.trim();
        if (note.isBlank() || note.length() > 500) throw error(HttpStatus.BAD_REQUEST, "Cần lý do hủy từ 1 đến 500 ký tự.");
        slots.khoaLich(items.stream().map(d -> d.getLichSan().getId()).sorted().toList());
        String old=booking.getTrangThai();
        booking.setTrangThai("DA_HUY");
        booking.setLyDoHuy(note);
        booking.setNgayHuy(policy.now());
        holds.giaiPhongDon(id);
        orders.ghiLichSu(id, actor(email).getId(), old, "DA_HUY",
                "Vận hành hủy: " + note + (ChinhSachDatSan.daThu(payment) ? " | Chờ hoàn đủ tiền đã thu" : ""), policy.now());
        return view(booking, true);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public QuanTriDonDto hoanTien(Long id, String email, YeuCauHoanTien request) {
        var booking=locked(id);
        var payment=payments.findLockedByDatSanId(id).orElse(null);
        if (!"DA_HUY".equals(booking.getTrangThai()) || !ChinhSachDatSan.daThu(payment)) {
            throw error(HttpStatus.CONFLICT, "Chỉ hoàn tiền cho đơn đã hủy, đã thu và chưa hoàn tiền.");
        }
        payment.setTrangThai("DA_HOAN_TIEN");
        payment.setThoiDiemHoanTien(policy.now());
        payments.saveAndFlush(payment);
        orders.ghiLichSu(id, actor(email).getId(), "DA_HUY", "DA_HUY",
                "HOAN_TIEN | Hoàn đủ " + payment.getSoTien().toPlainString() + " VND | Biên nhận: "
                        + request.maBienNhan().trim() + " | " + request.ghiChu().trim(), policy.now());
        return view(booking, true);
    }

    @Transactional
    public QuanTriDonDto hoanThanh(Long id, String email) {
        var booking=locked(id);
        var items=details.findByDatSanIdOrderByIdAsc(id);
        var payment=payments.findLockedByDatSanId(id).orElse(null);
        if (!policy.coTheHoanThanh(booking, items, payment)) {
            throw error(HttpStatus.CONFLICT, "Chỉ hoàn thành đơn đã xác nhận, đã thu tiền và đã kết thúc mọi suất.");
        }
        booking.setTrangThai("DA_HOAN_THANH");
        orders.ghiLichSu(id, actor(email).getId(), "DA_XAC_NHAN", "DA_HOAN_THANH", "Hoàn thành đơn", policy.now());
        return view(booking, true);
    }

    @Transactional
    public QuanTriDonDto checkIn(Long id,String email) {
        var order=locked(id);
        var items=details.findByDatSanIdOrderByIdAsc(id);
        if (!coTheCheckIn(order,items)) throw error(HttpStatus.CONFLICT,"Chỉ check-in một lần cho đơn đã xác nhận, từ 15 phút trước giờ chơi đến hết suất cuối.");
        orders.ghiLichSu(id,actor(email).getId(),order.getTrangThai(),order.getTrangThai(),"CHECK_IN | Khách đã đến sân",policy.now());
        return view(order,true);
    }

    private boolean coTheCheckIn(ThucTheDatSan order,List<ChiTietDatSan> items) {
        if (!"DA_XAC_NHAN".equals(order.getTrangThai()) || items.isEmpty()) return false;
        var start=items.stream().map(d -> ChinhSachDatSan.batDau(d.getLichSan())).min(java.time.LocalDateTime::compareTo).orElseThrow();
        var end=items.stream().map(d -> d.getLichSan().getNgayChoi().atTime(d.getLichSan().getKhungGio().getGioKetThuc())).max(java.time.LocalDateTime::compareTo).orElseThrow();
        return !policy.now().isBefore(start.minusMinutes(15)) && policy.now().isBefore(end)
                && jdbc.queryForObject("SELECT COUNT(*) FROM lich_su_dat_san WHERE dat_san_id=? AND ghi_chu LIKE 'CHECK_IN |%'",Long.class,order.getId())==0;
    }

    private ThucTheDatSan locked(Long id) {
        var order=orders.findLockedById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Không tìm thấy đơn."));
        access.kiemTraDon(order);
        return order;
    }
    private NguoiDung actor(String email) {
        return users.findByEmail(email).filter(NguoiDung::isDangHoatDong)
                .orElseThrow(() -> error(HttpStatus.UNAUTHORIZED, "Tài khoản không còn hoạt động."));
    }
    private QuanTriDonDto view(ThucTheDatSan booking, boolean history) {
        var dto=views.phanHoi(booking);
        var user=booking.getNguoiDung();
        var payment=payments.findByDatSanId(booking.getId()).orElse(null);
        var items=details.findByDatSanIdOrderByIdAsc(booking.getId());
        List<String> actions=new ArrayList<>();
        if ("CHO_XAC_NHAN".equals(booking.getTrangThai()) && !policy.hetHan(booking, items, payment) && policy.chuaBatDau(items)) actions.add("XAC_NHAN");
        if ("DA_XAC_NHAN".equals(booking.getTrangThai()) && (payment == null || "CHUA_THANH_TOAN".equals(payment.getTrangThai()))) actions.add("THANH_TOAN");
        if (policy.coTheHuy(booking, items, payment) && (!access.laNhanVien() || !ChinhSachDatSan.daThu(payment))) actions.add("HUY");
        if (dto.thanhToan().choHoanTien() && !access.laNhanVien()) actions.add("HOAN_TIEN");
        if (policy.coTheHoanThanh(booking, items, payment)) actions.add("HOAN_THANH");
        if (coTheCheckIn(booking,items)) actions.add("CHECK_IN");
        List<QuanTriDonDto.SuKienDto> events=history ? jdbc.query("""
                SELECT h.id, COALESCE(n.ho_ten,'Hệ thống') AS actor, h.trang_thai_cu, h.trang_thai_moi, h.ghi_chu, h.ngay_tao
                FROM lich_su_dat_san h LEFT JOIN nguoi_dung n ON n.id=h.nguoi_thuc_hien_id
                WHERE h.dat_san_id=? ORDER BY h.id
                """, (row, index) -> new QuanTriDonDto.SuKienDto(row.getLong("id"), row.getString("actor"),
                        row.getString("trang_thai_cu"), row.getString("trang_thai_moi"), row.getString("ghi_chu"),
                        row.getTimestamp("ngay_tao").toLocalDateTime()), booking.getId()) : List.of();
        return new QuanTriDonDto(dto, new QuanTriDonDto.KhachDto(user.getId(), user.getHoTen(), user.getEmail(), user.getSoDienThoai()), actions, events);
    }
    private static ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
