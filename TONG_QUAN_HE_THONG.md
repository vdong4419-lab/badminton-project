# Tổng quan hệ thống và thứ tự nâng cấp

Cập nhật 22/10/2026. Phân biệt phần đã có trong mã nguồn với phần đã kiểm tra trên môi trường thực tế.

## Chức năng đã xây dựng

| Nhóm | Hiện có |
| --- | --- |
| Khách hàng | Đăng ký, đăng nhập, xác minh email OTP cho tài khoản mới, quên mật khẩu OTP, trang cá nhân, sửa thông tin và đổi mật khẩu. |
| Bảo vệ tài khoản | BCrypt, JWT, giới hạn thử đăng nhập và gửi / nhập OTP, thu hồi phiên cũ sau khi đổi hoặc đặt lại mật khẩu; kiểm tra quyền ở backend. |
| Sân và lịch | Danh sách, tìm theo tên / mô tả trên trang chủ, chi tiết sân, lịch trống theo ngày, đặt nhiều suất, lưu giá theo đơn và chống đặt trùng. |
| Đơn đặt | Giữ suất chờ xác nhận 15 phút, tự hết hạn, xác nhận, hủy, check-in và hoàn thành theo điều kiện nghiệp vụ. |
| Thanh toán | QR MB do chủ sân cung cấp, số tiền và nội dung theo đơn, báo đã chuyển khoản, nhân viên / admin đối chiếu và xác nhận thu tiền. |
| Hoàn tiền | Hủy đơn hợp lệ đã thu tiền sẽ chờ admin hoàn đủ số tiền thực thu sau giảm giá; lưu lịch sử xử lý. |
| Khách quen | Từ 5 đơn hợp lệ giảm 5%, từ 10 đơn giảm 10%, tối đa 50.000đ/đơn; hiển thị hạng và tiến độ; áp dụng cả đặt tại quầy. |
| Thông báo | Chuông có số chưa đọc, các sự kiện trạng thái đặt sân / thu / hoàn tiền, mở đơn liên quan, đánh dấu đã đọc và phân trang. |
| Đánh giá công khai | Khách đã chơi xong, thanh toán và hoàn thành đơn được đánh giá 1–5 sao / viết bình luận; mỗi sân trong đơn một lần. |
| Dashboard | Tổng quan, sân, lịch, đơn, đặt tại quầy, tài khoản / nhân viên, nhật ký thao tác, khoản thu của nhân viên. |
| Báo cáo | Tiền thu, tiền hoàn, thu ròng theo kỳ, biểu đồ và xuất CSV. Thu ròng chưa trừ chi phí vận hành. |

## Đánh giá hiển thị trên sân

- Ngay dưới tên sân ở trang chủ: 5 ngôi sao có thể hiện phần lẻ, điểm trung bình theo thang 5 và số lượt đánh giá.
- Mỗi thẻ có trích bình luận mới nhất, tên khách và số sao của bình luận đó. Đây là bình luận mới nhất, không phải chọn riêng đánh giá tốt.
- Bấm điểm sao hoặc Đọc bình luận & thông tin sân sẽ mở `/san/:id#danh-gia`, cuộn tới danh sách bình luận đầy đủ có phân trang.
- Sân chưa có đánh giá hiển thị Chưa có đánh giá; không gán sẵn điểm tốt hoặc điểm 0/5.
- Trang chủ cập nhật mỗi 30 giây và khi quay lại tab. Số liệu tổng hợp từ các đánh giá lưu trong MySQL; một đơn có nhiều suất cùng sân vẫn chỉ tạo một đánh giá cho sân đó.
- API danh sách sân lấy điểm / số lượt / bình luận theo nhóm sân, tránh truy vấn riêng mỗi thẻ. Chỉ công khai tên khách và nội dung đánh giá; không trả email, điện thoại hoặc mã đơn.
- Chỉ sân đang hoạt động xuất hiện trong danh sách công khai. Không cần thay đổi cấu trúc database cho đợt hiển thị này.

## Những phần cần hoàn tất trước khi dùng thực tế

