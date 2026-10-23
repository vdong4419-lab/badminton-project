# Báo cáo sửa lỗi — 17/10/2026

Cập nhật 20/10/2026: đã bổ sung xác minh email và quên mật khẩu bằng OTP Gmail, giới hạn thử đăng nhập, chuông thông báo trạng thái đơn, đánh giá và bình luận sau buổi chơi. Cấu hình Gmail và luồng sử dụng nằm trong [OTP_THONG_BAO_DANH_GIA.md](OTP_THONG_BAO_DANH_GIA.md). Gửi thư thật cần mật khẩu ứng dụng Gmail được nhập trên máy chạy backend.

Cập nhật 18/10/2026: phần quản trị, thu / hoàn tiền, giữ suất 15 phút và dashboard đã được bổ sung sau báo cáo gốc này (xem DASHBOARD_VAN_HANH.md và QUAN_TRI_THANH_TOAN.md). Trang cá nhân, ưu đãi khách quen và chuyển khoản bằng QR được mô tả tại [KHACH_HANG_UU_DAI_QR.md](KHACH_HANG_UU_DAI_QR.md).

Đã hoàn thành sửa các lỗi chính được xác định khi đối chiếu mã nguồn với csdl.txt. Đã kiểm thử bằng HTTP API với MySQL riêng; chưa kiểm thử giao diện bằng trình duyệt hoặc triển khai hệ thống thực tế.

## Đã hoàn thành

| Phần | Kết quả |
| --- | --- |
| Đăng ký/đăng nhập | Có API đăng ký; AuthenticationManager đọc tài khoản trong database, kiểm tra BCrypt và tài khoản bị khóa; trả mã vai trò đúng. |
| JWT và phân quyền | Khóa HS512 đủ độ dài; đọc JWT_SECRET qua môi trường; cấu hình stateless; bộ lọc chạy đúng vị trí, nạp quyền từ database; tạo sân chỉ dành cho ADMIN. |
| Tìm lịch | Dùng đúng ngay_choi; lọc suất bị chiếm, đã qua giờ, đóng đặt và sân ngừng hoạt động. |
| Đặt sân | Tạo đúng đơn → chi tiết có giá chụp → giữ chỗ bằng ID chi tiết; khóa suất theo thứ tự và INSERT giữ chỗ; toàn bộ nằm trong transaction. |
| Hủy sân | Kiểm tra chủ đơn, trạng thái, giờ bắt đầu và thanh toán; giải phóng toàn bộ suất của đơn; ghi ngày/lý do hủy và lịch sử. |
| Bảo vệ dữ liệu | Trả DTO lịch sử có chi tiết sân/giờ/giá; không trả entity người dùng; chặn serialize mật khẩu băm. |
| Frontend | Đồng bộ tên trường/API/body hủy; hiển thị lỗi JSON; chặn gửi lặp khi đang xử lý; tải lại lịch khi xung đột; hủy request cũ khi đổi ngày; kiểm tra phiên đăng nhập. |
| Ngày và giá | Chọn ngày theo múi giờ Việt Nam; định dạng tiền VND; giá lịch sử lấy từ donGia. |
| Duy trì lịch | Bổ sung 7 ngày khi khởi động, mỗi ngày lúc 00:05 và khi admin tạo sân; không ghi đè giá tùy chỉnh hay mở lại suất bị đóng. |
| Cấu hình chạy | URL, mật khẩu database, JWT và CORS qua môi trường; Compose có nhập schema cho database mới và chờ MySQL sẵn sàng; có .env.example, hướng dẫn và script test riêng. |

## Kết quả kiểm tra

- Backend: biên dịch lại thành công, 10/10 bài kiểm thử qua (8 bài tích hợp MySQL và 2 bài JWT), không bỏ qua bài nào trong lượt kiểm tra đầy đủ.
- Có kiểm tra hai yêu cầu đặt cùng suất: chỉ một yêu cầu thành công; yêu cầu còn lại nhận 409.
- Có gây lỗi database sau khi đã ghi một phần giao dịch: đơn, chi tiết và giữ chỗ đều rollback.
- Có kiểm tra hai yêu cầu hủy đồng thời: chỉ một lần chuyển trạng thái và một bản ghi lịch sử hủy.
- Có kiểm tra giữ nguyên giá đã chụp, hủy rồi đặt lại, quyền admin, tài khoản khóa và từ chối hủy đơn người khác/đã chơi/hoàn thành/đã thanh toán.
- Frontend: npm run build và npm run lint thành công, không còn cảnh báo lint trong lượt kiểm tra.
- Kiểm tra thêm hàm ngày tại 01:30 giờ Việt Nam, định dạng tiền và phiên hết hạn: qua.
- Docker Compose: kiểm tra cấu hình thành công. Script kiem-thu-mysql.ps1 đã chạy thành công toàn bộ test trên container mới và tự dọn container đó.

Các dòng lỗi SQL duplicate/injected test conflict trong log kiểm thử là lỗi được chủ động gây ra để xác minh chống đặt trùng và rollback; kết quả bộ test vẫn thành công.

## Việc cần làm tiếp

1. Chạy thử trên trình duyệt với database bạn đang dùng, sau khi cấu hình DB_USERNAME/DB_PASSWORD trong IDE. Làm lần lượt đăng ký, đăng nhập, chọn sân/giờ, xem lịch sử, hủy và đặt lại; kiểm tra trên điện thoại.
2. Đã bổ sung màn hình/API admin xác nhận, hoàn thành đơn, quản lý lịch và ghi nhận thu / hoàn tiền; tiếp tục kiểm tra thao tác thực tế với nhân viên và chủ sân.
3. Đã áp dụng giữ suất 15 phút cho đơn chờ xác nhận và tác vụ tự giải phóng; tiếp tục kiểm tra luồng vận hành trong giờ cao điểm.
4. Đã bổ sung quên mật khẩu bằng OTP và giới hạn tần suất đăng nhập. Tiếp theo là refresh token, HTTPS, sao lưu / thử khôi phục và kiểm thử trên môi trường triển khai.

Xem HUONG_DAN_CHAY.md để chạy từ IDE, Docker hoặc chạy lại bộ kiểm thử. Đã có giao diện quản trị / chuyển khoản QR / hoàn tiền thủ công; vẫn cần kiểm tra giao diện và môi trường triển khai thực tế trước khi vận hành.
