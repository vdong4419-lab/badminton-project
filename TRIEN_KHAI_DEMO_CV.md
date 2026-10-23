# Phương án triển khai bản demo miễn phí cho CV

Khảo sát ngày 22/10/2026. Đây là phương án chuẩn bị; chưa tạo tài nguyên cloud hoặc có URL triển khai thật. Chính sách gói miễn phí cần kiểm tra lại khi đăng ký.

## Kiến trúc đề xuất

| Thành phần | Nơi chạy | Vai trò |
| --- | --- | --- |
| React / Vite | Vercel Hobby | Giao diện công khai với HTTPS và URL dạng `ten-du-an.vercel.app`. |
| Java 17 / Spring Boot | Render Free Web Service bằng Dockerfile hiện có | Chạy API, tác vụ nghiệp vụ và kiểm tra quyền. |
| MySQL | Aiven Free MySQL | Lưu dữ liệu demo riêng, không phụ thuộc máy cá nhân hoặc ổ đĩa container backend. |
| OTP Gmail | Đề xuất bổ sung Google Apps Script gọi qua HTTPS | Gửi từ tài khoản Google của chủ dự án mà không cần SMTP từ Render. Phần tích hợp này chưa được viết / triển khai trong đợt tư vấn này. |
| Mã nguồn | GitHub | Kết nối triển khai và trình bày README / kiến trúc / kết quả kiểm thử. |

Vercel Hobby dành cho dự án cá nhân, phi thương mại. Nếu chuyển sang vận hành sân thu tiền thực tế cần kiểm tra lại điều khoản gói và phương án hosting.

## Hạn chế cần biết

- Render Free cho backend ngủ sau 15 phút không có truy cập; lần gọi tiếp theo có thể mất khoảng một phút để khởi động. Cần thêm thông báo khởi động demo và cơ chế thử lại có giới hạn cho các request GET; không tự thử lại thao tác tạo đơn / thu tiền.
- Render Free chặn SMTP cổng 25, 465 và 587, vì vậy cấu hình Gmail SMTP hiện tại không thể dùng nguyên trạng trên gói này.
- Aiven Free MySQL hiện có 1 GB RAM / 1 GB lưu trữ, không cần thẻ và không có thời hạn cố định; dịch vụ có thể bị tắt khi không hoạt động. Cần theo dõi thông báo và bật lại nếu cần.
- Google Apps Script MailApp hiện giới hạn 100 người nhận email mỗi ngày với tài khoản Gmail cá nhân, tính theo người dùng và có thể thay đổi. Đây là phương án phù hợp demo ít người; phải triển khai và thử với chính tài khoản của chủ dự án.
- Môi trường miễn phí có giới hạn tài nguyên; chưa cam kết chạy liên tục hoặc tốc độ như hệ thống vận hành thực tế.

## Những thay đổi cần chuẩn bị trước khi triển khai

1. Cấu hình cổng backend qua biến PORT, giữ 8081 khi chạy local; giới hạn bộ nhớ JVM / số kết nối database phù hợp tài nguyên cloud.
2. Trỏ VITE_API_URL tới URL API HTTPS của Render; cấu hình CORS chính xác với domain frontend. Frontend hiện mặc định localhost nên không thể chỉ upload mà bỏ qua bước này.
3. Thêm cấu hình Vercel cho React Router để tải lại đường dẫn như /ca-nhan hoặc /lich-su không bị 404.
4. Chuẩn bị database demo mới trên Aiven, nhập schema khởi tạo một lần, cấu hình JDBC / tài khoản / TLS và kiểm tra quyền tạo các bảng bổ sung. Không tải database cá nhân đang dùng lên bản demo.
5. Giữ DB_PASSWORD, JWT_SECRET và khóa gửi mail trong biến môi trường của cloud; không đưa vào Git hoặc biến VITE_* ở trình duyệt.
6. Bổ sung bộ gửi OTP qua HTTPS nếu muốn dùng Gmail thật trên Render Free. Apps Script chạy dưới tài khoản chủ dự án, giới hạn mẫu thư OTP và bảo vệ endpoint bằng khóa bí mật; frontend không được giữ khóa này.
7. Có health check và thông báo backend đang khởi động. Không công khai thông tin kết nối / cấu hình nhạy cảm qua health check.

## Thiết kế bản demo cho HR

