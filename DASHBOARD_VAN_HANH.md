# Dashboard vận hành — đợt 1, 18/10/2026

## Phạm vi hoàn thành

Dashboard đã được chia thành các trang riêng, dùng menu bên trái trên màn hình lớn và menu cuộn trên điện thoại:

- /quan-tri/tong-quan: số đơn có lịch chơi theo ngày, đơn chờ, tiền thu/hoàn, tỷ lệ đặt suất và tiền chờ hoàn hiện tại.
- /quan-tri/lich-san: lịch theo ngày, từng sân/khung giờ; mở đơn từ ô đã đặt, chọn nhiều suất để đặt tại quầy; admin sửa giá và mở/đóng suất trống.
- /quan-tri/don-dat: tìm mã đơn/tên/điện thoại, lọc ngày chơi và trạng thái, xử lý xác nhận/thu/hủy/hoàn thành/check-in/hoàn tiền theo quyền.
- /quan-tri/thu-ca: các khoản do chính tài khoản hiện tại xác nhận thu hôm nay.
- /quan-tri/san: thêm/sửa tên, mô tả, ảnh bằng URL, giá mặc định và trạng thái sân; thấy cả sân tạm ngừng.
- /quan-tri/bao-cao: tiền thu, tiền hoàn, thu ròng theo khoảng ngày; biểu đồ theo ngày và xuất CSV.
- /quan-tri/tai-khoan: tạo tài khoản CUSTOMER/STAFF/ADMIN, sửa thông tin, vai trò và khóa/mở tài khoản.
- /quan-tri/nhat-ky: theo dõi ai thay đổi sân, lịch và tài khoản.

Không dùng tổng tiền của trang danh sách đơn làm doanh thu. Thẻ trạng thái hệ thống cố định trước đây được thay bằng số liệu nghiệp vụ từ backend.

## Quyền làm việc

| Chức năng | ADMIN | STAFF | CUSTOMER |
| --- | --- | --- | --- |
| Xem lịch vận hành | Mọi ngày | Hôm nay | Chỉ lịch trống công khai |
| Xem/xử lý đơn của khách | Mọi ngày | Đơn có ít nhất một suất hôm nay | Không |
| Đặt tại quầy cho khách có tài khoản | Có | Chỉ các suất hôm nay | Không |
| Xác nhận, thu tiền, check-in, hoàn thành | Có | Trong phạm vi hôm nay | Không |
| Hủy đơn của khách chưa thu tiền | Có | Trong phạm vi hôm nay, trước giờ chơi | Không |
| Hủy đơn của khách đã thu tiền / xác nhận đã hoàn tiền | Có | Không; chuyển admin xử lý | Không |
| Xem tiền tự ghi nhận hôm nay | Có | Có | Không |
| Báo cáo thu/hoàn toàn hệ thống, CSV | Có | Không | Không |
| Đổi giá/đóng lịch, quản lý sân | Có | Không | Không |
| Tạo/sửa/khóa/cấp quyền tài khoản và xem nhật ký | Có | Không | Không |

Khách vẫn chỉ xem và hủy đơn của mình theo chính sách trước giờ chơi. STAFF cũng có thể sử dụng các chức năng đặt sân cá nhân theo quyền chủ đơn; điều này không cấp quyền hủy đơn đã thanh toán của người khác.

Quyền được kiểm tra ở cả SecurityFilterChain và dịch vụ. Frontend chỉ hiển thị menu được cấp quyền. Khóa tài khoản hoặc đổi vai trò có hiệu lực với các yêu cầu tiếp theo dù JWT cũ còn hạn.

STAFF được tìm khách đang hoạt động theo tên/email/điện thoại, từ 3 ký tự và tối đa 10 kết quả. Các ký tự % và _ được xem là ký tự tìm kiếm thông thường.

## Quy tắc vận hành

- Đặt tại quầy cần một khách CUSTOMER đang hoạt động. Đơn được tạo và xác nhận trong cùng transaction, ghi đúng người thao tác. Nếu không giữ được đủ suất thì toàn bộ giao dịch rollback.
- Check-in chỉ thực hiện một lần cho đơn đã xác nhận, từ 15 phút trước suất đầu tiên đến hết suất cuối. Nhân viên chỉ xử lý đơn trong lịch hôm nay.
- Không sửa giá hoặc đóng suất đang bị giữ; không sửa suất đã bắt đầu.
- Không tạm ngừng sân còn giữ chỗ chưa kết thúc. Admin cần xử lý các đơn bị ảnh hưởng trước.
- Giá mặc định của sân dùng khi sinh suất mới. Những suất đã tạo giữ giá riêng; chỉnh giá cụ thể tại Lịch sân. Giá chụp trong chi tiết đơn luôn được giữ nguyên.
- Các giao dịch đặt và chỉnh sân/lịch dùng thứ tự khóa sân trước rồi suất để bảo vệ trường hợp đóng sân đồng thời với đặt sân.
- Không tự khóa hoặc đổi quyền tài khoản đang đăng nhập. Các thao tác tài khoản được tuần tự hóa bằng khóa vai trò ADMIN và kiểm tra quyền hiện tại trong database để giữ ít nhất một admin hoạt động.
- Email đăng nhập hiện có được giữ cố định trên màn hình quản trị. Mật khẩu ban đầu chỉ nhập khi tạo tài khoản và không xuất hiện trong danh sách hoặc nhật ký.

