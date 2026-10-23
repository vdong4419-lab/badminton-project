# Quản trị đơn, thanh toán, hoàn tiền và giữ suất

Cập nhật 18/10/2026: dashboard, vai trò STAFF, quản lý sân/lịch, tài khoản và báo cáo đã được mở rộng. Xem DASHBOARD_VAN_HANH.md để biết phân quyền và quy trình hiện tại.

Đã bổ sung chức năng trên cấu trúc database trong csdl.txt hiện có. Khởi động lại backend và frontend để sử dụng.

## Sử dụng

- Đăng nhập bằng tài khoản có vai trò ADMIN: ứng dụng mở /quan-tri. Từ trang chủ hoặc lịch sử cũng có liên kết Quản trị đơn dành cho admin.
- Bộ lọc gồm chờ xác nhận, đã xác nhận, chờ hoàn tiền, hoàn thành và đã hủy. Danh sách phân trang 20 đơn; chọn đơn để xem khách hàng, suất chơi và lịch sử.
- Chỉ các thao tác phù hợp với trạng thái hiện tại được hiển thị. API vẫn kiểm tra lại quyền, thời hạn và trạng thái khi thực hiện.

## Phân quyền

| Thao tác | CUSTOMER | ADMIN |
| --- | --- | --- |
| Xem sân/lịch trống | Có | Có |
| Đặt sân và xem lịch sử của mình | Có | Có |
| Hủy đơn của mình trước giờ chơi | Có | Có |
| Xem tất cả đơn và thông tin liên hệ khách | Không | Có |
| Xác nhận đơn, ghi nhận thu tiền | Không | Có |
| Hủy đơn của khách trước giờ chơi | Không | Có |
| Xác nhận đã hoàn tiền, hoàn thành đơn | Không | Có |
| Tự chọn vai trò khi đăng ký | Không | Không |

