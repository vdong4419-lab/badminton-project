# Trang cá nhân, ưu đãi khách quen và chuyển khoản QR

Tính năng bổ sung ngày 20/10/2026: xem [OTP_THONG_BAO_DANH_GIA.md](OTP_THONG_BAO_DANH_GIA.md) để bật gửi OTP Gmail, quên mật khẩu, thông báo và đánh giá sân.

Cập nhật ngày 18/10/2026. Các thay đổi nằm trong ứng dụng React / Spring Boot hiện có.

## Trang cá nhân

Mở `/ca-nhan` từ tên người dùng trên trang chủ, mục Trang cá nhân hoặc tên người dùng trong dashboard.

- Xem họ tên, email, số điện thoại, ngày tham gia.
- Sửa họ tên / số điện thoại của chính mình. Email và vai trò không thay đổi qua API hồ sơ.
- Đổi mật khẩu sau khi nhập đúng mật khẩu hiện tại. Mật khẩu mới có 8–72 ký tự và không vượt 72 byte UTF-8; không dùng lại mật khẩu cũ.
- Đổi mật khẩu thành công sẽ vô hiệu các JWT đã phát hành trước đó của tài khoản và yêu cầu đăng nhập lại.
- Xem số đơn đã hoàn thành, chi tiêu thực tế cho những đơn này và tiến độ quyền lợi thành viên.

## Chính sách ưu đãi đã chốt

| Số đơn đã thanh toán và hoàn thành | Hạng | Giảm cho đơn mới |
| --- | --- | --- |
| 0–4 | Thành viên | 0% |
| 5–9 | Khách quen | 5% |
| Từ 10 | Thân thiết | 10% |

Mức giảm tối đa 50.000đ mỗi đơn, làm tròn xuống đến đồng. Đơn đang chờ, chưa thu tiền, đã hủy hoặc đã hoàn tiền không được tính. Chỉ tài khoản CUSTOMER hưởng ưu đãi.

Backend tính báo giá và số tiền cuối cùng từ giá lịch trong database; frontend không quyết định mức giảm. Trang đặt sân hiển thị tiền sân, ưu đãi và tổng phải trả trước khi tạo đơn. Báo giá không giữ suất; giao dịch tạo đơn kiểm tra lại giá và tình trạng suất.

Ưu đãi cũng áp dụng khi admin / nhân viên đặt tại quầy cho khách. Mức giảm được lưu theo từng đơn, giữ nguyên khi khách lên hạng hoặc giá sân thay đổi. Đơn cũ trước tính năng này giữ nguyên tổng tiền, không được giảm hồi tố. Tiền thu và hoàn tiền dùng số tiền sau giảm đã ghi nhận trên đơn.

Ví dụ: khách đã hoàn thành 5 đơn, đặt tiền sân 100.000đ thì được giảm 5.000đ, trả 95.000đ. Nếu đã thu rồi hủy hợp lệ, admin hoàn đúng 95.000đ.

## Chuyển khoản bằng ảnh QR

Ảnh gốc người dùng cung cấp được lưu nguyên bản tại `frontend/public/images/thanh-toan-mb.jpg`, không vẽ lại QR. Thông tin hiển thị: MB, PHAN VAN DONG, số tài khoản 0388465777.

1. Khách tạo đơn, được chuyển sang `/thanh-toan/:donId`. Có thể mở lại từ Sân đã đặt → Xem đơn & thanh toán.
2. Khi đơn chờ xác nhận, vẫn áp dụng hạn giữ suất 15 phút. Trang thanh toán hiển thị hạn chờ; chưa mở QR chuyển khoản.
3. Khi admin / nhân viên có quyền xác nhận đơn, nếu chưa thu tiền và chưa tới giờ chơi, trang hiển thị QR, số tiền sau giảm và nội dung `DS` + ID đơn (ví dụ `DS123`). Khách có thể tải ảnh QR hoặc sao chép thông tin.
4. QR là ảnh tĩnh: khách nhập / kiểm tra đúng số tiền và nội dung đơn trong ứng dụng ngân hàng trước khi chuyển.
5. Khách bấm Tôi đã chuyển khoản sau khi thực sự chuyển. Thông báo được ghi vào lịch sử đơn và hiển thị ở dashboard. Gửi lại hoặc gửi đồng thời không tạo nhiều sự kiện cho cùng đơn.
6. Admin / nhân viên đối chiếu tài khoản ngân hàng, đúng số tiền và nội dung, rồi ghi nhận thu tiền bằng phương thức Chuyển khoản và mã giao dịch ngân hàng thực tế. Nội dung DS123 là mã đối chiếu đơn; mã giao dịch ngân hàng là mã của khoản tiền đã nhận.
7. Trang thanh toán tự tải trạng thái mỗi 10 giây. Sau khi thu tiền, hủy đơn hoặc tới giờ chơi, trang không hiển thị QR để chuyển tiếp.

