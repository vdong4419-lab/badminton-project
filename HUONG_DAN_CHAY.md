# Chạy hệ thống đặt sân

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

## Phạm vi hiện tại

- Đơn mới ở trạng thái CHO_XAC_NHAN và chiếm suất đến khi được hủy hoặc xử lý tiếp.
- Khách chỉ hủy đơn chưa bắt đầu, chưa thanh toán, đang chờ hoặc đã xác nhận.
- Đơn đã thanh toán cần quy trình quản trị hủy/hoàn tiền; phiên bản này chưa cung cấp giao diện đó.
- Đăng ký luôn tạo CUSTOMER. Muốn có ADMIN, quản trị database gán vai trò ADMIN cho đúng tài khoản đã đăng ký; API đăng ký không nhận quyền từ client.
- Refresh token, quên mật khẩu, giới hạn tần suất đăng nhập và màn hình quản trị xác nhận/thanh toán là các bước phát triển tiếp theo.
- Cần kiểm tra thêm trên trình duyệt và môi trường triển khai thực tế; build/lint và test API không thay thế kiểm thử giao diện.
