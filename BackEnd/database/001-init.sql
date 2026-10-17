SET time_zone = '+07:00';
-- CSDL học tập v1 - MySQL 8.4, InnoDB.
-- Chạy trên database mới. Không xóa hoặc sửa database dự án cũ.
CREATE DATABASE IF NOT EXISTS dat_san_cau_long
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE dat_san_cau_long;
SET NAMES utf8mb4;

CREATE TABLE vai_tro (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ma VARCHAR(30) NOT NULL,
    ten VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_vai_tro_ma UNIQUE (ma)
) ENGINE=InnoDB;

CREATE TABLE nguoi_dung (
    id BIGINT NOT NULL AUTO_INCREMENT,
    vai_tro_id BIGINT NOT NULL,
    ho_ten VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL,
    so_dien_thoai VARCHAR(20) NULL,
    mat_khau_bam VARCHAR(255) NOT NULL COMMENT 'BCrypt, không lưu mật khẩu gốc',
    dang_hoat_dong BOOLEAN NOT NULL DEFAULT TRUE,
    ngay_tao DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ngay_cap_nhat DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
      ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_nguoi_dung_email UNIQUE (email),
    CONSTRAINT uq_nguoi_dung_dien_thoai UNIQUE (so_dien_thoai),
    CONSTRAINT fk_nguoi_dung_vai_tro FOREIGN KEY (vai_tro_id)
      REFERENCES vai_tro(id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE san (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ten_san VARCHAR(100) NOT NULL,
    mo_ta VARCHAR(1000) NULL,
    anh_dai_dien VARCHAR(500) NULL,
    gia_moi_gio DECIMAL(12,2) NOT NULL,
    dang_hoat_dong BOOLEAN NOT NULL DEFAULT TRUE,
    ngay_tao DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_san_ten UNIQUE (ten_san),
    CONSTRAINT ck_san_gia CHECK (gia_moi_gio >= 0)
) ENGINE=InnoDB;

CREATE TABLE khung_gio (
    id BIGINT NOT NULL AUTO_INCREMENT,
    gio_bat_dau TIME NOT NULL,
    gio_ket_thuc TIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_khung_gio_bat_dau UNIQUE (gio_bat_dau),
    CONSTRAINT ck_khung_gio_hop_le CHECK (
      TIME_TO_SEC(gio_bat_dau) >= 0
      AND TIME_TO_SEC(gio_ket_thuc) < 86400
      AND TIME_TO_SEC(gio_ket_thuc) - TIME_TO_SEC(gio_bat_dau) = 1800
      AND MOD(TIME_TO_SEC(gio_bat_dau), 1800) = 0
    )
) ENGINE=InnoDB;

-- Mỗi dòng là một suất 30 phút của một sân trong một ngày.
-- Giá tại đây có thể điều chỉnh theo ngày/giờ, độc lập giá mặc định của sân.
CREATE TABLE lich_san (
    id BIGINT NOT NULL AUTO_INCREMENT,
    san_id BIGINT NOT NULL,
    khung_gio_id BIGINT NOT NULL,
    ngay_choi DATE NOT NULL,
    gia DECIMAL(12,2) NOT NULL,
    mo_dat BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uq_lich_san UNIQUE (san_id, ngay_choi, khung_gio_id),
    CONSTRAINT ck_lich_san_gia CHECK (gia >= 0),
    CONSTRAINT fk_lich_san_san FOREIGN KEY (san_id)
      REFERENCES san(id) ON DELETE RESTRICT,
    CONSTRAINT fk_lich_san_khung_gio FOREIGN KEY (khung_gio_id)
      REFERENCES khung_gio(id) ON DELETE RESTRICT,
    INDEX ix_lich_san_ngay (ngay_choi, san_id)
) ENGINE=InnoDB;

CREATE TABLE dat_san (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ma_dat CHAR(36) NOT NULL COMMENT 'UUID tạo tại backend',
    nguoi_dung_id BIGINT NOT NULL,
    trang_thai VARCHAR(30) NOT NULL DEFAULT 'CHO_XAC_NHAN',
    tong_tien DECIMAL(12,2) NOT NULL,
    ghi_chu VARCHAR(1000) NULL,
    ly_do_huy VARCHAR(500) NULL,
    ngay_huy DATETIME(6) NULL,
    ngay_tao DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ngay_cap_nhat DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
      ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_dat_san_ma UNIQUE (ma_dat),
    CONSTRAINT ck_dat_san_tien CHECK (tong_tien >= 0),
    CONSTRAINT ck_dat_san_trang_thai CHECK (trang_thai IN
      ('CHO_XAC_NHAN', 'DA_XAC_NHAN', 'DA_HOAN_THANH', 'DA_HUY')),
    CONSTRAINT fk_dat_san_nguoi_dung FOREIGN KEY (nguoi_dung_id)
      REFERENCES nguoi_dung(id) ON DELETE RESTRICT,
    INDEX ix_dat_san_nguoi_dung_ngay (nguoi_dung_id, ngay_tao),
    INDEX ix_dat_san_trang_thai_ngay (trang_thai, ngay_tao)
) ENGINE=InnoDB;

-- Giữ lịch sử kể cả khi đơn bị hủy. Giá được chụp tại lúc đặt.
CREATE TABLE chi_tiet_dat_san (
    id BIGINT NOT NULL AUTO_INCREMENT,
    dat_san_id BIGINT NOT NULL,
    lich_san_id BIGINT NOT NULL,
    don_gia DECIMAL(12,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_chi_tiet_don_lich UNIQUE (dat_san_id, lich_san_id),
    CONSTRAINT uq_chi_tiet_id_lich UNIQUE (id, lich_san_id),
    CONSTRAINT ck_chi_tiet_gia CHECK (don_gia >= 0),
    CONSTRAINT fk_chi_tiet_dat_san FOREIGN KEY (dat_san_id)
      REFERENCES dat_san(id) ON DELETE RESTRICT,
    CONSTRAINT fk_chi_tiet_lich_san FOREIGN KEY (lich_san_id)
      REFERENCES lich_san(id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- Chỉ chứa chỗ đang bị chiếm. PK chống hai đơn giữ cùng suất.
-- Khóa ngoại kép buộc chi tiết giữ chỗ phải đúng suất lịch đó.
CREATE TABLE giu_cho (
    lich_san_id BIGINT NOT NULL,
    chi_tiet_dat_san_id BIGINT NOT NULL,
    ngay_tao DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (lich_san_id),
    CONSTRAINT uq_giu_cho_chi_tiet UNIQUE (chi_tiet_dat_san_id),
    CONSTRAINT fk_giu_cho_chi_tiet FOREIGN KEY (chi_tiet_dat_san_id, lich_san_id)
      REFERENCES chi_tiet_dat_san(id, lich_san_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- v1 ghi nhận thanh toán thủ công, chưa kết nối cổng thanh toán.
CREATE TABLE thanh_toan (
    id BIGINT NOT NULL AUTO_INCREMENT,
    dat_san_id BIGINT NOT NULL,
    so_tien DECIMAL(12,2) NOT NULL,
    phuong_thuc VARCHAR(30) NOT NULL,
    trang_thai VARCHAR(30) NOT NULL DEFAULT 'CHUA_THANH_TOAN',
    ma_tham_chieu VARCHAR(100) NULL,
    nguoi_xac_nhan_id BIGINT NULL,
    thoi_diem_thanh_toan DATETIME(6) NULL,
    thoi_diem_hoan_tien DATETIME(6) NULL,
    ngay_tao DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_thanh_toan_dat_san UNIQUE (dat_san_id),
    CONSTRAINT ck_thanh_toan_tien CHECK (so_tien >= 0),
    CONSTRAINT ck_thanh_toan_phuong_thuc CHECK
      (phuong_thuc IN ('TIEN_MAT', 'CHUYEN_KHOAN')),
    CONSTRAINT ck_thanh_toan_trang_thai CHECK
      (trang_thai IN ('CHUA_THANH_TOAN', 'DA_THANH_TOAN', 'DA_HOAN_TIEN')),
    CONSTRAINT fk_thanh_toan_dat_san FOREIGN KEY (dat_san_id)
      REFERENCES dat_san(id) ON DELETE RESTRICT,
    CONSTRAINT fk_thanh_toan_xac_nhan FOREIGN KEY (nguoi_xac_nhan_id)
      REFERENCES nguoi_dung(id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE lich_su_dat_san (
    id BIGINT NOT NULL AUTO_INCREMENT,
    dat_san_id BIGINT NOT NULL,
    nguoi_thuc_hien_id BIGINT NULL,
    trang_thai_cu VARCHAR(30) NULL,
    trang_thai_moi VARCHAR(30) NOT NULL,
    ghi_chu VARCHAR(1000) NULL,
    ngay_tao DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT ck_lich_su_cu CHECK (trang_thai_cu IS NULL OR trang_thai_cu IN
      ('CHO_XAC_NHAN', 'DA_XAC_NHAN', 'DA_HOAN_THANH', 'DA_HUY')),
    CONSTRAINT ck_lich_su_moi CHECK (trang_thai_moi IN
      ('CHO_XAC_NHAN', 'DA_XAC_NHAN', 'DA_HOAN_THANH', 'DA_HUY')),
    CONSTRAINT fk_lich_su_dat_san FOREIGN KEY (dat_san_id)
      REFERENCES dat_san(id) ON DELETE RESTRICT,
    CONSTRAINT fk_lich_su_nguoi_dung FOREIGN KEY (nguoi_thuc_hien_id)
      REFERENCES nguoi_dung(id) ON DELETE RESTRICT,
    INDEX ix_lich_su_dat_san_ngay (dat_san_id, ngay_tao)
) ENGINE=InnoDB;

CREATE TABLE refresh_token (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nguoi_dung_id BIGINT NOT NULL,
    token_bam CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
      COMMENT 'SHA-256 của refresh token ngẫu nhiên; không lưu token gốc',
    het_han_luc DATETIME(6) NOT NULL,
    thu_hoi_luc DATETIME(6) NULL,
    ngay_tao DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_refresh_token_bam UNIQUE (token_bam),
    CONSTRAINT fk_refresh_token_nguoi_dung FOREIGN KEY (nguoi_dung_id)
      REFERENCES nguoi_dung(id) ON DELETE RESTRICT,
    INDEX ix_refresh_token_nguoi_dung (nguoi_dung_id)
) ENGINE=InnoDB;

CREATE TABLE token_khoi_phuc_mat_khau (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nguoi_dung_id BIGINT NOT NULL,
    token_bam CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    het_han_luc DATETIME(6) NOT NULL,
    da_dung_luc DATETIME(6) NULL,
    ngay_tao DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_khoi_phuc_token_bam UNIQUE (token_bam),
    CONSTRAINT fk_khoi_phuc_nguoi_dung FOREIGN KEY (nguoi_dung_id)
      REFERENCES nguoi_dung(id) ON DELETE RESTRICT,
    INDEX ix_khoi_phuc_nguoi_dung (nguoi_dung_id)
) ENGINE=InnoDB;
USE dat_san_cau_long;
SET NAMES utf8mb4;

INSERT INTO vai_tro (ma, ten) VALUES
('ADMIN', 'Quản trị viên'), ('CUSTOMER', 'Khách hàng');

INSERT INTO san (ten_san, mo_ta, gia_moi_gio) VALUES
('Sân 1', 'Sân trong nhà', 100000),
('Sân 2', 'Sân trong nhà', 100000),
('Sân 3', 'Sân trong nhà', 120000),
('Sân 4', 'Sân trong nhà', 120000);

-- 34 khung nửa giờ: 06:00-06:30 ... 22:30-23:00.
INSERT INTO khung_gio (gio_bat_dau, gio_ket_thuc)
WITH RECURSIVE cac_khung AS (
  SELECT 0 AS n
  UNION ALL SELECT n + 1 FROM cac_khung WHERE n < 33
)
SELECT SEC_TO_TIME(21600 + n * 1800), SEC_TO_TIME(23400 + n * 1800)
FROM cac_khung ORDER BY n;

-- Mở lịch 7 ngày, tính từ ngày chạy init theo múi giờ database.
INSERT INTO lich_san (san_id, khung_gio_id, ngay_choi, gia)
WITH RECURSIVE cac_ngay AS (
  SELECT CURRENT_DATE AS ngay, 0 AS n
  UNION ALL SELECT DATE_ADD(ngay, INTERVAL 1 DAY), n + 1
  FROM cac_ngay WHERE n < 6
)
SELECT s.id, k.id, n.ngay, ROUND(s.gia_moi_gio / 2, 2)
FROM san s CROSS JOIN khung_gio k CROSS JOIN cac_ngay n
ORDER BY s.id, n.ngay, k.gio_bat_dau;

-- Không tạo tài khoản giả có mật khẩu chưa mã hóa.
-- Sau này API đăng ký sẽ dùng BCryptPasswordEncoder tạo mat_khau_bam.
-- Các bảng đơn hàng/token/thanh toán trống là bình thường.
USE dat_san_cau_long;

