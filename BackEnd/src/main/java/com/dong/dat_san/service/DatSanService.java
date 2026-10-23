package com.dong.dat_san.service;

import com.dong.dat_san.dto.*;
import com.dong.dat_san.entity.*;
import com.dong.dat_san.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DatSanService {
    private final KhoDuLieuLichSan lich;
    private final KhoDuLieuGiuCho giuCho;
    private final KhoDuLieuDatSan don;
    private final KhoDuLieuNguoiDung users;
    private final KhoDuLieuChiTietDatSan chiTiet;
    private final Clock clock;
    private final KhoDuLieuThanhToan payments;
    private final ChinhSachDatSan policy;
    private final SanRepository courts;
    private final DichVuKhachQuen loyalty;
    private final JdbcTemplate jdbc;

    public DatSanService(KhoDuLieuLichSan lich, KhoDuLieuGiuCho giuCho, KhoDuLieuDatSan don,
                         KhoDuLieuNguoiDung users, KhoDuLieuChiTietDatSan chiTiet, Clock clock,
                         KhoDuLieuThanhToan payments, ChinhSachDatSan policy, SanRepository courts,
                         DichVuKhachQuen loyalty, JdbcTemplate jdbc) {
        this.lich = lich;
        this.giuCho = giuCho;
        this.don = don;
        this.users = users;
        this.chiTiet = chiTiet;
        this.clock = clock;
        this.payments = payments;
        this.policy = policy;
        this.courts = courts;
        this.loyalty = loyalty;
        this.jdbc = jdbc;
    }

    @Transactional
    public DatSanDto taoDatSan(String email, List<Long> ids) {
        NguoiDung user = nguoiDung(email);
        kiemTraIds(ids);
        courts.khoaTheoLich(ids);
        List<ThucTheLichSan> slots = lich.khoaLich(ids.stream().sorted().toList());
        if (slots.size() != ids.size()) {
            throw loi(HttpStatus.NOT_FOUND, "Một số suất lịch không tồn tại.");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        for (var slot : slots) {
            if (!slot.isMoDat() || !slot.getSan().isDangHoatDong() || !batDau(slot).isAfter(now)) {
                throw loi(HttpStatus.CONFLICT, "Suất đã qua giờ bắt đầu, đóng đặt hoặc sân ngừng hoạt động.");
            }
        }
        if (giuCho.existsByLichSanIdIn(ids)) {
            throw loi(HttpStatus.CONFLICT, "Một số suất vừa được người khác đặt. Vui lòng chọn lại.");
        }

        ThucTheDatSan booking = new ThucTheDatSan();
        booking.setMaDat(UUID.randomUUID().toString());
        booking.setNguoiDung(user);
        booking.setTrangThai("CHO_XAC_NHAN");
        var quote = loyalty.tinh(user, slots.stream().map(ThucTheLichSan::getGia).reduce(BigDecimal.ZERO, BigDecimal::add));
        booking.setTongTien(quote.tongTien());
        booking.setGhiChu("Đặt sân trực tuyến qua hệ thống");
        booking.setNgayTao(now);
        don.saveAndFlush(booking);
        loyalty.luu(booking.getId(), quote);

        List<ChiTietDatSan> details = new ArrayList<>();
        for (var slot : slots) {
            var detail = chiTiet.saveAndFlush(new ChiTietDatSan(booking, slot));
            giuCho.chiemSuat(slot.getId(), detail.getId(), now);
            details.add(detail);
        }
        don.ghiLichSu(booking.getId(), user.getId(), null, "CHO_XAC_NHAN", "Tạo đơn đặt sân", now);
        return dto(booking, details, null);
    }

    @Transactional(readOnly = true)
    public UuDaiDto baoGia(String email, List<Long> ids) {
        kiemTraIds(ids);
        var slots = lich.findAllById(ids);
        if (slots.size() != ids.size()) throw loi(HttpStatus.NOT_FOUND,"Một số suất lịch không tồn tại.");
        var now = LocalDateTime.now(clock);
        if (slots.stream().anyMatch(s -> !s.isMoDat() || !s.getSan().isDangHoatDong() || !batDau(s).isAfter(now))
                || giuCho.existsByLichSanIdIn(ids))
            throw loi(HttpStatus.CONFLICT,"Suất đã đóng, đã qua giờ hoặc đã được đặt. Vui lòng chọn lại.");
        return loyalty.tinh(nguoiDung(email), slots.stream().map(ThucTheLichSan::getGia).reduce(BigDecimal.ZERO,BigDecimal::add));
    }

    @Transactional(readOnly = true)
    public DatSanDto chiTietCuaToi(String email, Long id) {
        return phanHoi(donCuaToi(email,id,false));
    }

    @Transactional
    public DatSanDto baoChuyenKhoan(String email, Long id) {
        var booking = donCuaToi(email,id,true);
        var items = chiTiet.findByDatSanIdOrderByIdAsc(id);
        var payment = payments.findLockedByDatSanId(id).orElse(null);
        if (!coTheChuyenKhoan(booking,items,payment))
            throw loi(HttpStatus.CONFLICT,"Chỉ báo chuyển khoản cho đơn đã xác nhận, chưa thu tiền và chưa bắt đầu.");
        // A locking read sees a notification committed by a request that held the order lock before us.
        var notifiedAt = jdbc.query("SELECT ngay_tao FROM lich_su_dat_san WHERE dat_san_id=? AND ghi_chu LIKE 'BAO_CHUYEN_KHOAN |%' ORDER BY id LIMIT 1 FOR UPDATE",
                (row,index) -> row.getTimestamp("ngay_tao").toLocalDateTime(),id).stream().findFirst().orElse(null);
        if (notifiedAt == null) {
            notifiedAt = LocalDateTime.now(clock);
            don.ghiLichSu(id,booking.getNguoiDung().getId(),booking.getTrangThai(),booking.getTrangThai(),
                    "BAO_CHUYEN_KHOAN | Khách báo đã chuyển khoản, cần đối chiếu ngân hàng | DS"+id,notifiedAt);
        }
        return dto(booking,items,payment,notifiedAt);
    }

    private ThucTheDatSan donCuaToi(String email, Long id, boolean lock) {
        var user = nguoiDung(email);
        var booking = (lock ? don.findLockedById(id) : don.findById(id))
                .orElseThrow(() -> loi(HttpStatus.NOT_FOUND,"Không tìm thấy đơn đặt sân."));
        if (!booking.getNguoiDung().getId().equals(user.getId()))
            throw loi(HttpStatus.FORBIDDEN,"Bạn chỉ được xem và xử lý đơn của mình.");
        return booking;
    }

    private LocalDateTime baoChuyenKhoanLuc(Long id) {
        return jdbc.queryForObject("SELECT MIN(ngay_tao) FROM lich_su_dat_san WHERE dat_san_id=? AND ghi_chu LIKE 'BAO_CHUYEN_KHOAN |%'",
                LocalDateTime.class,id);
    }

    private boolean coTheChuyenKhoan(ThucTheDatSan booking, List<ChiTietDatSan> items, ThanhToan payment) {
        return "DA_XAC_NHAN".equals(booking.getTrangThai()) && policy.chuaBatDau(items)
                && (payment == null || "CHUA_THANH_TOAN".equals(payment.getTrangThai()));
    }

    private static void kiemTraIds(List<Long> ids) {
        if (ids == null || ids.isEmpty() || ids.size() > 48 || ids.stream().anyMatch(id -> id == null || id <= 0)
                || new HashSet<>(ids).size() != ids.size())
            throw loi(HttpStatus.BAD_REQUEST,"Danh sách suất không hợp lệ, rỗng hoặc bị trùng.");
    }

    @Transactional(readOnly = true)
    public List<DatSanDto> xemLichSu(String email) {
        NguoiDung user = nguoiDung(email);
        return don.findByNguoiDungIdOrderByNgayTaoDescIdDesc(user.getId()).stream()
                .map(booking -> dto(booking, chiTiet.findByDatSanIdOrderByIdAsc(booking.getId()),
                        payments.findByDatSanId(booking.getId()).orElse(null)))
                .toList();
    }

    @Transactional
    public String huyDatSan(String email, Long id, String reason) {
        NguoiDung user = nguoiDung(email);
        ThucTheDatSan booking = don.findLockedById(id)
                .orElseThrow(() -> loi(HttpStatus.NOT_FOUND, "Không tìm thấy đơn đặt sân."));
        if (!booking.getNguoiDung().getId().equals(user.getId())) {
            throw loi(HttpStatus.FORBIDDEN, "Bạn không có quyền hủy đơn này.");
        }
        List<ChiTietDatSan> details = chiTiet.findByDatSanIdOrderByIdAsc(id);
        ThanhToan payment = payments.findLockedByDatSanId(id).orElse(null);
        if (!policy.coTheHuy(booking, details, payment)) {
            throw loi(HttpStatus.CONFLICT, "Chỉ được hủy đơn chưa bắt đầu, còn hạn và đang chờ hoặc đã xác nhận.");
        }
        String note = reason == null || reason.isBlank() ? "Khách hàng hủy" : reason.trim();
        if (note.length() > 500) {
            throw loi(HttpStatus.BAD_REQUEST, "Lý do hủy tối đa 500 ký tự.");
        }
        // Khóa cùng các suất trước khi giải phóng, đồng bộ với giao dịch đặt mới.
        lich.khoaLich(details.stream().map(detail -> detail.getLichSan().getId()).sorted().toList());
        String previous = booking.getTrangThai();
        booking.setTrangThai("DA_HUY");
        booking.setLyDoHuy(note);
        booking.setNgayHuy(LocalDateTime.now(clock));
        giuCho.giaiPhongDon(id);
        boolean paid = ChinhSachDatSan.daThu(payment);
        don.ghiLichSu(id, user.getId(), previous, "DA_HUY", note + (paid ? " | Chờ admin hoàn đủ tiền đã thu" : ""), booking.getNgayHuy());
        return paid ? "Đã hủy đơn. Đang chờ quản trị viên hoàn đủ tiền đã thu."
                : "Đã hủy đơn và giải phóng toàn bộ suất lịch.";
    }

    private NguoiDung nguoiDung(String email) {
        var user = users.findByEmail(email)
                .orElseThrow(() -> loi(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập lại."));
        if (!user.isDangHoatDong()) {
            throw loi(HttpStatus.UNAUTHORIZED, "Tài khoản đã bị khóa.");
        }
        return user;
    }

    public DatSanDto phanHoi(ThucTheDatSan booking) {
        return dto(booking, chiTiet.findByDatSanIdOrderByIdAsc(booking.getId()),
                payments.findByDatSanId(booking.getId()).orElse(null));
    }

    private DatSanDto dto(ThucTheDatSan booking, List<ChiTietDatSan> details, ThanhToan payment) {
        return dto(booking,details,payment,baoChuyenKhoanLuc(booking.getId()));
    }

    private DatSanDto dto(ThucTheDatSan booking, List<ChiTietDatSan> details, ThanhToan payment, LocalDateTime notifiedAt) {
        var items = details.stream()
                .map(detail -> new DatSanDto.ChiTietDto(detail.getId(), detail.getDonGia(), LichSanDto.from(detail.getLichSan())))
                .toList();
        return new DatSanDto(booking.getId(), booking.getMaDat(), booking.getTrangThai(), booking.getTongTien(),
                booking.getLyDoHuy(), booking.getNgayTao(), booking.getNgayHuy(),
                policy.coTheHuy(booking, details, payment), items, policy.hetHanLuc(booking, details, payment),
                ThanhToanDto.from(booking, payment), loyalty.daApDung(booking.getId(),booking.getTongTien()),
                new DatSanDto.ChuyenKhoanDto("DS"+booking.getId(),notifiedAt,coTheChuyenKhoan(booking,details,payment)));
    }

    private static LocalDateTime batDau(ThucTheLichSan slot) {
        return slot.getNgayChoi().atTime(slot.getKhungGio().getGioBatDau());
    }

    private static ResponseStatusException loi(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