1. **Gmail thật:** mã nguồn và cấu hình SMTP đã có; cần nhập mật khẩu ứng dụng của `vdong4419@gmail.com`, thử gửi / nhận OTP thật, kiểm tra thư rác. Lượt kiểm thử tự động dùng mail giả lập, không xác nhận việc Gmail đã gửi thành công. Xem OTP_THONG_BAO_DANH_GIA.md.
2. **Kiểm tra giao diện và quy trình:** chạy trên máy tính / điện thoại bằng tài khoản CUSTOMER, STAFF và ADMIN; thử từ đăng ký → xác minh → đặt → xác nhận → chuyển khoản → thu tiền → hoàn thành → đánh giá. Thử riêng hủy / hết hạn / hoàn tiền.
3. **Triển khai:** tên miền, HTTPS, khóa JWT cố định, tài khoản database phù hợp, sao lưu và thử khôi phục, ghi nhận / theo dõi lỗi, quy trình cập nhật cấu trúc database có phiên bản. Hiện thay đổi bảng bổ sung được chạy khi khởi động qua operations.sql.

## Chức năng nên nâng cấp tiếp

| Thứ tự | Chức năng | Lý do |
| --- | --- | --- |
| 1 | Admin phản hồi đánh giá, khách báo cáo bình luận không phù hợp, admin ẩn / khôi phục có lý do và nhật ký | Khi đã công khai bình luận, chủ sân cần phản hồi và xử lý nội dung vi phạm minh bạch; không tự động che đánh giá thấp. |
| 2 | Đổi lịch cho đơn còn hợp lệ | Khách đổi ngày / giờ theo chính sách; chỉ giải phóng lịch cũ khi giữ được đủ lịch mới, tính rõ chênh lệch tiền. |
| 3 | Nhắc lịch trước giờ chơi | Giảm quên lịch; bổ sung thông báo trong website, sau đó email khi SMTP đã hoạt động ổn định. |
| 4 | Dashboard cấu hình chương trình khuyến mãi / voucher | Hiện ưu đãi khách quen theo mức cố định; cần ngày hiệu lực, điều kiện và giới hạn lượt sử dụng để chủ sân tự quản lý. |
| 5 | Ca làm việc, bàn giao tiền và chi phí | Khoản thu của nhân viên hiện chưa phải ca mở / đóng có đối soát tiền mặt. Cần ghi chi phí để tính kết quả kinh doanh. |
| 6 | Đặt lịch cố định hằng tuần | Phù hợp nhóm khách quen; cần quy định suất trùng, thanh toán và hủy từng buổi. |
| 7 | Album sân, thông tin tiện ích / địa chỉ / nội quy và tải ảnh lên | Hiện ảnh sân được cấu hình bằng URL. Nội dung thật giúp khách chọn sân và tránh thông tin giới thiệu không khớp thực tế. |

Thanh toán QR thủ công có thể tiếp tục dùng theo lựa chọn của chủ sân. Nếu sau này cần tự xác nhận tiền, phải tích hợp nguồn giao dịch / nhà cung cấp có xác thực và đối soát; ảnh QR tĩnh không cung cấp xác nhận đó.

Đợt tiếp theo đề xuất tập trung vào phản hồi / quản lý đánh giá và đổi lịch; các bước kiểm tra Gmail, giao diện và chuẩn bị triển khai cần hoàn tất trước khi đưa vào vận hành thực tế.

## Kết quả kiểm tra đợt hiển thị đánh giá

- Ngày 22/10/2026: 50/50 bài backend qua, không lỗi hoặc bỏ qua (42 bài HTTP / MySQL và 8 bài đơn vị). MySQL dùng container riêng.
- Kiểm tra thêm: sân chưa có đánh giá, tổng hợp nhiều lượt, lấy đúng bình luận mới nhất, dữ liệu nhất quán giữa danh sách / chi tiết / tìm theo tên, ẩn sân tạm ngừng và không trả dữ liệu riêng của khách qua thẻ sân công khai.
- Frontend build và lint thành công. Chưa kiểm thử tương tác bằng trình duyệt trong phiên này.
- Log: `BackEnd/danh-gia-cong-khai-tests.log`.
- Khởi động lại backend và tải lại frontend để dùng API / giao diện mới. Không cần nhập lại SQL hoặc tạo bảng mới cho đợt này.
