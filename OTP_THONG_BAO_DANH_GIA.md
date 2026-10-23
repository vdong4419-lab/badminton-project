# OTP Gmail, thông báo và đánh giá sân

Cập nhật hiển thị ngày 22/10/2026: trang chủ có điểm sao trung bình, số lượt và trích bình luận mới nhất trên từng thẻ sân; bấm để xem toàn bộ bình luận. Tổng hợp chức năng và việc nên làm tiếp: [TONG_QUAN_HE_THONG.md](TONG_QUAN_HE_THONG.md).

Cập nhật 20/10/2026.

## Bật gửi email từ vdong4419@gmail.com

Backend đã cấu hình Gmail SMTP tại `smtp.gmail.com:587`, dùng STARTTLS, địa chỉ gửi `vdong4419@gmail.com`. Bạn cần cấu hình mật khẩu ứng dụng của chính tài khoản này trên máy chạy backend.

1. Đăng nhập tài khoản Google `vdong4419@gmail.com`, bật xác minh hai bước.
2. Mở [Mật khẩu ứng dụng Google](https://myaccount.google.com/apppasswords), tạo mật khẩu ứng dụng cho BadmintonHub nếu tài khoản hỗ trợ tính năng này. Xem [hướng dẫn chính thức của Google](https://support.google.com/mail/answer/185833?hl=vi).
3. Trong PowerShell tại BackEnd, chạy:

~~~powershell
cd D:\badminton-project\BackEnd
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\chay-gmail.ps1
~~~

Script yêu cầu nhập mật khẩu ứng dụng dưới dạng ẩn, chỉ truyền qua biến môi trường của tiến trình backend; không ghi vào file hoặc hiển thị mật khẩu. Cấu hình database vẫn dùng các biến môi trường / application.properties hiện có. Dừng backend đang dùng cổng 8081 trước khi chạy bản này.

Nếu chạy từ IDE, cấu hình biến môi trường Run:

~~~text
MAIL_ENABLED=true
MAIL_USERNAME=vdong4419@gmail.com
MAIL_PASSWORD=<mật khẩu ứng dụng Gmail của bạn>
~~~

Docker Compose: thêm các giá trị tương ứng vào `.env` của BackEnd; các biến đã được truyền vào service backend. Không commit `.env` hoặc chia sẻ mật khẩu trong chat. Khi chưa cấu hình Gmail, MAIL_ENABLED mặc định false: đăng ký mới / gửi lại mã cho tài khoản cần xác minh sẽ báo chưa cấu hình gửi email; ứng dụng không đưa OTP ra màn hình hoặc log. API quên mật khẩu luôn giữ thông báo chung, kể cả khi SMTP lỗi, để không tiết lộ tài khoản; máy chủ ghi cảnh báo cần kiểm tra cấu hình mail và rollback mã chưa gửi. Tài khoản đã tồn tại vẫn đăng nhập được theo quyền hiện có.

Tham khảo: [Gmail SMTP chính thức](https://developers.google.com/workspace/gmail/imap/imap-smtp), [Spring Boot gửi email](https://docs.spring.io/spring-boot/reference/io/email.html).

## Xác minh email và quên mật khẩu

- Đăng ký mới → gửi email OTP → chuyển sang `/xac-minh-email` → nhập đúng OTP → đăng nhập. Tài khoản mới chưa xác minh chưa được cấp JWT.
- Tài khoản có sẵn trước nâng cấp và tài khoản do admin chủ động tạo được giữ nguyên khả năng đăng nhập; không khóa hàng loạt tài khoản cũ.
- Đăng nhập → Quên mật khẩu → `/quen-mat-khau` → nhập email và gửi mã → nhập OTP và mật khẩu mới → đăng nhập lại. Đặt lại thành công vô hiệu các JWT cũ của tài khoản.
- OTP 6 số ngẫu nhiên, lưu BCrypt, hiệu lực 10 phút, dùng một lần, tối đa 5 lần nhập sai. Gửi lại cách ít nhất 60 giây; mã mới thay thế mã trước của cùng mục đích. OTP xác minh email và OTP đặt lại mật khẩu được lưu riêng.
- Mật khẩu mới tối thiểu 8 ký tự, tối đa 72 ký tự / 72 byte UTF-8.
- Gửi yêu cầu quên mật khẩu với email không tồn tại trả thông báo chung để không tiết lộ danh sách tài khoản.
- Nếu gửi email thất bại trong khi đăng ký, giao dịch tạo tài khoản rollback để không để lại tài khoản tạo dở.

Giới hạn được lưu trong MySQL, duy trì qua các lần khởi động: mỗi email được thử đăng nhập tối đa 5 lần thất bại trong cửa sổ 15 phút, đăng nhập thành công xóa bộ đếm email; mỗi IP tối đa 50 lượt đăng nhập trong 15 phút (kể cả thành công). Các API gửi lại OTP và quên mật khẩu dùng chung giới hạn 5 yêu cầu/email/giờ và 20 yêu cầu/IP/giờ; xác nhận mã tối đa 20 yêu cầu/email/15 phút và 50 yêu cầu/IP/15 phút; đăng ký tối đa 20 yêu cầu/IP/giờ.

Ứng dụng dùng IP kết nối thực tế do servlet cung cấp, không tin trực tiếp header X-Forwarded-For do khách tự gửi. Khi triển khai qua proxy cần cấu hình nhận diện IP đúng tại hạ tầng để tránh gộp tất cả khách vào một IP.

## Chuông thông báo

Chuông xuất hiện trên trang chủ, lịch sử, trang đặt lịch, hồ sơ và thanh toán. Badge đếm thông báo chưa đọc; danh sách tự cập nhật mỗi 30 giây. Bấm một thông báo để đánh dấu đã đọc và mở đúng đơn. Có đánh dấu tất cả đã đọc và trang `/thong-bao` với phân trang.

Các sự kiện gồm: tạo đơn chờ xác nhận, sân đã xác nhận, đã nhận tiền, hủy / tự hết hạn, đã hoàn tiền và hoàn thành buổi chơi. Nguồn thông báo là lịch sử đơn đã được commit cùng giao dịch nghiệp vụ, nên sự kiện rollback không sinh thông báo giả. Trạng thái đã đọc lưu trong database, không mất khi tải lại hoặc đăng nhập trên thiết bị khác. Khách chỉ nhận thông báo của đơn mình sở hữu.

## Đánh giá và bình luận

- Sân đã đặt → đơn Hoàn thành → Đánh giá sân & dịch vụ, hoặc mở nút đánh giá từ chi tiết thanh toán.
- Chỉ chủ đơn đã thanh toán, được hoàn thành và mọi suất đã kết thúc mới đánh giá được.
- Chọn 1–5 sao và viết bình luận 10–2000 ký tự; mỗi sân trong một đơn được đánh giá một lần. Nhiều suất của cùng sân trong đơn vẫn chỉ tạo một đánh giá cho sân đó.
- Khóa đơn và ràng buộc duy nhất trong database ngăn gửi trùng kể cả khi bấm đồng thời.
- Trang chủ → Xem thông tin & đánh giá → `/san/:sanId`: thông tin sân, điểm trung bình, số đánh giá và bình luận có phân trang. Chỉ công khai họ tên người đánh giá, nội dung, sao và thời điểm; không trả email, số điện thoại hoặc mã đơn.
- Khách chưa đủ điều kiện vẫn có thể xem các đánh giá công khai. Khi chưa được nhân viên/admin hoàn thành đơn, nút gửi đánh giá chưa mở.

## Khởi động và kiểm tra

Khởi động lại backend để tự bổ sung các bảng `xac_minh_email`, `ma_otp`, `gioi_han_yeu_cau`, `thong_bao_da_doc`, `danh_gia_san` qua `db/operations.sql`. Không nhập lại `001-init.sql` trên database có dữ liệu.

Giao diện tiếp tục chạy bằng `npm.cmd run dev` trong frontend. Tính năng gửi mail cần mật khẩu ứng dụng Gmail; kiểm thử tự động dùng mail giả lập, không gửi thư thật tới khách.

Các API công khai mới: POST `/api/xac-thuc/gui-lai-otp`, `/xac-minh-email`, `/quen-mat-khau`, `/dat-lai-mat-khau`; GET `/api/san/{id}/danh-gia`. Các API thông báo và đánh giá theo đơn yêu cầu đăng nhập, kiểm tra quyền sở hữu tại backend.

## Kết quả kiểm tra ngày 20/10/2026

- Backend: 49/49 bài qua, không bỏ qua: 41 bài HTTP / MySQL, 3 bài chính sách, 3 bài gửi mail giả lập và 2 bài JWT. MySQL dùng container kiểm thử riêng, không dùng database thật.
- Đã kiểm tra tài khoản chưa xác minh không được đăng nhập; OTP băm, hết hạn, hết lượt sai, gửi lại và chỉ dùng một lần; đặt lại mật khẩu đồng thời chỉ thành công một lần và thu hồi phiên cũ.
- Đã kiểm tra giới hạn đăng nhập theo email chuẩn hóa, hết cửa sổ 15 phút; gửi email thất bại rollback đăng ký và không tiết lộ tài khoản qua API quên mật khẩu.
- Đã kiểm tra thông báo của đúng chủ đơn, lưu trạng thái đã đọc, từ chối đánh dấu thông báo của người khác.
- Đã kiểm tra điều kiện đánh giá, sân thuộc đơn, số sao, quyền sở hữu, chống hai lượt đánh giá đồng thời và dữ liệu công khai không có email / mã đơn.
- Frontend build và lint thành công, không còn cảnh báo lint. Script chạy Gmail đã kiểm tra cú pháp PowerShell.
- Log: `BackEnd/otp-thong-bao-tests.log`. Gửi thư Gmail thật chưa kiểm tra do chưa cấu hình mật khẩu ứng dụng; tương tác giao diện chưa kiểm thử bằng trình duyệt trong phiên này.
