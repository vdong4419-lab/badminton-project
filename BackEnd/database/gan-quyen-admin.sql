-- Thay bằng email của tài khoản đã đăng ký mà bạn muốn cấp quyền ADMIN.
-- Chạy thủ công bằng tài khoản quản trị database; không có mật khẩu admin mặc định.
USE dat_san_cau_long;
SET @email_admin = 'vdong4419@gmail.com';
UPDATE nguoi_dung n JOIN vai_tro v ON v.ma = 'ADMIN'
SET n.vai_tro_id = v.id
WHERE n.email = @email_admin AND n.dang_hoat_dong = TRUE;
SELECT n.email, n.ho_ten, v.ma AS vai_tro
FROM nguoi_dung n JOIN vai_tro v ON v.id = n.vai_tro_id
WHERE n.email = @email_admin;