Phân quyền được áp dụng tại SecurityFilterChain cho /api/quan-tri/** và tại lớp dịch vụ quản trị bằng @PreAuthorize. Quyền hiện tại được đọc từ database trên mỗi yêu cầu có JWT. Frontend kiểm tra phiên qua GET /api/xac-thuc/toi; thay đổi giao diện hoặc localStorage không cấp được quyền tại backend.

Nếu chưa có admin: đăng ký tài khoản bình thường rồi dùng tài khoản quản trị database chạy BackEnd/database/gan-quyen-admin.sql, thay email trong file bằng đúng email cần cấp quyền. Sau đó đăng nhập lại. File này không tự chạy khi khởi động.

## Chính sách 15 phút

- Đơn mới ở CHO_XAC_NHAN, giữ suất tối đa 15 phút từ ngay_tao, hoặc đến giờ bắt đầu suất đầu tiên nếu giờ đó đến trước.
- Admin xác nhận khi đơn còn hạn và chưa bắt đầu: chuyển DA_XAC_NHAN, giữ lịch chính thức và bỏ thời hạn giữ suất tạm.
- Đơn chờ chưa thanh toán hết hạn: hệ thống chuyển DA_HUY, ghi lý do, thời điểm và lịch sử; giải phóng toàn bộ giữ chỗ trong cùng transaction. Chi tiết và giá đã đặt vẫn còn trong lịch sử.
- Tác vụ chạy khi ứng dụng sẵn sàng và khoảng mỗi 30 giây; các API xem lịch, đặt sân, xem lịch sử và xử lý đơn cũng dọn đơn hết hạn trước khi làm việc.
- Đơn đã xác nhận hoặc có khoản đã thu không bị tự hủy do quá 15 phút. Bản ghi cũ có tiền đã thu cần được admin xử lý, không tự giải phóng như một đơn chờ chưa thu tiền.
- BOOKING_HOLD_MINUTES mặc định 15 (cho phép 1–120). BOOKING_EXPIRY_SCAN_MS mặc định 30000. Thời hạn được tính từ ngày tạo theo cấu hình đang chạy; đổi cấu hình sẽ áp dụng cả cho đơn chờ hiện có. Thời gian nghiệp vụ là Asia/Ho_Chi_Minh.

Các đơn CHO_XAC_NHAN cũ đã quá 15 phút cũng sẽ được xử lý hết hạn khi chạy bản mới.

## Thu tiền và hoàn tiền

1. Admin xác nhận đơn còn hạn.
2. Sau khi nhận đủ tiền thực tế, chọn Ghi nhận thu tiền. Hệ thống ghi đúng tổng tiền đơn; phương thức TIEN_MAT hoặc CHUYEN_KHOAN. Chuyển khoản phải có mã giao dịch. Client không quyết định số tiền.
3. Nếu khách hoặc admin hủy trước giờ chơi, suất được giải phóng. Nếu đã thu tiền, đơn xuất hiện trong Chờ hoàn tiền; trạng thái thanh toán vẫn DA_THANH_TOAN đến khi tiền thực sự được trả lại.
4. Admin hoàn đủ số tiền đã thu cho khách bên ngoài ứng dụng, rồi chọn Xác nhận đã hoàn tiền, điền mã giao dịch/biên nhận, ghi chú và xác nhận đã đối chiếu. Hệ thống chuyển DA_HOAN_TIEN, ghi thời điểm và người thao tác trong lịch sử. Mã thu tiền ban đầu được giữ lại; biên nhận hoàn được lưu trong lịch sử xử lý.
5. Đơn đã sử dụng xong: admin chỉ được đánh dấu hoàn thành khi mọi suất đã kết thúc và đơn đã thu tiền.

Thu/hoàn tiền là ghi nhận thủ công. Ứng dụng chưa gửi tiền qua ngân hàng hoặc kết nối cổng thanh toán. Phiên bản này hoàn toàn bộ số tiền đã thu cho một đơn đã hủy; chưa có hoàn một phần, hủy từng suất hoặc hủy sau khi bắt đầu chơi.

Đơn hàng và thanh toán có trạng thái riêng: DA_HUY không có nghĩa là đã hoàn tiền. Không gộp đơn chờ hoàn với đơn đã hoàn khi đối soát tiền.

## API quản trị

| Method | Đường dẫn | Công dụng |
| --- | --- | --- |
| GET | /api/quan-tri/don-dat | Phân trang, lọc trangThai, choHoanTien, page |
| GET | /api/quan-tri/don-dat/{id} | Chi tiết khách/đơn và lịch sử |
| PUT | /api/quan-tri/don-dat/{id}/xac-nhan | Xác nhận còn hạn |
| POST | /api/quan-tri/don-dat/{id}/thanh-toan | phuongThuc, maThamChieu |
| PUT | /api/quan-tri/don-dat/{id}/huy | lyDoHuy bắt buộc |
| POST | /api/quan-tri/don-dat/{id}/hoan-tien | maBienNhan, ghiChu bắt buộc |
| PUT | /api/quan-tri/don-dat/{id}/hoan-thanh | Hoàn thành đơn đã chơi và thu tiền |

Khách vẫn dùng PUT /api/dat-san/{id}/huy và chỉ được tác động đơn thuộc mình. Nếu đã thu tiền, thao tác này tạo nghĩa vụ chờ hoàn tiền.

Các thao tác tài chính khóa đơn và bản ghi thanh toán trong transaction; yêu cầu lặp/sai trạng thái nhận 409. Thu, hủy, hoàn và xử lý hết hạn đều ghi người thao tác cùng thời điểm. Hệ thống tự xử lý hết hạn có nguoi_thuc_hien_id NULL.

## Kiểm tra

Chạy BackEnd/kiem-thu-mysql.ps1 để kiểm thử bằng container MySQL riêng. Frontend kiểm tra bằng npm run build và npm run lint. Kết quả kiểm thử của phiên triển khai này được ghi trong phần báo cáo dưới đây.
## Báo cáo hoàn thành — 17/10/2026

- Backend: 22/22 bài kiểm thử thành công, gồm 17 bài tích hợp API/MySQL, 3 bài kiểm tra chính sách với đồng hồ cố định và 2 bài JWT. Không bỏ qua bài nào trong lượt kiểm thử đầy đủ.
- Đã kiểm tra từ chối quyền CUSTOMER/khách chưa đăng nhập với toàn bộ API quản trị, không tự nâng quyền qua đăng ký, thu đúng tổng tiền phía server và giữ lịch sử người thao tác.
- Đã kiểm tra thu tiền đồng thời và hoàn tiền đồng thời: mỗi thao tác chỉ có một lần thành công; yêu cầu lặp nhận 409.
- Đã kiểm tra hủy đồng thời với thu tiền, xác nhận đồng thời với hết hạn, tự hủy đúng mốc 15 phút, rút ngắn hạn theo giờ chơi, không làm hết hạn đơn đã xác nhận hoặc có tiền đã thu, và rollback khi ghi lịch sử hết hạn bị lỗi.
- Frontend: npm run build và npm run lint thành công, không còn cảnh báo lint.
- MySQL kiểm thử chạy trong container riêng do script tạo và dọn. Chưa chạy thử trang quản trị trực tiếp bằng trình duyệt trên môi trường database đang sử dụng.

Các dòng duplicate/injected failure trong log test được chủ động gây ra để kiểm tra tranh chấp và rollback, không phải bài kiểm thử thất bại.
