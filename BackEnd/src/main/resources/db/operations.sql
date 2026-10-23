INSERT INTO vai_tro(ma, ten) VALUES ('STAFF', 'Nhân viên')
ON DUPLICATE KEY UPDATE ma = vai_tro.ma;

CREATE TABLE IF NOT EXISTS xac_minh_email (
    nguoi_dung_id BIGINT NOT NULL PRIMARY KEY,
    xac_minh_luc DATETIME(6) NULL,
    FOREIGN KEY (nguoi_dung_id) REFERENCES nguoi_dung(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ma_otp (
    nguoi_dung_id BIGINT NOT NULL,
    muc_dich VARCHAR(30) NOT NULL,
    ma_bam VARCHAR(100) NOT NULL,
    het_han DATETIME(6) NOT NULL,
    gui_luc DATETIME(6) NOT NULL,
    so_lan_sai INT NOT NULL DEFAULT 0,
    da_dung BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (nguoi_dung_id,muc_dich),
    FOREIGN KEY (nguoi_dung_id) REFERENCES nguoi_dung(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS gioi_han_yeu_cau (
    khoa CHAR(64) NOT NULL PRIMARY KEY,
    bat_dau DATETIME(6) NOT NULL,
    so_lan INT NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS thong_bao_da_doc (
    lich_su_id BIGINT NOT NULL PRIMARY KEY,
    nguoi_dung_id BIGINT NOT NULL,
    doc_luc DATETIME(6) NOT NULL,
    FOREIGN KEY (lich_su_id) REFERENCES lich_su_dat_san(id),
    FOREIGN KEY (nguoi_dung_id) REFERENCES nguoi_dung(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS danh_gia_san (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    dat_san_id BIGINT NOT NULL,
    san_id BIGINT NOT NULL,
    diem INT NOT NULL,
    binh_luan VARCHAR(2000) NOT NULL,
    ngay_tao DATETIME(6) NOT NULL,
    UNIQUE KEY uq_danh_gia_don_san (dat_san_id,san_id),
    CONSTRAINT ck_danh_gia_diem CHECK (diem BETWEEN 1 AND 5),
    FOREIGN KEY (dat_san_id) REFERENCES dat_san(id),
    FOREIGN KEY (san_id) REFERENCES san(id),
    INDEX ix_danh_gia_san_ngay (san_id,ngay_tao,id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS nhat_ky_quan_tri (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nguoi_thuc_hien_id BIGINT NOT NULL,
    hanh_dong VARCHAR(60) NOT NULL,
    doi_tuong VARCHAR(100) NOT NULL,
    noi_dung VARCHAR(1000) NOT NULL,
    ngay_tao DATETIME(6) NOT NULL,
    CONSTRAINT fk_nhat_ky_quan_tri_nguoi FOREIGN KEY (nguoi_thuc_hien_id) REFERENCES nguoi_dung(id),
    INDEX ix_nhat_ky_quan_tri_ngay (ngay_tao, id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS uu_dai_dat_san (
    dat_san_id BIGINT NOT NULL PRIMARY KEY,
    tam_tinh DECIMAL(12,2) NOT NULL,
    ty_le_giam INT NOT NULL,
    so_tien_giam DECIMAL(12,2) NOT NULL,
    hang_thanh_vien VARCHAR(50) NOT NULL,
    CONSTRAINT fk_uu_dai_dat_san FOREIGN KEY (dat_san_id) REFERENCES dat_san(id),
    CONSTRAINT ck_uu_dai_tien CHECK (tam_tinh >= 0 AND so_tien_giam >= 0 AND so_tien_giam <= tam_tinh AND so_tien_giam <= 50000),
    CONSTRAINT ck_uu_dai_ty_le CHECK (ty_le_giam IN (0,5,10))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS phien_ban_mat_khau (
    nguoi_dung_id BIGINT NOT NULL PRIMARY KEY,
    phien_ban BIGINT NOT NULL,
    CONSTRAINT fk_phien_ban_mat_khau_nguoi FOREIGN KEY (nguoi_dung_id) REFERENCES nguoi_dung(id)
) ENGINE=InnoDB;
