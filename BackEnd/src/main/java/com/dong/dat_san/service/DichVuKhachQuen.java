package com.dong.dat_san.service;

import com.dong.dat_san.dto.UuDaiDto;
import com.dong.dat_san.entity.NguoiDung;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DichVuKhachQuen {
    private static final BigDecimal GIOI_HAN = new BigDecimal("50000");
    private final JdbcTemplate jdbc;
    public DichVuKhachQuen(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record ThanhVien(long soDonHoanThanh, BigDecimal tongChiTieu, String hang,
                            int tyLeGiam, BigDecimal giamToiDa, long donConLai, int mocTiepTheo) {}

    public ThanhVien thongTin(NguoiDung user) {
        var stats = jdbc.queryForObject("""
                SELECT COUNT(*) AS so_don, COALESCE(SUM(t.so_tien),0) AS chi_tieu
                FROM dat_san d JOIN thanh_toan t ON t.dat_san_id=d.id
                WHERE d.nguoi_dung_id=? AND d.trang_thai='DA_HOAN_THANH' AND t.trang_thai='DA_THANH_TOAN'
                """, (row, index) -> new Object[]{row.getLong("so_don"), row.getBigDecimal("chi_tieu")}, user.getId());
        long count = (Long) stats[0];
        boolean customer = "CUSTOMER".equals(user.getVaiTro().getMa());
        int rate = !customer ? 0 : count >= 10 ? 10 : count >= 5 ? 5 : 0;
        int next = count >= 10 ? 0 : count >= 5 ? 10 : 5;
        return new ThanhVien(count, (BigDecimal) stats[1], !customer ? "Tài khoản vận hành" : rate == 10 ? "Thân thiết" : rate == 5 ? "Khách quen" : "Thành viên",
                rate, GIOI_HAN, customer && next > 0 ? next - count : 0, customer ? next : 0);
    }

    public UuDaiDto tinh(NguoiDung user, BigDecimal subtotal) {
        var member = thongTin(user);
        BigDecimal discount = subtotal.multiply(BigDecimal.valueOf(member.tyLeGiam()))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN).min(GIOI_HAN);
        return new UuDaiDto(subtotal, member.tyLeGiam(), discount, subtotal.subtract(discount), member.hang());
    }

    public void luu(Long orderId, UuDaiDto quote) {
        jdbc.update("INSERT INTO uu_dai_dat_san(dat_san_id,tam_tinh,ty_le_giam,so_tien_giam,hang_thanh_vien) VALUES (?,?,?,?,?)",
                orderId, quote.tamTinh(), quote.tyLeGiam(), quote.soTienGiam(), quote.hangThanhVien());
    }

    public UuDaiDto daApDung(Long id, BigDecimal total) {
        return jdbc.query("SELECT tam_tinh,ty_le_giam,so_tien_giam,hang_thanh_vien FROM uu_dai_dat_san WHERE dat_san_id=?",
                (row, index) -> new UuDaiDto(row.getBigDecimal("tam_tinh"), row.getInt("ty_le_giam"),
                        row.getBigDecimal("so_tien_giam"), total, row.getString("hang_thanh_vien")), id)
                .stream().findFirst().orElse(new UuDaiDto(total, 0, BigDecimal.ZERO, total, "Chưa áp dụng"));
    }
}
