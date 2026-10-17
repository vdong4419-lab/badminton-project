package com.dong.dat_san.service;

import com.dong.dat_san.dto.*;
import com.dong.dat_san.entity.*;
import com.dong.dat_san.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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

    public DatSanService(KhoDuLieuLichSan lich, KhoDuLieuGiuCho giuCho, KhoDuLieuDatSan don,
                         KhoDuLieuNguoiDung users, KhoDuLieuChiTietDatSan chiTiet, Clock clock) {
        this.lich = lich;
        this.giuCho = giuCho;
        this.don = don;
        this.users = users;
        this.chiTiet = chiTiet;
        this.clock = clock;
    }

    @Transactional
    public DatSanDto taoDatSan(String email, List<Long> ids) {
        NguoiDung user = nguoiDung(email);
        if (ids == null || ids.isEmpty() || ids.size() > 48
                || ids.stream().anyMatch(id -> id == null || id <= 0)
                || new HashSet<>(ids).size() != ids.size()) {
            throw loi(HttpStatus.BAD_REQUEST, "Danh sách suất không hợp lệ, rỗng hoặc bị trùng.");
        }
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
        booking.setTongTien(slots.stream().map(ThucTheLichSan::getGia).reduce(BigDecimal.ZERO, BigDecimal::add));
        booking.setGhiChu("Đặt sân trực tuyến qua hệ thống");
        booking.setNgayTao(now);
        don.saveAndFlush(booking);

        List<ChiTietDatSan> details = new ArrayList<>();
        for (var slot : slots) {
            var detail = chiTiet.saveAndFlush(new ChiTietDatSan(booking, slot));
            giuCho.chiemSuat(slot.getId(), detail.getId(), now);
            details.add(detail);
        }
        don.ghiLichSu(booking.getId(), user.getId(), null, "CHO_XAC_NHAN", "Tạo đơn đặt sân", now);
        return dto(booking, details, false);
    }

    @Transactional(readOnly = true)
    public List<DatSanDto> xemLichSu(String email) {
        NguoiDung user = nguoiDung(email);
        return don.findByNguoiDungIdOrderByNgayTaoDescIdDesc(user.getId()).stream()
                .map(booking -> dto(booking, chiTiet.findByDatSanIdOrderByIdAsc(booking.getId()),
                        don.countDaThanhToan(booking.getId()) > 0))
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
        boolean paid = don.countDaThanhToan(id) > 0;
        if (!coTheHuy(booking, details, paid)) {
            throw loi(HttpStatus.CONFLICT, paid
                    ? "Đơn đã thanh toán. Vui lòng liên hệ quản trị viên để xử lý hủy và hoàn tiền."
                    : "Chỉ được hủy đơn chưa bắt đầu và đang chờ hoặc đã xác nhận.");
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
        don.ghiLichSu(id, user.getId(), previous, "DA_HUY", note, booking.getNgayHuy());
        return "Đã hủy đơn và giải phóng toàn bộ suất lịch.";
    }

    private NguoiDung nguoiDung(String email) {
        var user = users.findByEmail(email)
                .orElseThrow(() -> loi(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập lại."));
        if (!user.isDangHoatDong()) {
            throw loi(HttpStatus.UNAUTHORIZED, "Tài khoản đã bị khóa.");
        }
        return user;
    }

    private boolean coTheHuy(ThucTheDatSan booking, List<ChiTietDatSan> details, boolean paid) {
        return !paid && Set.of("CHO_XAC_NHAN", "DA_XAC_NHAN").contains(booking.getTrangThai())
                && !details.isEmpty()
                && details.stream().allMatch(detail -> batDau(detail.getLichSan()).isAfter(LocalDateTime.now(clock)));
    }

    private DatSanDto dto(ThucTheDatSan booking, List<ChiTietDatSan> details, boolean paid) {
        var items = details.stream()
                .map(detail -> new DatSanDto.ChiTietDto(detail.getId(), detail.getDonGia(), LichSanDto.from(detail.getLichSan())))
                .toList();
        return new DatSanDto(booking.getId(), booking.getMaDat(), booking.getTrangThai(), booking.getTongTien(),
                booking.getLyDoHuy(), booking.getNgayTao(), booking.getNgayHuy(),
                coTheHuy(booking, details, paid), items);
    }

    private static LocalDateTime batDau(ThucTheLichSan slot) {
        return slot.getNgayChoi().atTime(slot.getKhungGio().getGioBatDau());
    }

    private static ResponseStatusException loi(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
