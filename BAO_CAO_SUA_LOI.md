# Báo cáo sửa lỗi — 17/10/2026

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
2. Xây dựng màn hình/API admin xác nhận, hoàn thành đơn, quản lý lịch và ghi nhận thanh toán thủ công. Thiết kế hủy/hoàn tiền cho đơn đã thanh toán.
3. Chốt chính sách giữ suất cho CHO_XAC_NHAN: hiện đơn giữ suất đến khi được hủy hoặc xử lý tiếp; nếu cần tự hết hạn, phải bổ sung thời điểm hết hạn và tác vụ giải phóng có transaction.
4. Thêm refresh token, quên mật khẩu và giới hạn tần suất đăng nhập; sau đó cấu hình HTTPS, backup và kiểm thử trên môi trường triển khai.

Xem HUONG_DAN_CHAY.md để chạy từ IDE, Docker hoặc chạy lại bộ kiểm thử. Chưa có giao diện quản trị/thanh toán/hoàn tiền và chưa coi đây là bản đã xác minh đầy đủ để vận hành thực tế.