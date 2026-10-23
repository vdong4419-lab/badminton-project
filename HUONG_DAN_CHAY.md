# Chạy hệ thống đặt sân

OTP Gmail, quên mật khẩu, chuông thông báo và đánh giá: xem [OTP_THONG_BAO_DANH_GIA.md](OTP_THONG_BAO_DANH_GIA.md). Gửi OTP thật cần cấu hình Gmail; có script `BackEnd/chay-gmail.ps1` để nhập mật khẩu ứng dụng dưới dạng ẩn khi chạy backend.

Trang cá nhân, chính sách ưu đãi khách quen và hướng dẫn chuyển khoản bằng QR: xem [KHACH_HANG_UU_DAI_QR.md](KHACH_HANG_UU_DAI_QR.md). Khởi động lại backend sau khi cập nhật để tự tạo các bảng bổ sung, không cần nhập lại database.

Backend cần Java 17, MySQL 8.0.16 trở lên (đã kiểm tra bằng image mysql:8.0). Frontend dùng Node.js phù hợp với Vite trong package.json.

## Chạy từ IDE với database hiện có

Nếu database đã được tạo bằng csdl.txt, giữ nguyên database đó; không chạy lại script khởi tạo trên database đã có bảng.

Đặt các biến môi trường cho cấu hình Run của IDE:

- DB_URL: jdbc:mysql://127.0.0.1:3306/dat_san_cau_long?connectionTimeZone=Asia/Ho_Chi_Minh
- DB_USERNAME và DB_PASSWORD: tài khoản MySQL thực tế của bạn.
- JWT_SECRET: Base64 của ít nhất 64 byte ngẫu nhiên.
- CORS_ALLOWED_ORIGINS: mặc định http://localhost:5173,http://127.0.0.1:5173.

Nếu kết nối MySQL được chạy qua Compose trong dự án, dùng cổng host 3307 thay vì 3306.

Tạo khóa JWT trong PowerShell:

~~~powershell
$bytes = New-Object byte[] 64
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
[Convert]::ToBase64String($bytes)
~~~

Chạy DatSanApplication trong IDE, hoặc từ thư mục BackEnd:

~~~powershell
$env:DB_USERNAME = 'tai_khoan_mysql'
$env:DB_PASSWORD = 'mat_khau_mysql_cua_ban'
# Gán JWT_SECRET bằng khóa vừa tạo nếu muốn token còn hiệu lực qua các lần khởi động.
.\mvnw.cmd spring-boot:run
~~~

Khi phát triển mà chưa đặt JWT_SECRET, backend tự sinh khóa an toàn trong bộ nhớ; token sẽ mất hiệu lực sau khi khởi động lại. Khi triển khai, cấu hình khóa cố định qua biến môi trường.

Frontend, trong terminal riêng tại thư mục frontend:

~~~powershell
npm install
npm run dev
~~~

Có thể tạo frontend/.env.local với VITE_API_URL=http://localhost:8081/api. Khi đưa lên máy chủ, đổi URL API và CORS sang địa chỉ thực tế. Vite đọc biến môi trường khi khởi động/build.

## Khi Maven Wrapper hoặc cổng 8081 báo lỗi