- Dữ liệu mẫu có sân, lịch, một số đơn đang chờ / đã xác nhận / hoàn thành, ưu đãi và đánh giá để nhìn thấy chức năng ngay.
- Tài khoản khách trải nghiệm đã được tạo sẵn; HR vẫn có thể xem sân và đánh giá khi chưa đăng nhập.
- Nếu mở dashboard cho người xem CV, chuẩn bị tài khoản / chế độ demo giới hạn thao tác và dữ liệu riêng. Không chia sẻ tài khoản admin của hệ thống thật.
- Hiển thị rõ đây là bản demo. Cần điều chỉnh màn hình thanh toán để người xem không chuyển tiền vào QR ngân hàng thật trong quá trình trải nghiệm.
- README có ảnh giao diện, sơ đồ kiến trúc, chức năng chính, phân quyền, cách chạy local, cách kiểm thử và giới hạn của bản miễn phí.
- Có video ngắn minh họa trọn luồng đặt sân / vận hành, để HR vẫn xem được khi backend đang ngủ hoặc dịch vụ cloud tạm gián đoạn.
- Link CV gồm Live Demo và Source Code; có thể thêm Video Demo. Chỉ đưa link live sau khi đã kiểm tra từ trình duyệt ẩn danh / mạng khác.

## Cách kiểm thử sau triển khai

| Nhóm | Cách kiểm tra | Kết quả cần đạt |
| --- | --- | --- |
| Trước triển khai | Chạy lại bộ test backend với MySQL riêng và frontend build / lint | Không có lỗi; bộ kiểm tra gần nhất hiện có 50 bài backend đã qua. |
| Truy cập bên ngoài | Mở link bằng trình duyệt ẩn danh, điện thoại và mạng 4G | HTTPS hoạt động, API không còn trỏ localhost, ảnh tải được. |
| React Router | Mở trực tiếp /lich-su, /ca-nhan, chi tiết sân và tải lại trang | Không lỗi 404 từ hosting, chuyển đăng nhập đúng khi cần. |
| Tài khoản | Đăng ký bằng email kiểm thử của mình, xác minh OTP, quên / đặt lại mật khẩu | Nhận thư thật, mã dùng một lần, phiên cũ bị vô hiệu, giới hạn thử vẫn hoạt động. |
| Đặt sân | Đặt, xác nhận, ghi nhận thanh toán demo, hoàn thành, đánh giá | Trạng thái, ưu đãi, thông báo và sao công khai thống nhất. |
| Tranh suất | Hai tài khoản khác nhau gửi yêu cầu cho cùng một suất gần đồng thời | Chỉ một đơn giữ được suất, yêu cầu còn lại báo xung đột. |
| Phân quyền | Khách gọi API admin / xem đơn người khác; nhân viên gọi API ngoài phạm vi | Backend từ chối; không chỉ dựa vào ẩn nút ở giao diện. |
| Lưu dữ liệu | Restart / redeploy backend rồi mở lại dữ liệu | Database còn dữ liệu và tài khoản; không phụ thuộc filesystem container. |
| Khởi động nguội | Để backend ngủ rồi truy cập lại | Có thông báo chờ / thử lại GET hợp lý; không tự tạo trùng giao dịch. |
| Tải nhẹ | Tăng dần một nhóm nhỏ người dùng trên database demo | Theo dõi thời gian phản hồi, tỷ lệ lỗi, RAM và kết nối; dừng khi chạm giới hạn gói, không test tải lên hệ thống thật. |

## Trình tự thực hiện

1. Chuẩn bị các cấu hình triển khai, hướng dẫn nhập dữ liệu demo và chế độ xem dành cho CV trong mã nguồn.
2. Chủ dự án tạo / kết nối tài khoản GitHub, Vercel, Render, Aiven và triển khai Apps Script dưới tài khoản Google của mình nếu cần OTP thật.
3. Tạo MySQL và dữ liệu demo → triển khai backend → triển khai frontend → cập nhật URL / CORS → kiểm thử bên ngoài.
4. Hoàn tất tài khoản trải nghiệm, ảnh / video / README rồi đưa URL đã kiểm tra lên CV.

Không cần mua tên miền riêng cho bản đầu tiên. Không coi bản demo miễn phí là môi trường vận hành sân thật.

## Tài liệu chính thức đã đối chiếu

- [Vercel Hobby](https://vercel.com/docs/plans/hobby)
- [Vite SPA trên Vercel](https://vercel.com/docs/frameworks/frontend/vite)
- [Giới hạn Render Free](https://render.com/docs/free)
- [Aiven MySQL Free](https://aiven.io/docs/products/mysql/concepts/mysql-free-tier)
- [Google Apps Script Web Apps](https://developers.google.com/apps-script/guides/web)
- [Giới hạn Google Apps Script](https://developers.google.com/apps-script/guides/services/quotas)