Đây là luồng chuyển khoản thủ công có đối chiếu. Không có webhook ngân hàng hoặc tự động xác nhận tiền; nút Tôi đã chuyển khoản chỉ tạo thông báo. Khách không được tự đặt trạng thái đã thanh toán.

## Phân quyền

| Vai trò | Quyền |
| --- | --- |
| Chưa đăng nhập | Không truy cập hồ sơ, chi tiết đơn, báo giá hoặc báo chuyển khoản. |
| CUSTOMER | Xem / sửa hồ sơ của mình; xem / báo chuyển khoản đơn của mình; hưởng ưu đãi theo lịch sử hợp lệ. |
| STAFF | Sửa hồ sơ của mình; xử lý đơn trong phạm vi ngày hiện tại theo quy định vận hành sẵn có; xác nhận nhận tiền sau đối chiếu; không hoàn tiền hoặc thay đổi quyền người dùng. |
| ADMIN | Xác nhận đơn, thu tiền, xử lý hủy / hoàn tiền theo chính sách hiện có; quản lý tài khoản. |

API hồ sơ không nhận ID người khác; API chi tiết đơn và báo chuyển khoản kiểm tra chủ đơn ở backend. UI vẫn dùng quyền do backend trả về.

## Khởi động và kiểm tra

Khởi động lại backend để tự bổ sung bảng `uu_dai_dat_san` và `phien_ban_mat_khau` qua `db/operations.sql`. Tài khoản MySQL cần quyền CREATE trong database dự án. Không chạy lại `001-init.sql` trên database đang có dữ liệu.

~~~powershell
cd D:\badminton-project\BackEnd
.\mvnw.cmd spring-boot:run
~~~

Trong terminal khác:

~~~powershell
cd D:\badminton-project\frontend
npm.cmd run dev
~~~

Nếu còn backend Docker cùng cổng 8081, dừng riêng container `badminton_backend` trước khi chạy Maven như hướng dẫn trong HUONG_DAN_CHAY.md.

Kiểm tra giao diện trên máy: đăng nhập → mở Trang cá nhân → sửa thông tin → đặt sân → xác nhận đơn bằng admin → xem QR đúng ảnh / đúng tiền / đúng nội dung → báo chuyển khoản → kiểm tra thông báo ở dashboard. Khi kiểm tra giao diện không cần thực hiện chuyển tiền thật; chỉ ghi nhận đã thu tiền khi đối chiếu giao dịch thực tế.

## API bổ sung

| API | Mục đích |
| --- | --- |
| GET /api/ho-so | Hồ sơ và quyền lợi tài khoản hiện tại |
| PUT /api/ho-so | Sửa họ tên, số điện thoại |
| PUT /api/ho-so/mat-khau | Đổi mật khẩu và thu hồi phiên cũ |
| POST /api/dat-san/bao-gia | Tính giá theo danhSachLichSanIds, không giữ suất |
| GET /api/dat-san/{id} | Xem đơn của mình, ưu đãi đã áp dụng và trạng thái chuyển khoản |
| POST /api/dat-san/{id}/bao-chuyen-khoan | Gửi thông báo đã chuyển khoản |

Kiểm thử tích hợp dùng script `BackEnd/kiem-thu-mysql.ps1` với MySQL tạm riêng. Lượt kiểm tra ngày 18/10/2026: 38/38 bài qua, không lỗi và không bỏ qua (33 bài HTTP / MySQL, 3 bài chính sách và 2 bài JWT). Có kiểm tra sửa hồ sơ không đổi email / vai trò, thu hồi token, đổi mật khẩu đồng thời, mốc 5 / 10 đơn, giới hạn giảm giá, giá chụp, hoàn đúng tiền thực thu, đặt tại quầy và báo chuyển khoản đồng thời không ghi trùng / không tự thu tiền.

Kết quả nằm ở `BackEnd/khach-hang-tests.log`. Frontend build và lint thành công. Chưa kiểm thử tương tác bằng trình duyệt trong phiên làm việc này.
