package com.dong.dat_san.service;

import com.dong.dat_san.entity.NguoiDung;
import com.dong.dat_san.entity.ThucTheDatSan;
import com.dong.dat_san.entity.ThucTheGiuCho;
import com.dong.dat_san.entity.ThucTheLichSan;
import com.dong.dat_san.repository.KhoDuLieuDatSan;
import com.dong.dat_san.repository.KhoDuLieuGiuCho;
import com.dong.dat_san.repository.KhoDuLieuLichSan;
import com.dong.dat_san.repository.KhoDuLieuNguoiDung;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class DatSanService {

    @Autowired
    private KhoDuLieuLichSan khoDuLieuLichSan;

    @Autowired
    private KhoDuLieuGiuCho khoDuLieuGiuCho;

    @Autowired
    private KhoDuLieuDatSan khoDuLieuDatSan;

    @Autowired
    private KhoDuLieuNguoiDung khoDuLieuNguoiDung;

    @Transactional
    public String taoDatSan(Long nguoiDungId, List<Long> danhSachLichSanIds) {
        // 1. Kiểm tra người dùng tồn tại
        NguoiDung nguoiDung = khoDuLieuNguoiDung.findById(nguoiDungId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin người dùng!"));

        // 2. Kiểm tra các suất lịch có tồn tại không
        List<ThucTheLichSan> danhSachLich = khoDuLieuLichSan.findAllById(danhSachLichSanIds);
        if (danhSachLich.size() != danhSachLichSanIds.size()) {
            throw new RuntimeException("Một số suất lịch không tồn tại!");
        }

        // 3. Tính tổng tiền
        BigDecimal tongTien = danhSachLich.stream()
                .map(ThucTheLichSan::getGia)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. Tạo và lưu đơn đặt sân vào bảng dat_san
        ThucTheDatSan datSan = new ThucTheDatSan();
        datSan.setMaDat(UUID.randomUUID().toString());
        datSan.setNguoiDung(nguoiDung);
        datSan.setTrangThai("CHO_XAC_NHAN");
        datSan.setTongTien(tongTien);
        datSan.setGhiChu("Đặt sân trực tuyến qua hệ thống");
        
        ThucTheDatSan savedDatSan = khoDuLieuDatSan.save(datSan);

        // 5. Giữ chỗ từng suất lịch để chống đặt trùng
        for (Long lichSanId : danhSachLichSanIds) {
            try {
                ThucTheGiuCho giuCho = new ThucTheGiuCho(lichSanId, savedDatSan.getId());
                khoDuLieuGiuCho.save(giuCho);
            } catch (Exception e) {
                throw new RuntimeException("Suất lịch với ID " + lichSanId + " đã có người giữ chỗ hoặc đặt trước!");
            }
        }

        return "Đặt sân thành công! Mã đơn: " + savedDatSan.getMaDat() + " | Tổng tiền: " + tongTien;
    }

        @Transactional
        public String huyDatSan(Long nguoiDungId, Long datSanId, String lyDoHuy) {
            ThucTheDatSan datSan = khoDuLieuDatSan.findById(datSanId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt sân!"));

            if (!datSan.getNguoiDung().getId().equals(nguoiDungId)) {
                throw new RuntimeException("Bạn không có quyền hủy đơn đặt sân này!");
            }

            if ("DA_HUY".equals(datSan.getTrangThai())) {
                throw new RuntimeException("Đơn đặt sân này đã bị hủy trước đó rồi!");
            }

            // Cập nhật trạng thái đơn
            datSan.setTrangThai("DA_HUY");
            datSan.setLyDoHuy(lyDoHuy);
            khoDuLieuDatSan.save(datSan);

            // Xóa các bản ghi giữ chỗ dựa trên ID đơn đặt sân để giải phóng lịch
            khoDuLieuGiuCho.deleteByChiTietDatSanId(datSan.getId());

            return "Đã hủy đơn đặt sân và hoàn trả suất lịch thành công!";
        }
}