Trong PowerShell, chạy lệnh với tiền tố `.\` từ đúng thư mục:

~~~powershell
cd D:\badminton-project\BackEnd
.\mvnw.cmd -v
.\mvnw.cmd spring-boot:run
~~~

Lỗi `Cannot index into a null array` / `Cannot start maven from wrapper` do wrapper đọc `Target[0]` khi thư mục `.m2` không phải liên kết đã được sửa trong `BackEnd/mvnw.cmd`. Lệnh `-v` phải hiển thị Maven và Java 17 trước khi chạy ứng dụng.

Nếu đã chạy backend bằng Docker, dừng riêng container backend trước khi chạy bằng Maven để giải phóng cổng 8081:

~~~powershell
docker stop badminton_backend
.\mvnw.cmd spring-boot:run
~~~

MySQL cài trên máy dùng cổng 3306; MySQL của Compose dùng cổng 3307. Đây có thể là hai database có dữ liệu khác nhau. Chọn đúng địa chỉ bằng `DB_URL` và tài khoản bằng `DB_USERNAME` / `DB_PASSWORD` trong terminal chạy Maven. Cấu hình mặc định của dự án trỏ tới MySQL trên máy ở cổng 3306.

Nếu log báo `Schema-validation: missing table [dat_san]`, database đang kết nối thiếu schema hoặc ứng dụng đang trỏ nhầm database. Kiểm tra `SELECT DATABASE(); SHOW TABLES;` trong MySQL. Chỉ nhập `BackEnd/database/001-init.sql` vào database mới dành cho dự án, chưa có bảng; không chạy lại script trên database đang có dữ liệu. Không đổi `ddl-auto=validate` để che lỗi thiếu bảng.

Backend sẵn sàng khi log có `Tomcat started on port 8081` và `Started DatSanApplication`. Giữ terminal chạy; dùng Ctrl+C để dừng.

## Chạy Docker Compose

Trong BackEnd, sao chép .env.example thành .env và điền MYSQL_ROOT_PASSWORD, DB_PASSWORD, JWT_SECRET. Compose tự đọc .env; chạy Spring Boot từ IDE không tự đọc file này.

~~~powershell
docker compose up --build -d
~~~

Database mới sẽ tự nhập database/001-init.sql. Backend đợi MySQL sẵn sàng, dùng tài khoản dat_san_app và giữ ddl-auto=validate.

Với volume MySQL cũ: biến MYSQL_USER/MYSQL_PASSWORD chỉ tạo tài khoản ở lần khởi tạo đầu tiên. Hãy dùng tài khoản đã có qua DB_USERNAME/DB_PASSWORD hoặc tạo tài khoản ứng dụng trong MySQL. Không xóa volume để khắc phục lỗi kết nối. Backend sẽ bổ sung lịch 7 ngày khi chạy và lúc 00:05 theo giờ Việt Nam, giữ nguyên giá tùy chỉnh và trạng thái đóng đặt.

## Kiểm thử

Chạy các test JWT không cần database:

~~~powershell
.\mvnw.cmd test
~~~

Các test tích hợp mặc định được bỏ qua để không chạm database đang dùng. Để kiểm tra toàn bộ với Docker đang chạy:

~~~powershell
.\kiem-thu-mysql.ps1
~~~

Nếu Windows báo đang chặn chạy file .ps1, có thể chạy riêng tiến trình kiểm thử (không đổi chính sách toàn máy):

~~~powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\kiem-thu-mysql.ps1
~~~

Script tạo container riêng trên cổng 3308, nhập schema vào dat_san_kiem_thu, chạy test, rồi xóa đúng container vừa tạo. Dùng -Port để đổi cổng hoặc -MavenCommand để chỉ định Maven có sẵn. Lần đầu có thể cần tải image và thư viện.

Bộ test kiểm tra đăng ký/đăng nhập, quyền admin, tài khoản bị khóa, bảo vệ dữ liệu mật khẩu, đặt nhiều suất, hai yêu cầu tranh cùng suất, rollback khi lỗi database, hủy đồng thời, từ chối hủy đơn người khác/đã chơi/đã hoàn thành/đã thanh toán và giữ nguyên giá lịch sử.

Frontend:

~~~powershell
npm run build
npm run lint
~~~

## Quản trị và chính sách giữ suất

Dashboard vận hành mới: xem DASHBOARD_VAN_HANH.md ở thư mục gốc. Khi khởi động, backend bổ sung vai trò STAFF và bảng nhật ký quản trị qua db/operations.sql. Admin có thể tạo tài khoản nhân viên ngay trong dashboard; tài khoản MySQL ứng dụng cần quyền tạo bảng trong database dự án.

- Đơn mới ở CHO_XAC_NHAN và giữ suất tối đa 15 phút, không vượt giờ bắt đầu suất đầu tiên. Quá hạn tự hủy và giải phóng suất; admin xác nhận thì giữ lịch chính thức.
- Khách chỉ hủy đơn của mình trước khi bắt đầu, đang chờ còn hạn hoặc đã xác nhận. Nếu đã thu tiền, đơn chuyển sang chờ admin hoàn đủ tiền đã thu.
- Admin đăng nhập tại /dang-nhap, vào /quan-tri để xác nhận, thu tiền, hủy, hoàn tiền và hoàn thành đơn. Có bộ lọc chờ hoàn tiền và lịch sử xử lý.
- Đăng ký luôn tạo CUSTOMER. Muốn có ADMIN, quản trị database gán vai trò ADMIN cho đúng tài khoản đã đăng ký; API đăng ký không nhận quyền từ client.
- Chi tiết phân quyền, quy trình và API: xem QUAN_TRI_THANH_TOAN.md ở thư mục gốc. Chưa tích hợp chuyển tiền tự động/cổng thanh toán; admin xác nhận theo giao dịch thực tế.
- Refresh token, quên mật khẩu và giới hạn tần suất đăng nhập là các bước phát triển tiếp theo.
- Cần kiểm tra thêm trên trình duyệt và môi trường triển khai thực tế; build/lint và test API không thay thế kiểm thử giao diện.