## Ý nghĩa số liệu tiền

- Tiền thu: các giao dịch có ngày thu nằm trong kỳ, bao gồm giao dịch đã được hoàn ở ngày khác.
- Tiền hoàn: các giao dịch có ngày hoàn nằm trong kỳ.
- Thu ròng: tiền thu trong kỳ trừ tiền hoàn trong kỳ; có thể âm, chưa trừ chi phí vận hành.
- Tiền chờ hoàn hiện tại: tổng nghĩa vụ hoàn của đơn đã hủy nhưng chưa xác nhận đã hoàn, không giới hạn theo kỳ báo cáo.
- Thu tiền của tôi: tiền chính tài khoản đó ghi nhận hôm nay. Đây không phải thu ròng hoặc số dư tiền mặt của một ca có giờ mở/đóng.

Báo cáo tối đa 366 ngày. Mỗi ngày được trả về kể cả khi không có giao dịch. Dữ liệu tiền được tổng hợp tại backend trên toàn bộ database, độc lập với phân trang đơn.

## Khởi động và database

Khởi động lại backend và frontend. Spring Boot chạy db/operations.sql trước khi validate entity để thêm vai trò STAFF và tạo bảng nhat_ky_quan_tri nếu chưa có. Tài khoản MySQL chạy ứng dụng cần quyền tạo bảng và ghi dữ liệu trong database của dự án. Script này không thêm tài khoản hay mật khẩu nhân viên mặc định.

Đăng nhập ADMIN, vào Tài khoản & nhân viên để tạo STAFF. Nhân viên đăng nhập sẽ vào thẳng Lịch sân; admin vào Tổng quan.

Giữ các quy tắc giữ suất 15 phút và hoàn đủ tiền đã thu. Thu/hoàn tiền vẫn được ghi nhận thủ công sau khi đối chiếu giao dịch thực tế.

## Kiểm tra và phần tiếp theo

Bộ kiểm thử đầy đủ chạy bằng BackEnd/kiem-thu-mysql.ps1 trên MySQL riêng. Frontend chạy npm run build và npm run lint.

Đợt này tập trung dashboard, sân/lịch, nhân viên, tài khoản và dòng tiền. Các bước kế tiếp là voucher khách quen dựa trên giờ chơi hoàn thành đã thanh toán, chương trình khuyến mãi có giới hạn sử dụng, sau đó thanh toán trực tuyến khi đã chọn nhà cung cấp và cấu hình kết nối.
## Kết quả kiểm tra của đợt này

- Backend: 31/31 bài kiểm thử thành công, không bỏ qua bài nào: 26 bài tích hợp MySQL/API, 3 bài chính sách giữ suất và 2 bài JWT.
- Đã kiểm tra STAFF chỉ truy cập đơn/lịch hôm nay và khoản thu của chính mình; không gọi được API quản lý tài khoản/sân/báo cáo hoặc hoàn tiền.
- Đã kiểm tra tạo và khóa STAFF, từ chối token sau khi khóa, không trả mật khẩu trong danh sách/nhật ký, không tự hạ quyền/khóa chính mình và hai admin hạ quyền nhau đồng thời chỉ có một thao tác thành công.
- Đã kiểm tra chỉnh giá suất trống, giữ nguyên giá lịch sử, từ chối đóng/sửa suất đã đặt, ẩn sân tạm ngừng khỏi khách nhưng vẫn thấy trong quản trị.
- Đã kiểm tra đóng sân đồng thời với đặt sân: không tồn tại đơn chiếm suất trên sân vừa bị đóng.
- Đã kiểm tra đặt tại quầy xác nhận ngay, ghi đúng người thao tác, từ chối đặt trùng và check-in đồng thời chỉ ghi một lần.
- Đã kiểm tra báo cáo thu trước/hoàn sau, thu ròng âm ở ngày hoàn, xuất CSV và chặn khoảng ngày không hợp lệ.
- Frontend build và lint thành công. Chưa xác minh trực tiếp dashboard mới bằng trình duyệt trên database đang sử dụng.

Các dòng injected failure/duplicate trong log kiểm thử là lỗi được chủ động gây ra để kiểm tra rollback và chống tranh chấp.