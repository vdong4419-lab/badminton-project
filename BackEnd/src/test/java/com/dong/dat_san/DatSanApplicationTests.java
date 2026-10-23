package com.dong.dat_san;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dong.dat_san.service.DichVuLichSan;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"debug=false", "logging.level.root=INFO", "logging.level.org.springframework=INFO", "logging.level.org.hibernate=INFO"})
@ActiveProfiles("integration")
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_TESTS", matches = "true")
class DatSanApplicationTests {
    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder encoder;
    @Autowired DichVuLichSan schedules;
    @Autowired com.dong.dat_san.service.DichVuHetHanGiuCho expiry;
    @org.springframework.boot.test.mock.mockito.SpyBean java.time.Clock businessClock;
    @org.springframework.boot.test.mock.mockito.MockBean com.dong.dat_san.service.DichVuGuiMail mail;
    private long court;
    private List<Long> slots;
    private LocalDate tomorrow;
    private final String password = "Test-password-2026";

    @BeforeEach
    void fixture() {
        assertThat(jdbc.queryForObject("SELECT DATABASE()", String.class)).isEqualTo("dat_san_kiem_thu");
        jdbc.update("DELETE FROM gioi_han_yeu_cau");
        http.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
        tomorrow = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(1);
        String name = "Test-" + UUID.randomUUID();
        jdbc.update("INSERT INTO san (ten_san,gia_moi_gio) VALUES (?,100000)", name);
        court = jdbc.queryForObject("SELECT id FROM san WHERE ten_san=?", Long.class, name);
        var frames = jdbc.queryForList("SELECT id FROM khung_gio ORDER BY gio_bat_dau LIMIT 3", Long.class);
        for (Long frame : frames) {
            jdbc.update("INSERT INTO lich_san (san_id,khung_gio_id,ngay_choi,gia) VALUES (?,?,?,50000)",
                    court, frame, tomorrow);
        }
        slots = jdbc.queryForList("SELECT id FROM lich_san WHERE san_id=? ORDER BY id", Long.class, court);
    }

    @Test
    void registrationLoginValidationAndPasswordPrivacy() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        var registration = Map.of("email", email.toUpperCase(Locale.ROOT), "matKhau", password,
                "hoTen", " Khách kiểm thử ", "soDienThoai", "");
        assertThat(call(HttpMethod.POST, "/api/xac-thuc/dang-ky", registration, null).getStatusCode().value()).isEqualTo(201);
        assertThat(call(HttpMethod.POST, "/api/xac-thuc/dang-ky", registration, null).getStatusCode().value()).isEqualTo(409);
        String hash = jdbc.queryForObject("SELECT mat_khau_bam FROM nguoi_dung WHERE email=?", String.class, email);
        assertThat(hash).isNotEqualTo(password);
        assertThat(encoder.matches(password, hash)).isTrue();
        verifyEmail(email);
        var login = call(HttpMethod.POST, "/api/xac-thuc/dang-nhap", Map.of("email", email, "matKhau", password), null);
        assertThat(login.getStatusCode().value()).isEqualTo(200);
        assertThat(tree(login).get("vaiTro").asText()).isEqualTo("CUSTOMER");
        assertThat(login.getBody()).doesNotContain("matKhau", hash);
        assertThat(call(HttpMethod.POST, "/api/xac-thuc/dang-nhap", Map.of("email", email, "matKhau", "wrong"), null)
                .getStatusCode().value()).isEqualTo(401);
        assertThat(call(HttpMethod.POST, "/api/xac-thuc/dang-ky",
                Map.of("email", "invalid", "matKhau", "short", "hoTen", ""), null).getStatusCode().value()).isEqualTo(400);
        String token = tree(login).get("token").asText();
        jdbc.update("UPDATE nguoi_dung SET dang_hoat_dong=FALSE WHERE email=?", email);
        assertThat(call(HttpMethod.GET, "/api/dat-san/lich-su", null, token).getStatusCode().value()).isEqualTo(401);
        assertThat(call(HttpMethod.GET, "/api/dat-san/lich-su", null, "broken.token").getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void publicReadsAndAdminOnlyCourtCreation() throws Exception {
        assertThat(call(HttpMethod.GET, "/api/san", null, null).getStatusCode().value()).isEqualTo(200);
        assertThat(available()).hasSize(3);
        var request = Map.of("tenSan", "Admin-" + UUID.randomUUID(), "giaMoiGio", 120000);
        assertThat(call(HttpMethod.POST, "/api/san", request, null).getStatusCode().value()).isEqualTo(401);
        String email = "admin-" + UUID.randomUUID() + "@example.com";
        String token = user(email);
        assertThat(call(HttpMethod.POST, "/api/san", request, token).getStatusCode().value()).isEqualTo(403);
        jdbc.update("UPDATE nguoi_dung SET vai_tro_id=(SELECT id FROM vai_tro WHERE ma='ADMIN') WHERE email=?", email);
        var result = call(HttpMethod.POST, "/api/san", request, token);
        assertThat(result.getStatusCode().value()).isEqualTo(201);
        long createdId = tree(result).get("id").asLong();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM lich_san WHERE san_id=?", Integer.class, createdId)).isEqualTo(238);
        jdbc.update("UPDATE lich_san SET gia=12345,mo_dat=FALSE WHERE id=?", slots.get(0));
        schedules.moLich();
        assertThat(jdbc.queryForObject("SELECT gia FROM lich_san WHERE id=?", Integer.class, slots.get(0))).isEqualTo(12345);
        assertThat(jdbc.queryForObject("SELECT mo_dat FROM lich_san WHERE id=?", Boolean.class, slots.get(0))).isFalse();
    }

    @Test
    void bookingHistoryCancellationAndRebookingKeepPriceSnapshot() throws Exception {
        String token = user();
        var booking = book(token, slots.subList(0, 2));
        assertThat(booking.getStatusCode().value()).isEqualTo(201);
        long id = tree(booking).get("id").asLong();
        assertThat(tree(booking).get("tongTien").asInt()).isEqualTo(100000);
        assertThat(tree(booking).get("chiTietDatSans")).hasSize(2);
        assertThat(available()).hasSize(1);
        jdbc.update("UPDATE lich_san SET gia=70000 WHERE id=?", slots.get(0));
        var history = call(HttpMethod.GET, "/api/dat-san/lich-su", null, token);
        assertThat(history.getStatusCode().value()).isEqualTo(200);
        assertThat(history.getBody()).doesNotContain("matKhau", "nguoiDung", "email");
        var first = tree(history).get(0);
        assertThat(first.get("chiTietDatSans").get(0).get("donGia").asInt()).isEqualTo(50000);
        assertThat(first.get("chiTietDatSans").get(0).get("lichSan").get("ngayChoi").asText()).isEqualTo(tomorrow.toString());
        assertThat(call(HttpMethod.PUT, "/api/dat-san/" + id + "/huy", Map.of("lyDoHuy", "Đổi lịch chơi"), token)
                .getStatusCode().value()).isEqualTo(200);
        assertThat(available()).hasSize(3);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM chi_tiet_dat_san WHERE dat_san_id=?", Integer.class, id)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM lich_su_dat_san WHERE dat_san_id=?", Integer.class, id)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT ly_do_huy FROM dat_san WHERE id=?", String.class, id)).isEqualTo("Đổi lịch chơi");
        assertThat(jdbc.queryForObject("SELECT ngay_huy IS NOT NULL FROM dat_san WHERE id=?", Boolean.class, id)).isTrue();
        assertThat(book(user(), slots.subList(0, 2)).getStatusCode().value()).isEqualTo(201);
    }

    @Test
    void concurrentBookingsHaveExactlyOneWinner() throws Exception {
        String a = user();
        String b = user();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Integer> first = pool.submit(() -> { start.await(); return book(a, List.of(slots.get(0))).getStatusCode().value(); });
            Future<Integer> second = pool.submit(() -> { start.await(); return book(b, List.of(slots.get(0))).getStatusCode().value(); });
            start.countDown();
            assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM giu_cho WHERE lich_san_id=?", Integer.class, slots.get(0))).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM chi_tiet_dat_san WHERE lich_san_id=?", Integer.class, slots.get(0))).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void databaseFailureRollsBackOrderDetailsAndEarlierHolds() throws Exception {
        String token = user();
        int before = jdbc.queryForObject("SELECT COUNT(*) FROM dat_san", Integer.class);
        jdbc.execute("CREATE TRIGGER test_hold_conflict BEFORE INSERT ON giu_cho FOR EACH ROW BEGIN "
                + "IF NEW.lich_san_id = " + slots.get(1) + " THEN SIGNAL SQLSTATE '23000' "
                + "SET MYSQL_ERRNO=1062, MESSAGE_TEXT='injected test conflict'; END IF; END");
        try {
            assertThat(book(token, slots.subList(0, 2)).getStatusCode().value()).isEqualTo(409);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dat_san", Integer.class)).isEqualTo(before);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM giu_cho WHERE lich_san_id IN (?,?)",
                    Integer.class, slots.get(0), slots.get(1))).isZero();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM chi_tiet_dat_san WHERE lich_san_id IN (?,?)",
                    Integer.class, slots.get(0), slots.get(1))).isZero();
        } finally {
            jdbc.execute("DROP TRIGGER test_hold_conflict");
        }
    }

    @Test
    void invalidOrUnavailableSlotsDoNotCreateOrders() throws Exception {
        String token = user();
        assertThat(book(token, List.of()).getStatusCode().value()).isEqualTo(400);
        assertThat(book(token, List.of(slots.get(0), slots.get(0))).getStatusCode().value()).isEqualTo(400);
        assertThat(book(token, List.of(-1L)).getStatusCode().value()).isEqualTo(400);
        assertThat(book(token, List.of(Long.MAX_VALUE)).getStatusCode().value()).isEqualTo(404);
        jdbc.update("UPDATE lich_san SET ngay_choi=? WHERE id=?", tomorrow.minusDays(2), slots.get(0));
        jdbc.update("UPDATE lich_san SET mo_dat=FALSE WHERE id=?", slots.get(1));
        assertThat(available()).hasSize(1);
        assertThat(book(token, List.of(slots.get(0))).getStatusCode().value()).isEqualTo(409);
        assertThat(book(token, List.of(slots.get(1))).getStatusCode().value()).isEqualTo(409);
        jdbc.update("UPDATE san SET dang_hoat_dong=FALSE WHERE id=?", court);
        assertThat(available()).isEmpty();
        assertThat(book(token, List.of(slots.get(2))).getStatusCode().value()).isEqualTo(409);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM chi_tiet_dat_san WHERE lich_san_id IN (?,?,?)",
                Integer.class, slots.get(0), slots.get(1), slots.get(2))).isZero();
    }

    @Test
    void cancellationRejectsOtherUsersCompletedStartedAndQueuesPaidRefund() throws Exception {
        String token = user();
        var response = book(token, List.of(slots.get(0)));
        long id = tree(response).get("id").asLong();
        String path = "/api/dat-san/" + id + "/huy";
        assertThat(call(HttpMethod.PUT, path, Map.of("lyDoHuy", "Khác chủ đơn"), user()).getStatusCode().value()).isEqualTo(403);
        jdbc.update("UPDATE dat_san SET trang_thai='DA_HOAN_THANH' WHERE id=?", id);
        assertThat(call(HttpMethod.PUT, path, null, token).getStatusCode().value()).isEqualTo(409);
        jdbc.update("UPDATE dat_san SET trang_thai='DA_XAC_NHAN' WHERE id=?", id);
        jdbc.update("UPDATE lich_san SET ngay_choi=? WHERE id=?", tomorrow.minusDays(2), slots.get(0));
        assertThat(call(HttpMethod.PUT, path, null, token).getStatusCode().value()).isEqualTo(409);
        jdbc.update("UPDATE lich_san SET ngay_choi=? WHERE id=?", tomorrow, slots.get(0));
        jdbc.update("INSERT INTO thanh_toan (dat_san_id,so_tien,phuong_thuc,trang_thai,thoi_diem_thanh_toan) "
                + "VALUES (?,50000,'TIEN_MAT','DA_THANH_TOAN',CURRENT_TIMESTAMP)", id);
        assertThat(call(HttpMethod.PUT, path, null, token).getStatusCode().value()).isEqualTo(200);
        assertThat(tree(call(HttpMethod.GET, "/api/dat-san/lich-su", null, token)).get(0).get("coTheHuy").asBoolean()).isFalse();
        assertThat(tree(call(HttpMethod.GET, "/api/dat-san/lich-su", null, token)).get(0).get("thanhToan").get("choHoanTien").asBoolean()).isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM giu_cho WHERE lich_san_id=?", Integer.class, slots.get(0))).isZero();
    }

    @Test
    void concurrentCancellationRecordsOneTransition() throws Exception {
        String token = user();
        long id = tree(book(token, slots)).get("id").asLong();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Callable<Integer> cancel = () -> {
                start.await();
                return call(HttpMethod.PUT, "/api/dat-san/" + id + "/huy", Map.of("lyDoHuy", "Hủy đồng thời"), token)
                        .getStatusCode().value();
            };
            var first = pool.submit(cancel);
            var second = pool.submit(cancel);
            start.countDown();
            assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM lich_su_dat_san WHERE dat_san_id=?", Integer.class, id)).isEqualTo(2);
            assertThat(available()).hasSize(3);
        } finally {
            pool.shutdownNow();
        }
    }


    @Test
    void customersCannotReadOrMutateAdminOrdersOrPromoteThemselves() throws Exception {
        String customer=user();
        long id=tree(book(customer, List.of(slots.get(0)))).get("id").asLong();
        String path="/api/quan-tri/don-dat/" + id;
        assertThat(call(HttpMethod.GET, "/api/quan-tri/don-dat", null, customer).getStatusCode().value()).isEqualTo(403);
        assertThat(call(HttpMethod.GET, path, null, customer).getStatusCode().value()).isEqualTo(403);
        for (String action : List.of("xac-nhan","huy","hoan-thanh")) {
            assertThat(call(HttpMethod.PUT, path + "/" + action, Map.of("lyDoHuy","test"), customer).getStatusCode().value()).isEqualTo(403);
        }
        for (String action : List.of("thanh-toan","hoan-tien")) {
            assertThat(call(HttpMethod.POST, path + "/" + action, Map.of("phuongThuc","TIEN_MAT"), customer).getStatusCode().value()).isEqualTo(403);
        }
        assertThat(call(HttpMethod.GET, path, null, null).getStatusCode().value()).isEqualTo(401);
        String email="fake-admin-" + UUID.randomUUID() + "@example.com";
        assertThat(call(HttpMethod.POST, "/api/xac-thuc/dang-ky",
                Map.of("email",email,"matKhau",password,"hoTen","Test","vaiTro","ADMIN"), null).getStatusCode().value()).isEqualTo(201);
        assertThat(jdbc.queryForObject("SELECT v.ma FROM nguoi_dung n JOIN vai_tro v ON v.id=n.vai_tro_id WHERE n.email=?", String.class,email)).isEqualTo("CUSTOMER");
        assertThat(tree(call(HttpMethod.GET,"/api/xac-thuc/toi",null,customer)).get("vaiTro").asText()).isEqualTo("CUSTOMER");
    }

    @Test
    void adminConfirmsCollectsAndRefundsFullAmountWithAuditTrail() throws Exception {
        String customer=user(), admin=admin();
        long id=tree(book(customer, slots.subList(0,2))).get("id").asLong();
        String path="/api/quan-tri/don-dat/" + id;
        assertThat(call(HttpMethod.PUT,path+"/xac-nhan",null,admin).getStatusCode().value()).isEqualTo(200);
        var collection=call(HttpMethod.POST,path+"/thanh-toan",
                Map.of("phuongThuc","CHUYEN_KHOAN","maThamChieu","PAY-"+id,"soTien",1),admin);
        assertThat(collection.getStatusCode().value()).isEqualTo(200);
        assertThat(tree(collection).get("don").get("thanhToan").get("soTien").asInt()).isEqualTo(100000);
        assertThat(tree(collection).get("don").get("hetHanGiuChoLuc").isNull()).isTrue();
        assertThat(call(HttpMethod.POST,path+"/hoan-tien",Map.of("maBienNhan","RF-"+id,"ghiChu","Hoàn đủ"),admin).getStatusCode().value()).isEqualTo(409);
        assertThat(call(HttpMethod.PUT,"/api/dat-san/"+id+"/huy",Map.of("lyDoHuy","Đổi kế hoạch"),customer).getStatusCode().value()).isEqualTo(200);
        var pending=tree(call(HttpMethod.GET,"/api/quan-tri/don-dat?choHoanTien=true",null,admin));
        assertThat(pending.get("items").findValues("maDat")).isNotEmpty();
        var refund=call(HttpMethod.POST,path+"/hoan-tien",Map.of("maBienNhan","RF-"+id,"ghiChu","Đã chuyển đủ cho khách","soTien",1),admin);
        assertThat(refund.getStatusCode().value()).isEqualTo(200);
        var money=tree(refund).get("don").get("thanhToan");
        assertThat(money.get("soTien").asInt()).isEqualTo(100000);
        assertThat(money.get("trangThai").asText()).isEqualTo("DA_HOAN_TIEN");
        assertThat(money.get("maThamChieu").asText()).isEqualTo("PAY-"+id);
        assertThat(money.get("choHoanTien").asBoolean()).isFalse();
        assertThat(refund.getBody()).doesNotContain("matKhauBam");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM lich_su_dat_san WHERE dat_san_id=? AND ghi_chu LIKE 'HOAN_TIEN%'",Integer.class,id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT nguoi_thuc_hien_id IS NOT NULL FROM lich_su_dat_san WHERE dat_san_id=? AND ghi_chu LIKE 'HOAN_TIEN%'",Boolean.class,id)).isTrue();
        assertThat(call(HttpMethod.POST,path+"/hoan-tien",Map.of("maBienNhan","RF-again","ghiChu","Lặp"),admin).getStatusCode().value()).isEqualTo(409);
        assertThat(tree(call(HttpMethod.GET,"/api/dat-san/lich-su",null,customer)).get(0).get("thanhToan").get("trangThai").asText()).isEqualTo("DA_HOAN_TIEN");
    }

    @Test
    void expiredPendingOrdersReleaseSlotsOnceAndCannotBeConfirmed() throws Exception {
        String customer=user(), admin=admin();
        long id=tree(book(customer,slots)).get("id").asLong();
        jdbc.update("UPDATE dat_san SET ngay_tao=DATE_SUB(CURRENT_TIMESTAMP,INTERVAL 16 MINUTE) WHERE id=?",id);
        expiry.quet(); expiry.quet();
        assertThat(jdbc.queryForObject("SELECT trang_thai FROM dat_san WHERE id=?",String.class,id)).isEqualTo("DA_HUY");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM lich_su_dat_san WHERE dat_san_id=?",Integer.class,id)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM giu_cho WHERE lich_san_id IN (?,?,?)",Integer.class,slots.get(0),slots.get(1),slots.get(2))).isZero();
        assertThat(call(HttpMethod.PUT,"/api/quan-tri/don-dat/"+id+"/xac-nhan",null,admin).getStatusCode().value()).isEqualTo(409);
        assertThat(book(user(),slots).getStatusCode().value()).isEqualTo(201);
    }

    @Test
    void confirmedAndLegacyPaidOrdersDoNotExpire() throws Exception {
        String customer=user(), admin=admin();
        long confirmed=tree(book(customer,List.of(slots.get(0)))).get("id").asLong();
        assertThat(call(HttpMethod.PUT,"/api/quan-tri/don-dat/"+confirmed+"/xac-nhan",null,admin).getStatusCode().value()).isEqualTo(200);
        long paidPending=tree(book(customer,List.of(slots.get(1)))).get("id").asLong();
        jdbc.update("INSERT INTO thanh_toan (dat_san_id,so_tien,phuong_thuc,trang_thai) VALUES (?,50000,'TIEN_MAT','DA_THANH_TOAN')",paidPending);
        jdbc.update("UPDATE dat_san SET ngay_tao=DATE_SUB(CURRENT_TIMESTAMP,INTERVAL 20 MINUTE) WHERE id IN (?,?)",confirmed,paidPending);
        expiry.quet();
        assertThat(jdbc.queryForObject("SELECT trang_thai FROM dat_san WHERE id=?",String.class,confirmed)).isEqualTo("DA_XAC_NHAN");
        assertThat(jdbc.queryForObject("SELECT trang_thai FROM dat_san WHERE id=?",String.class,paidPending)).isEqualTo("CHO_XAC_NHAN");
        assertThat(available()).hasSize(1);
    }

    @Test
    void concurrentCollectionAndRefundHaveOneWinnerEach() throws Exception {
        String customer=user(), admin=admin();
        long id=tree(book(customer,slots)).get("id").asLong();
        String path="/api/quan-tri/don-dat/"+id;
        assertThat(call(HttpMethod.PUT,path+"/xac-nhan",null,admin).getStatusCode().value()).isEqualTo(200);
        var money=Map.of("phuongThuc","TIEN_MAT");
        assertConcurrentStatuses(() -> call(HttpMethod.POST,path+"/thanh-toan",money,admin).getStatusCode().value(),200,409);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM thanh_toan WHERE dat_san_id=?",Integer.class,id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM lich_su_dat_san WHERE dat_san_id=? AND ghi_chu LIKE 'THU_TIEN%'",Integer.class,id)).isEqualTo(1);
        assertThat(call(HttpMethod.PUT,path+"/huy",Map.of("lyDoHuy","Sân bảo trì"),admin).getStatusCode().value()).isEqualTo(200);
        assertConcurrentStatuses(() -> call(HttpMethod.POST,path+"/hoan-tien",Map.of("maBienNhan","CASH-"+id,"ghiChu","Đã trả đủ"),admin).getStatusCode().value(),200,409);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM lich_su_dat_san WHERE dat_san_id=? AND ghi_chu LIKE 'HOAN_TIEN%'",Integer.class,id)).isEqualTo(1);
    }

    @Test
    void invalidTransitionsAndCompletionRulesAreEnforced() throws Exception {
        String customer=user(), admin=admin();
        long id=tree(book(customer,List.of(slots.get(0)))).get("id").asLong();
        String path="/api/quan-tri/don-dat/"+id;
        assertThat(call(HttpMethod.POST,path+"/thanh-toan",Map.of("phuongThuc","TIEN_MAT"),admin).getStatusCode().value()).isEqualTo(409);
        assertThat(call(HttpMethod.PUT,path+"/xac-nhan",null,admin).getStatusCode().value()).isEqualTo(200);
        assertThat(call(HttpMethod.PUT,path+"/xac-nhan",null,admin).getStatusCode().value()).isEqualTo(409);
        assertThat(call(HttpMethod.POST,path+"/thanh-toan",Map.of("phuongThuc","KHAC"),admin).getStatusCode().value()).isEqualTo(400);
        assertThat(call(HttpMethod.POST,path+"/thanh-toan",Map.of("phuongThuc","CHUYEN_KHOAN"),admin).getStatusCode().value()).isEqualTo(400);
        assertThat(call(HttpMethod.PUT,path+"/hoan-thanh",null,admin).getStatusCode().value()).isEqualTo(409);
        assertThat(call(HttpMethod.POST,path+"/thanh-toan",Map.of("phuongThuc","TIEN_MAT"),admin).getStatusCode().value()).isEqualTo(200);
        assertThat(call(HttpMethod.PUT,path+"/hoan-thanh",null,admin).getStatusCode().value()).isEqualTo(409);
        jdbc.update("UPDATE lich_san SET ngay_choi=? WHERE id=?",tomorrow.minusDays(2),slots.get(0));
        assertThat(call(HttpMethod.PUT,path+"/hoan-thanh",null,admin).getStatusCode().value()).isEqualTo(200);
        assertThat(call(HttpMethod.PUT,path+"/huy",Map.of("lyDoHuy","Hủy sau chơi"),admin).getStatusCode().value()).isEqualTo(409);
        assertThat(call(HttpMethod.POST,path+"/hoan-tien",Map.of("maBienNhan","R","ghiChu","Sai trạng thái"),admin).getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void failedExpiryRollsBackCancellationAndSlotRelease() throws Exception {
        long id=tree(book(user(),slots)).get("id").asLong();
        jdbc.update("UPDATE dat_san SET ngay_tao=DATE_SUB(CURRENT_TIMESTAMP,INTERVAL 16 MINUTE) WHERE id=?",id);
        jdbc.execute("CREATE TRIGGER test_expiry_failure BEFORE INSERT ON lich_su_dat_san FOR EACH ROW BEGIN "
                + "IF NEW.dat_san_id="+id+" AND NEW.trang_thai_moi='DA_HUY' THEN SIGNAL SQLSTATE '23000' "
                + "SET MYSQL_ERRNO=1062, MESSAGE_TEXT='injected expiry failure'; END IF; END");
        try {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> expiry.xuLy(id)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            assertThat(jdbc.queryForObject("SELECT trang_thai FROM dat_san WHERE id=?",String.class,id)).isEqualTo("CHO_XAC_NHAN");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM giu_cho WHERE lich_san_id IN (?,?,?)",Integer.class,slots.get(0),slots.get(1),slots.get(2))).isEqualTo(3);
        } finally { jdbc.execute("DROP TRIGGER test_expiry_failure"); }
        expiry.xuLy(id);
        assertThat(jdbc.queryForObject("SELECT trang_thai FROM dat_san WHERE id=?",String.class,id)).isEqualTo("DA_HUY");
    }


    @Test
    void collectionRacingCustomerCancellationCannotLoseRefundObligation() throws Exception {
        String customer=user(), admin=admin();
        long id=tree(book(customer,List.of(slots.get(0)))).get("id").asLong();
        String path="/api/quan-tri/don-dat/"+id;
        assertThat(call(HttpMethod.PUT,path+"/xac-nhan",null,admin).getStatusCode().value()).isEqualTo(200);
        ExecutorService pool=Executors.newFixedThreadPool(2);
        CountDownLatch start=new CountDownLatch(1);
        try {
            var payment=pool.submit(() -> { start.await(); return call(HttpMethod.POST,path+"/thanh-toan",Map.of("phuongThuc","TIEN_MAT"),admin).getStatusCode().value(); });
            var cancellation=pool.submit(() -> { start.await(); return call(HttpMethod.PUT,"/api/dat-san/"+id+"/huy",Map.of("lyDoHuy","Đổi lịch"),customer).getStatusCode().value(); });
            start.countDown();
            assertThat(cancellation.get(20,TimeUnit.SECONDS)).isEqualTo(200);
            assertThat(payment.get(20,TimeUnit.SECONDS)).isIn(200,409);
            var result=tree(call(HttpMethod.GET,path,null,admin)).get("don");
            assertThat(result.get("trangThai").asText()).isEqualTo("DA_HUY");
            var money=result.get("thanhToan");
            assertThat(money.get("choHoanTien").asBoolean()).isEqualTo("DA_THANH_TOAN".equals(money.get("trangThai").asText()));
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM giu_cho WHERE lich_san_id=?",Integer.class,slots.get(0))).isZero();
        } finally { pool.shutdownNow(); }
    }

    @Test
    void expirationRacingConfirmationKeepsExpiredOrderCancelled() throws Exception {
        String admin=admin();
        long id=tree(book(user(),List.of(slots.get(0)))).get("id").asLong();
        jdbc.update("UPDATE dat_san SET ngay_tao=DATE_SUB(CURRENT_TIMESTAMP,INTERVAL 16 MINUTE) WHERE id=?",id);
        ExecutorService pool=Executors.newFixedThreadPool(2);
        CountDownLatch start=new CountDownLatch(1);
        try {
            var expiryJob=pool.submit(() -> { start.await(); expiry.xuLy(id); return true; });
            var confirmation=pool.submit(() -> { start.await(); return call(HttpMethod.PUT,"/api/quan-tri/don-dat/"+id+"/xac-nhan",null,admin).getStatusCode().value(); });
            start.countDown();
            assertThat(expiryJob.get(20,TimeUnit.SECONDS)).isTrue();
            assertThat(confirmation.get(20,TimeUnit.SECONDS)).isEqualTo(409);
            assertThat(jdbc.queryForObject("SELECT trang_thai FROM dat_san WHERE id=?",String.class,id)).isEqualTo("DA_HUY");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM lich_su_dat_san WHERE dat_san_id=?",Integer.class,id)).isEqualTo(2);
        } finally { pool.shutdownNow(); }
    }



    @Test
    void staffIsLimitedToTodaysOperationsAndTheirOwnCollections() throws Exception {
        String staff=staff(), customer=user(), admin=admin();
        long id=tree(book(customer,List.of(slots.get(0)))).get("id").asLong();
        String path="/api/quan-tri/don-dat/"+id;
        assertThat(call(HttpMethod.GET,path,null,staff).getStatusCode().value()).isEqualTo(403);
        assertThat(call(HttpMethod.GET,"/api/quan-tri/lich-san?ngay="+tomorrow,null,staff).getStatusCode().value()).isEqualTo(403);
        assertThat(call(HttpMethod.PUT,path+"/xac-nhan",null,admin).getStatusCode().value()).isEqualTo(200);
        jdbc.update("UPDATE lich_san SET ngay_choi=? WHERE id=?",tomorrow.minusDays(1),slots.get(0));
        assertThat(call(HttpMethod.GET,path,null,staff).getStatusCode().value()).isEqualTo(200);
        assertThat(call(HttpMethod.POST,path+"/thanh-toan",Map.of("phuongThuc","TIEN_MAT"),staff).getStatusCode().value()).isEqualTo(200);
        assertThat(call(HttpMethod.POST,path+"/hoan-tien",Map.of("maBienNhan","RF","ghiChu","Test"),staff).getStatusCode().value()).isEqualTo(403);
        var own=tree(call(HttpMethod.GET,"/api/quan-tri/thu-ca",null,staff));
        assertThat(own.get("tongThu").asInt()).isEqualTo(50000);
        assertThat(own.get("giaoDich")).hasSize(1);
        var asAdmin=tree(call(HttpMethod.GET,"/api/quan-tri/thu-ca",null,admin));
        assertThat(asAdmin.get("tongThu").asInt()).isZero();
        for (String route:List.of("/san","/tai-khoan","/bao-cao?tuNgay="+tomorrow+"&denNgay="+tomorrow,
                "/tong-quan?ngay="+tomorrow,"/nhat-ky","/bao-cao.csv?tuNgay="+tomorrow+"&denNgay="+tomorrow)) {
            assertThat(call(HttpMethod.GET,"/api/quan-tri"+route,null,staff).getStatusCode().value()).isEqualTo(403);
            assertThat(call(HttpMethod.GET,"/api/quan-tri"+route,null,customer).getStatusCode().value()).isEqualTo(403);
        }
        assertThat(call(HttpMethod.GET,"/api/quan-tri/don-dat",null,staff).getStatusCode().value()).isEqualTo(200);
        assertThat(tree(call(HttpMethod.GET,"/api/quan-tri/don-dat?tim="+id,null,staff)).get("items")).isNotNull();
    }

    @Test
    void accountManagementCreatesStaffAndRevokesExistingTokensOnLock() throws Exception {
        String admin=admin();
        String email="managed-"+UUID.randomUUID()+"@example.com";
        var payload=new HashMap<String,Object>();
        payload.put("hoTen","Nhân viên");payload.put("email",email);payload.put("vaiTro","STAFF");
        payload.put("dangHoatDong",true);payload.put("matKhau",password);
        var created=call(HttpMethod.POST,"/api/quan-tri/tai-khoan",payload,admin);
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        long id=tree(created).get("id").asLong();
        var login=call(HttpMethod.POST,"/api/xac-thuc/dang-nhap",Map.of("email",email,"matKhau",password),null);
        assertThat(login.getStatusCode().value()).isEqualTo(200);
        String token=tree(login).get("token").asText();
        assertThat(tree(login).get("vaiTro").asText()).isEqualTo("STAFF");
        assertThat(call(HttpMethod.GET,"/api/quan-tri/tai-khoan",null,token).getStatusCode().value()).isEqualTo(403);
        payload.remove("matKhau");payload.put("dangHoatDong",false);
        assertThat(call(HttpMethod.PUT,"/api/quan-tri/tai-khoan/"+id,payload,admin).getStatusCode().value()).isEqualTo(200);
        assertThat(call(HttpMethod.GET,"/api/quan-tri/thu-ca",null,token).getStatusCode().value()).isEqualTo(401);
        var list=call(HttpMethod.GET,"/api/quan-tri/tai-khoan?tim="+email,null,admin);
        assertThat(list.getStatusCode().value()).isEqualTo(200);
        assertThat(list.getBody()).doesNotContain("matKhau","mat_khau_bam",password);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM nhat_ky_quan_tri WHERE doi_tuong=?",Integer.class,"NGUOI_DUNG:"+id)).isEqualTo(2);
    }

    @Test
    void adminCannotLockOrDemoteTheirOwnAccount() throws Exception {
        String email="owner-"+UUID.randomUUID()+"@example.com", token=user(email);
        jdbc.update("UPDATE nguoi_dung SET vai_tro_id=(SELECT id FROM vai_tro WHERE ma='ADMIN') WHERE email=?",email);
        long id=jdbc.queryForObject("SELECT id FROM nguoi_dung WHERE email=?",Long.class,email);
        var payload=new HashMap<String,Object>();
        payload.put("hoTen","Owner");payload.put("email",email);payload.put("vaiTro","CUSTOMER");payload.put("dangHoatDong",true);
        assertThat(call(HttpMethod.PUT,"/api/quan-tri/tai-khoan/"+id,payload,token).getStatusCode().value()).isEqualTo(409);
        payload.put("vaiTro","ADMIN");payload.put("dangHoatDong",false);
        assertThat(call(HttpMethod.PUT,"/api/quan-tri/tai-khoan/"+id,payload,token).getStatusCode().value()).isEqualTo(409);
        assertThat(jdbc.queryForObject("SELECT dang_hoat_dong FROM nguoi_dung WHERE id=?",Boolean.class,id)).isTrue();
    }

    @Test
    void courtsAndSlotsCanBeManagedWithoutChangingExistingPricesOrReservations() throws Exception {
        String admin=admin(), customer=user();
        var form=new HashMap<String,Object>();
        form.put("tenSan","Court-"+UUID.randomUUID());form.put("giaMoiGio",180000);
        form.put("moTa","Mô tả mới");form.put("anhDaiDien","https://example.com/court.png");form.put("dangHoatDong",true);
        var updated=call(HttpMethod.PUT,"/api/quan-tri/san/"+court,form,admin);
        assertThat(updated.getStatusCode().value()).isEqualTo(200);
        assertThat(tree(updated).get("moTa").asText()).isEqualTo("Mô tả mới");
        assertThat(jdbc.queryForObject("SELECT gia FROM lich_san WHERE id=?",Integer.class,slots.get(0))).isEqualTo(50000);
        assertThat(call(HttpMethod.PUT,"/api/quan-tri/lich-san/"+slots.get(1),Map.of("gia",65000,"moDat",false),admin).getStatusCode().value()).isEqualTo(200);
        assertThat(book(customer,List.of(slots.get(1))).getStatusCode().value()).isEqualTo(409);
        long orderId=tree(book(customer,List.of(slots.get(0)))).get("id").asLong();
        assertThat(call(HttpMethod.PUT,"/api/quan-tri/lich-san/"+slots.get(0),Map.of("gia",1000,"moDat",false),admin).getStatusCode().value()).isEqualTo(409);
        form.put("dangHoatDong",false);
        assertThat(call(HttpMethod.PUT,"/api/quan-tri/san/"+court,form,admin).getStatusCode().value()).isEqualTo(409);
        assertThat(call(HttpMethod.PUT,"/api/dat-san/"+orderId+"/huy",Map.of("lyDoHuy","Đổi lịch"),customer).getStatusCode().value()).isEqualTo(200);
        assertThat(call(HttpMethod.PUT,"/api/quan-tri/san/"+court,form,admin).getStatusCode().value()).isEqualTo(200);
        assertThat(call(HttpMethod.GET,"/api/san/"+court,null,null).getStatusCode().value()).isEqualTo(404);
        assertThat(call(HttpMethod.GET,"/api/quan-tri/san",null,admin).getBody()).contains(form.get("tenSan").toString());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM nhat_ky_quan_tri WHERE doi_tuong=?",Integer.class,"SAN:"+court)).isEqualTo(2);
    }

    @Test
    void reportsKeepCollectionOnOriginalDateAndRefundOnRefundDate() throws Exception {
        String customer=user(), admin=admin();
        long first=tree(book(customer,List.of(slots.get(0)))).get("id").asLong();
        long second=tree(book(customer,List.of(slots.get(1)))).get("id").asLong();
        jdbc.update("UPDATE dat_san SET trang_thai='DA_HUY' WHERE id IN (?,?)",first,second);
        jdbc.update("""
                INSERT INTO thanh_toan(dat_san_id,so_tien,phuong_thuc,trang_thai,thoi_diem_thanh_toan,thoi_diem_hoan_tien)
                VALUES (?,50000,'TIEN_MAT','DA_HOAN_TIEN','2001-01-02 10:00:00','2001-01-03 10:00:00')
                """,first);
        jdbc.update("""
                INSERT INTO thanh_toan(dat_san_id,so_tien,phuong_thuc,trang_thai,thoi_diem_thanh_toan)
                VALUES (?,50000,'TIEN_MAT','DA_THANH_TOAN','2001-01-02 11:00:00')
                """,second);
        var received=tree(call(HttpMethod.GET,"/api/quan-tri/bao-cao?tuNgay=2001-01-02&denNgay=2001-01-02",null,admin));
        assertThat(received.get("tongThu").asInt()).isEqualTo(100000);
        assertThat(received.get("tongHoan").asInt()).isZero();
        var refunded=tree(call(HttpMethod.GET,"/api/quan-tri/bao-cao?tuNgay=2001-01-03&denNgay=2001-01-03",null,admin));
        assertThat(refunded.get("tongThu").asInt()).isZero();
        assertThat(refunded.get("tongHoan").asInt()).isEqualTo(50000);
        assertThat(refunded.get("thuRong").asInt()).isEqualTo(-50000);
        var csv=call(HttpMethod.GET,"/api/quan-tri/bao-cao.csv?tuNgay=2001-01-02&denNgay=2001-01-03",null,admin);
        assertThat(csv.getStatusCode().value()).isEqualTo(200);
        assertThat(csv.getBody()).contains("2001-01-02,100000.00,0,100000.00","2001-01-03,0,50000.00,-50000.00");
        assertThat(call(HttpMethod.GET,"/api/quan-tri/bao-cao?tuNgay=2001-01-03&denNgay=2001-01-02",null,admin).getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void counterBookingConfirmsCustomerOrderAndRecordsOperator() throws Exception {
        String customerEmail="desk-"+UUID.randomUUID()+"@example.com", customer=user(customerEmail), admin=admin();
        long customerId=jdbc.queryForObject("SELECT id FROM nguoi_dung WHERE email=?",Long.class,customerEmail);
        var input=Map.of("nguoiDungId",customerId,"danhSachLichSanIds",slots.subList(0,2));
        var result=call(HttpMethod.POST,"/api/quan-tri/dat-tai-quay",input,admin);
        assertThat(result.getStatusCode().value()).isEqualTo(201);
        var order=tree(result).get("don");
        assertThat(order.get("trangThai").asText()).isEqualTo("DA_XAC_NHAN");
        assertThat(order.get("hetHanGiuChoLuc").isNull()).isTrue();
        long id=order.get("id").asLong();
        assertThat(jdbc.queryForObject("SELECT nguoi_thuc_hien_id<>? FROM lich_su_dat_san WHERE dat_san_id=? AND trang_thai_cu IS NULL",Boolean.class,customerId,id)).isTrue();
        assertThat(call(HttpMethod.POST,"/api/quan-tri/dat-tai-quay",input,admin).getStatusCode().value()).isEqualTo(409);
        assertThat(call(HttpMethod.POST,"/api/quan-tri/dat-tai-quay",input,customer).getStatusCode().value()).isEqualTo(403);
        assertThat(call(HttpMethod.POST,"/api/quan-tri/dat-tai-quay",Map.of("nguoiDungId",customerId,"danhSachLichSanIds",List.of(slots.get(2))),staff()).getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void checkInIsRecordedOnceDuringBookingWindow() throws Exception {
        String admin=admin(), customer=user();
        long id=tree(book(customer,List.of(slots.get(0)))).get("id").asLong();
        String path="/api/quan-tri/don-dat/"+id;
        assertThat(call(HttpMethod.PUT,path+"/xac-nhan",null,admin).getStatusCode().value()).isEqualTo(200);
        assertThat(call(HttpMethod.PUT,path+"/check-in",null,admin).getStatusCode().value()).isEqualTo(409);
        java.time.Instant instant=tomorrow.atTime(6,5).atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        org.mockito.Mockito.doReturn(instant).when(businessClock).instant();
        assertConcurrentStatuses(() -> call(HttpMethod.PUT,path+"/check-in",null,admin).getStatusCode().value(),200,409);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM lich_su_dat_san WHERE dat_san_id=? AND ghi_chu LIKE 'CHECK_IN |%'",Integer.class,id)).isEqualTo(1);
    }


    @Test
    void concurrentAdminsCannotDemoteEachOtherAfterLosingAuthority() throws Exception {
        String emailA="owner-a-"+UUID.randomUUID()+"@example.com", emailB="owner-b-"+UUID.randomUUID()+"@example.com";
        String tokenA=user(emailA), tokenB=user(emailB);
        jdbc.update("UPDATE nguoi_dung SET vai_tro_id=(SELECT id FROM vai_tro WHERE ma='ADMIN') WHERE email IN (?,?)",emailA,emailB);
        long idA=jdbc.queryForObject("SELECT id FROM nguoi_dung WHERE email=?",Long.class,emailA);
        long idB=jdbc.queryForObject("SELECT id FROM nguoi_dung WHERE email=?",Long.class,emailB);
        ExecutorService pool=Executors.newFixedThreadPool(2);
        CountDownLatch start=new CountDownLatch(1);
        try {
            var first=pool.submit(() -> { start.await();return call(HttpMethod.PUT,"/api/quan-tri/tai-khoan/"+idB,Map.of("hoTen","Owner B","email",emailB,"vaiTro","CUSTOMER","dangHoatDong",true),tokenA).getStatusCode().value(); });
            var second=pool.submit(() -> { start.await();return call(HttpMethod.PUT,"/api/quan-tri/tai-khoan/"+idA,Map.of("hoTen","Owner A","email",emailA,"vaiTro","CUSTOMER","dangHoatDong",true),tokenB).getStatusCode().value(); });
            start.countDown();
            assertThat(List.of(first.get(20,TimeUnit.SECONDS),second.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,403);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM nguoi_dung n JOIN vai_tro v ON v.id=n.vai_tro_id WHERE n.id IN (?,?) AND v.ma='ADMIN' AND n.dang_hoat_dong=TRUE",Integer.class,idA,idB)).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }

    @Test
    void closingCourtRacingBookingCannotLeaveReservationOnClosedCourt() throws Exception {
        String customer=user(), admin=admin();
        String name=jdbc.queryForObject("SELECT ten_san FROM san WHERE id=?",String.class,court);
        ExecutorService pool=Executors.newFixedThreadPool(2);
        CountDownLatch start=new CountDownLatch(1);
        try {
            var closing=pool.submit(() -> { start.await();return call(HttpMethod.PUT,"/api/quan-tri/san/"+court,Map.of("tenSan",name,"giaMoiGio",100000,"dangHoatDong",false),admin).getStatusCode().value(); });
            var booking=pool.submit(() -> { start.await();return book(customer,List.of(slots.get(0))).getStatusCode().value(); });
            start.countDown();
            int closeStatus=closing.get(20,TimeUnit.SECONDS), bookStatus=booking.get(20,TimeUnit.SECONDS);
            assertThat(closeStatus==200 && bookStatus==409 || closeStatus==409 && bookStatus==201).isTrue();
            boolean active=jdbc.queryForObject("SELECT dang_hoat_dong FROM san WHERE id=?",Boolean.class,court);
            int held=jdbc.queryForObject("SELECT COUNT(*) FROM giu_cho WHERE lich_san_id=?",Integer.class,slots.get(0));
            assertThat(active || held==0).isTrue();
        } finally { pool.shutdownNow(); }
        assertThat(tree(call(HttpMethod.GET,"/api/quan-tri/tim-khach?tim=___",null,staff()))).isEmpty();
    }


    @Test
    void profileIsPrivateAndCannotChangeIdentityOrRole() throws Exception {
        String email="profile-"+UUID.randomUUID()+"@example.com", token=user(email), other=user();
        assertThat(call(HttpMethod.GET,"/api/ho-so",null,null).getStatusCode().value()).isEqualTo(401);
        long id=tree(call(HttpMethod.GET,"/api/ho-so",null,token)).get("id").asLong();
        String phone="09"+String.valueOf(id+100000000L);
        var saved=call(HttpMethod.PUT,"/api/ho-so",Map.of("hoTen"," Tên mới ","soDienThoai",phone,
                "email","hacked@example.com","vaiTro","ADMIN","id",Long.MAX_VALUE),token);
        assertThat(saved.getStatusCode().value()).isEqualTo(200);
        assertThat(tree(saved).get("hoTen").asText()).isEqualTo("Tên mới");
        assertThat(tree(saved).get("email").asText()).isEqualTo(email);
        assertThat(tree(saved).get("vaiTro").asText()).isEqualTo("CUSTOMER");
        assertThat(saved.getBody()).doesNotContain("matKhauBam",password);
        assertThat(call(HttpMethod.PUT,"/api/ho-so",Map.of("hoTen","Người khác","soDienThoai",phone),other).getStatusCode().value()).isEqualTo(409);
        assertThat(call(HttpMethod.PUT,"/api/ho-so",Map.of("hoTen","   ","soDienThoai","invalid"),token).getStatusCode().value()).isEqualTo(400);
        assertThat(call(HttpMethod.GET,"/api/quan-tri/tai-khoan",null,token).getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void passwordChangeRequiresCurrentPasswordAndInvalidatesExistingTokens() throws Exception {
        String email="password-"+UUID.randomUUID()+"@example.com", token=user(email);
        assertThat(call(HttpMethod.PUT,"/api/ho-so/mat-khau",Map.of("matKhauHienTai","wrong","matKhauMoi","New-password-2026"),token).getStatusCode().value()).isEqualTo(400);
        assertThat(call(HttpMethod.PUT,"/api/ho-so/mat-khau",Map.of("matKhauHienTai",password,"matKhauMoi","ế".repeat(30)),token).getStatusCode().value()).isEqualTo(400);
        assertThat(call(HttpMethod.PUT,"/api/ho-so/mat-khau",Map.of("matKhauHienTai",password,"matKhauMoi",password),token).getStatusCode().value()).isEqualTo(400);
        assertThat(call(HttpMethod.PUT,"/api/ho-so/mat-khau",Map.of("matKhauHienTai",password,"matKhauMoi","New-password-2026"),token).getStatusCode().value()).isEqualTo(200);
        assertThat(call(HttpMethod.GET,"/api/ho-so",null,token).getStatusCode().value()).isEqualTo(401);
        assertThat(call(HttpMethod.POST,"/api/xac-thuc/dang-nhap",Map.of("email",email,"matKhau",password),null).getStatusCode().value()).isEqualTo(401);
        var login=call(HttpMethod.POST,"/api/xac-thuc/dang-nhap",Map.of("email",email,"matKhau","New-password-2026"),null);
        assertThat(login.getStatusCode().value()).isEqualTo(200);
        assertThat(call(HttpMethod.GET,"/api/ho-so",null,tree(login).get("token").asText()).getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void loyaltyCountsOnlyPaidCompletedOrdersAtFiveAndTenAndCapsDiscount() throws Exception {
        String email="loyalty-"+UUID.randomUUID()+"@example.com",token=user(email);
        for (int i=0;i<4;i++) seedLoyaltyOrder(email,"DA_HOAN_THANH","DA_THANH_TOAN");
        seedLoyaltyOrder(email,"DA_HOAN_THANH","CHUA_THANH_TOAN");
        seedLoyaltyOrder(email,"DA_HOAN_THANH","DA_HOAN_TIEN");
        seedLoyaltyOrder(email,"DA_HUY","DA_THANH_TOAN");
        var request=Map.of("danhSachLichSanIds",slots);
        assertThat(tree(call(HttpMethod.POST,"/api/dat-san/bao-gia",request,token)).get("tyLeGiam").asInt()).isZero();
        assertThat(tree(call(HttpMethod.GET,"/api/ho-so",null,token)).get("thanhVien").get("soDonHoanThanh").asInt()).isEqualTo(4);
        seedLoyaltyOrder(email,"DA_HOAN_THANH","DA_THANH_TOAN");
        var first=tree(call(HttpMethod.POST,"/api/dat-san/bao-gia",request,token));
        assertThat(first.get("tyLeGiam").asInt()).isEqualTo(5);
        assertThat(first.get("soTienGiam").asInt()).isEqualTo(7500);
        for (int i=0;i<5;i++) seedLoyaltyOrder(email,"DA_HOAN_THANH","DA_THANH_TOAN");
        jdbc.update("UPDATE lich_san SET gia=250000 WHERE san_id=?",court);
        var capped=tree(call(HttpMethod.POST,"/api/dat-san/bao-gia",request,token));
        assertThat(capped.get("tyLeGiam").asInt()).isEqualTo(10);
        assertThat(capped.get("soTienGiam").asInt()).isEqualTo(50000);
        assertThat(capped.get("tongTien").asInt()).isEqualTo(700000);
        assertThat(call(HttpMethod.POST,"/api/dat-san/bao-gia",Map.of("danhSachLichSanIds",List.of(slots.get(0),slots.get(0))),token).getStatusCode().value()).isEqualTo(400);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM giu_cho WHERE lich_san_id IN (?,?,?)",Integer.class,slots.toArray())).isZero();
    }

    @Test
    void discountedBookingKeepsSnapshotAndRefundsOnlyAmountActuallyPaid() throws Exception {
        String email="snapshot-"+UUID.randomUUID()+"@example.com",token=user(email),admin=admin();
        for (int i=0;i<5;i++) seedLoyaltyOrder(email,"DA_HOAN_THANH","DA_THANH_TOAN");
        var created=call(HttpMethod.POST,"/api/dat-san",Map.of("danhSachLichSanIds",slots.subList(0,2),"tongTien",1,"soTienGiam",999999),token);
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        long id=tree(created).get("id").asLong();
        assertThat(tree(created).get("tongTien").asInt()).isEqualTo(95000);
        for (int i=0;i<5;i++) seedLoyaltyOrder(email,"DA_HOAN_THANH","DA_THANH_TOAN");
        jdbc.update("UPDATE lich_san SET gia=999999 WHERE san_id=?",court);
        var detail=tree(call(HttpMethod.GET,"/api/dat-san/"+id,null,token));
        assertThat(detail.get("uuDai").get("tyLeGiam").asInt()).isEqualTo(5);
        assertThat(detail.get("uuDai").get("tamTinh").asInt()).isEqualTo(100000);
        assertThat(detail.get("tongTien").asInt()).isEqualTo(95000);
        assertThat(call(HttpMethod.PUT,"/api/quan-tri/don-dat/"+id+"/xac-nhan",null,admin).getStatusCode().value()).isEqualTo(200);
        assertThat(call(HttpMethod.POST,"/api/quan-tri/don-dat/"+id+"/thanh-toan",Map.of("phuongThuc","CHUYEN_KHOAN","maThamChieu","TEST-"+UUID.randomUUID()),admin).getStatusCode().value()).isEqualTo(200);
        assertThat(call(HttpMethod.PUT,"/api/dat-san/"+id+"/huy",Map.of("lyDoHuy","Đổi lịch"),token).getStatusCode().value()).isEqualTo(200);
        var refunded=call(HttpMethod.POST,"/api/quan-tri/don-dat/"+id+"/hoan-tien",Map.of("maBienNhan","REFUND-TEST","ghiChu","Hoàn đủ tiền thực thu"),admin);
        assertThat(refunded.getStatusCode().value()).isEqualTo(200);
        assertThat(tree(refunded).get("don").get("thanhToan").get("soTien").asInt()).isEqualTo(95000);
    }

    @Test
    void transferNotificationIsOwnerOnlyIdempotentAndNeverMarksOrderPaid() throws Exception {
        String owner=user(),other=user(),admin=admin();
        long id=tree(book(owner,List.of(slots.get(0)))).get("id").asLong();
        assertThat(call(HttpMethod.GET,"/api/dat-san/"+id,null,other).getStatusCode().value()).isEqualTo(403);
        assertThat(call(HttpMethod.POST,"/api/dat-san/"+id+"/bao-chuyen-khoan",null,owner).getStatusCode().value()).isEqualTo(409);
        assertThat(tree(call(HttpMethod.GET,"/api/dat-san/"+id,null,owner)).get("chuyenKhoan").get("coTheChuyenKhoan").asBoolean()).isFalse();
        assertThat(call(HttpMethod.PUT,"/api/quan-tri/don-dat/"+id+"/xac-nhan",null,admin).getStatusCode().value()).isEqualTo(200);
        assertThat(call(HttpMethod.POST,"/api/dat-san/"+id+"/bao-chuyen-khoan",null,other).getStatusCode().value()).isEqualTo(403);
        assertConcurrentStatuses(() -> call(HttpMethod.POST,"/api/dat-san/"+id+"/bao-chuyen-khoan",null,owner).getStatusCode().value(),200,200);
        for (int i=0;i<2;i++) {
            var result=call(HttpMethod.POST,"/api/dat-san/"+id+"/bao-chuyen-khoan",null,owner);
            assertThat(result.getStatusCode().value()).isEqualTo(200);
            assertThat(tree(result).get("thanhToan").get("trangThai").asText()).isEqualTo("CHUA_THANH_TOAN");
            assertThat(tree(result).get("chuyenKhoan").get("daBaoLuc").isNull()).isFalse();
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM lich_su_dat_san WHERE dat_san_id=? AND ghi_chu LIKE 'BAO_CHUYEN_KHOAN |%'",Integer.class,id)).isEqualTo(1);
        var adminView=tree(call(HttpMethod.GET,"/api/quan-tri/don-dat/"+id,null,admin));
        assertThat(adminView.get("don").get("chuyenKhoan").get("noiDung").asText()).isEqualTo("DS"+id);
        assertThat(call(HttpMethod.POST,"/api/quan-tri/don-dat/"+id+"/thanh-toan",Map.of("phuongThuc","CHUYEN_KHOAN","maThamChieu","TEST-"+UUID.randomUUID()),owner).getStatusCode().value()).isEqualTo(403);
        assertThat(call(HttpMethod.POST,"/api/quan-tri/don-dat/"+id+"/thanh-toan",Map.of("phuongThuc","CHUYEN_KHOAN","maThamChieu","TEST-"+UUID.randomUUID()),admin).getStatusCode().value()).isEqualTo(200);
        assertThat(call(HttpMethod.POST,"/api/dat-san/"+id+"/bao-chuyen-khoan",null,owner).getStatusCode().value()).isEqualTo(409);
        assertThat(tree(call(HttpMethod.GET,"/api/dat-san/"+id,null,owner)).get("chuyenKhoan").get("coTheChuyenKhoan").asBoolean()).isFalse();
    }

    @Test
    void counterBookingsUseCustomerLoyaltyAndOperationsAccountsGetNoDiscount() throws Exception {
        String email="counter-loyalty-"+UUID.randomUUID()+"@example.com",token=user(email),admin=admin();
        for (int i=0;i<5;i++) seedLoyaltyOrder(email,"DA_HOAN_THANH","DA_THANH_TOAN");
        long id=tree(call(HttpMethod.GET,"/api/ho-so",null,token)).get("id").asLong();
        var result=call(HttpMethod.POST,"/api/quan-tri/dat-tai-quay",Map.of("nguoiDungId",id,"danhSachLichSanIds",slots.subList(0,2)),admin);
        assertThat(result.getStatusCode().value()).isIn(200,201);
        assertThat(tree(result).get("don").get("tongTien").asInt()).isEqualTo(95000);
        var quote=tree(call(HttpMethod.POST,"/api/dat-san/bao-gia",Map.of("danhSachLichSanIds",List.of(slots.get(2))),admin));
        assertThat(quote.get("tyLeGiam").asInt()).isZero();
    }

    @Test
    void simultaneousPasswordChangesAcceptCurrentPasswordOnlyOnce() throws Exception {
        String email="password-race-"+UUID.randomUUID()+"@example.com",token=user(email);
        ExecutorService pool=Executors.newFixedThreadPool(2);
        CountDownLatch start=new CountDownLatch(1);
        try {
            var first=pool.submit(() -> { start.await();return call(HttpMethod.PUT,"/api/ho-so/mat-khau",Map.of("matKhauHienTai",password,"matKhauMoi","New-password-A"),token).getStatusCode().value(); });
            var second=pool.submit(() -> { start.await();return call(HttpMethod.PUT,"/api/ho-so/mat-khau",Map.of("matKhauHienTai",password,"matKhauMoi","New-password-B"),token).getStatusCode().value(); });
            start.countDown();
            var statuses=List.of(first.get(20,TimeUnit.SECONDS),second.get(20,TimeUnit.SECONDS));
            assertThat(statuses.stream().filter(s -> s==200).count()).isEqualTo(1);
            assertThat(statuses).allMatch(s -> s==200 || s==400 || s==401);
            assertThat(call(HttpMethod.GET,"/api/ho-so",null,token).getStatusCode().value()).isEqualTo(401);
        } finally { pool.shutdownNow(); }
    }

    private void seedLoyaltyOrder(String email,String orderStatus,String paymentStatus) {
        String code=UUID.randomUUID().toString();
        long userId=jdbc.queryForObject("SELECT id FROM nguoi_dung WHERE email=?",Long.class,email);
        jdbc.update("INSERT INTO dat_san(ma_dat,nguoi_dung_id,trang_thai,tong_tien) VALUES (?,?,?,100000)",code,userId,orderStatus);
        long orderId=jdbc.queryForObject("SELECT id FROM dat_san WHERE ma_dat=?",Long.class,code);
        jdbc.update("INSERT INTO thanh_toan(dat_san_id,so_tien,phuong_thuc,trang_thai) VALUES (?,100000,'TIEN_MAT',?)",orderId,paymentStatus);
    }

    @Test
    void newAccountsMustVerifyEmailAndWrongOtpAttemptsPersist() throws Exception {
        String email="unverified-"+UUID.randomUUID()+"@example.com";
        var registration=call(HttpMethod.POST,"/api/xac-thuc/dang-ky",Map.of("email",email,"hoTen","Khách mới","matKhau",password),null);
        assertThat(registration.getStatusCode().value()).isEqualTo(201);
        String code=lastOtp(email,"XAC_MINH_EMAIL");
        assertThat(code).matches("[0-9]{6}");
        assertThat(registration.getBody()).doesNotContain(code);
        assertThat(call(HttpMethod.POST,"/api/xac-thuc/dang-nhap",Map.of("email",email,"matKhau",password),null).getStatusCode().value()).isEqualTo(403);
        String hash=jdbc.queryForObject("SELECT o.ma_bam FROM ma_otp o JOIN nguoi_dung n ON n.id=o.nguoi_dung_id WHERE n.email=?",String.class,email);
        assertThat(hash).isNotEqualTo(code); assertThat(encoder.matches(code,hash)).isTrue();
        String wrong=code.equals("000000") ? "111111" : "000000";
        for (int i=0;i<5;i++) assertThat(call(HttpMethod.POST,"/api/xac-thuc/xac-minh-email",Map.of("email",email,"otp",wrong),null).getStatusCode().value()).isEqualTo(400);
        assertThat(jdbc.queryForObject("SELECT o.so_lan_sai FROM ma_otp o JOIN nguoi_dung n ON n.id=o.nguoi_dung_id WHERE n.email=?",Integer.class,email)).isEqualTo(5);
        assertThat(call(HttpMethod.POST,"/api/xac-thuc/xac-minh-email",Map.of("email",email,"otp",code),null).getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void otpResendHasCooldownReplacesPreviousCodeAndExpires() throws Exception {
        String email="resend-"+UUID.randomUUID()+"@example.com";
        call(HttpMethod.POST,"/api/xac-thuc/dang-ky",Map.of("email",email,"hoTen","Khách OTP","matKhau",password),null);
        String first=lastOtp(email,"XAC_MINH_EMAIL");
        assertThat(call(HttpMethod.POST,"/api/xac-thuc/gui-lai-otp",Map.of("email",email),null).getStatusCode().value()).isEqualTo(200);
        org.mockito.Mockito.verify(mail,org.mockito.Mockito.times(1)).guiOtp(org.mockito.ArgumentMatchers.eq(email),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.eq("XAC_MINH_EMAIL"));
        var later=java.time.Instant.now().plusSeconds(61);
        org.mockito.Mockito.doReturn(later).when(businessClock).instant();
        assertThat(call(HttpMethod.POST,"/api/xac-thuc/gui-lai-otp",Map.of("email",email),null).getStatusCode().value()).isEqualTo(200);
        String second=lastOtp(email,"XAC_MINH_EMAIL");
        org.mockito.Mockito.verify(mail,org.mockito.Mockito.times(2)).guiOtp(org.mockito.ArgumentMatchers.eq(email),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.eq("XAC_MINH_EMAIL"));
        if (!first.equals(second)) assertThat(call(HttpMethod.POST,"/api/xac-thuc/xac-minh-email",Map.of("email",email,"otp",first),null).getStatusCode().value()).isEqualTo(400);
        org.mockito.Mockito.doReturn(later.plusSeconds(601)).when(businessClock).instant();
        assertThat(call(HttpMethod.POST,"/api/xac-thuc/xac-minh-email",Map.of("email",email,"otp",second),null).getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void passwordResetDoesNotRevealAccountsIsSingleUseAndRevokesTokens() throws Exception {
        String email="reset-"+UUID.randomUUID()+"@example.com",token=user(email);
        var known=call(HttpMethod.POST,"/api/xac-thuc/quen-mat-khau",Map.of("email",email),null);
        var unknown=call(HttpMethod.POST,"/api/xac-thuc/quen-mat-khau",Map.of("email","missing-"+UUID.randomUUID()+"@example.com"),null);
        assertThat(known.getStatusCode().value()).isEqualTo(200);assertThat(unknown.getBody()).isEqualTo(known.getBody());
        String code=lastOtp(email,"DAT_LAI_MAT_KHAU");
        assertThat(known.getBody()).doesNotContain(code);
        var request=Map.of("email",email,"otp",code,"matKhauMoi","Recovered-password-2026");
        assertConcurrentStatuses(() -> call(HttpMethod.POST,"/api/xac-thuc/dat-lai-mat-khau",request,null).getStatusCode().value(),200,400);
        assertThat(call(HttpMethod.GET,"/api/ho-so",null,token).getStatusCode().value()).isEqualTo(401);
        assertThat(call(HttpMethod.POST,"/api/xac-thuc/dang-nhap",Map.of("email",email,"matKhau",password),null).getStatusCode().value()).isEqualTo(401);
        assertThat(call(HttpMethod.POST,"/api/xac-thuc/dang-nhap",Map.of("email",email,"matKhau","Recovered-password-2026"),null).getStatusCode().value()).isEqualTo(200);
        assertThat(call(HttpMethod.POST,"/api/xac-thuc/dat-lai-mat-khau",request,null).getStatusCode().value()).isEqualTo(400);
        assertThat(call(HttpMethod.POST,"/api/xac-thuc/xac-minh-email",Map.of("email",email,"otp",lastOtp(email,"XAC_MINH_EMAIL")),null).getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void failedMailDeliveryRollsBackRegistration() throws Exception {
        String email="mail-failure-"+UUID.randomUUID()+"@example.com";
        org.mockito.Mockito.doThrow(new org.springframework.web.server.ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Gửi mail thất bại"))
                .when(mail).guiOtp(org.mockito.ArgumentMatchers.eq(email),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString());
        assertThat(call(HttpMethod.POST,"/api/xac-thuc/dang-ky",Map.of("email",email,"hoTen","Khách mail","matKhau",password),null).getStatusCode().value()).isEqualTo(503);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM nguoi_dung WHERE email=?",Integer.class,email)).isZero();
    }

    @Test
    void loginRateLimitNormalizesEmailAndRecoversAfterFifteenMinutes() throws Exception {
        String email="login-limit-"+UUID.randomUUID()+"@example.com";user(email);
        for (int i=0;i<5;i++) assertThat(call(HttpMethod.POST,"/api/xac-thuc/dang-nhap",Map.of("email",i%2==0 ? email.toUpperCase(Locale.ROOT) : email,"matKhau","wrong"),null).getStatusCode().value()).isEqualTo(401);
        assertThat(call(HttpMethod.POST,"/api/xac-thuc/dang-nhap",Map.of("email",email,"matKhau",password),null).getStatusCode().value()).isEqualTo(429);
        org.mockito.Mockito.doReturn(java.time.Instant.now().plusSeconds(901)).when(businessClock).instant();
        assertThat(call(HttpMethod.POST,"/api/xac-thuc/dang-nhap",Map.of("email",email,"matKhau",password),null).getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void notificationsArePrivateAndReadStateIsIdempotent() throws Exception {
        String owner=user(),other=user(),admin=admin();
        long order=tree(book(owner,List.of(slots.get(0)))).get("id").asLong();
        var initial=tree(call(HttpMethod.GET,"/api/thong-bao",null,owner));
        assertThat(initial.get("totalUnread").asInt()).isEqualTo(1);
        long notification=initial.get("items").get(0).get("id").asLong();
        assertThat(tree(call(HttpMethod.GET,"/api/thong-bao",null,other)).get("items")).isEmpty();
        assertThat(call(HttpMethod.PUT,"/api/thong-bao/"+notification+"/da-doc",null,other).getStatusCode().value()).isEqualTo(404);
        for (int i=0;i<2;i++) assertThat(call(HttpMethod.PUT,"/api/thong-bao/"+notification+"/da-doc",null,owner).getStatusCode().value()).isEqualTo(200);
        assertThat(tree(call(HttpMethod.GET,"/api/thong-bao",null,owner)).get("totalUnread").asInt()).isZero();
        assertThat(call(HttpMethod.PUT,"/api/quan-tri/don-dat/"+order+"/xac-nhan",null,admin).getStatusCode().value()).isEqualTo(200);
        var confirmed=tree(call(HttpMethod.GET,"/api/thong-bao",null,owner));
        assertThat(confirmed.get("totalUnread").asInt()).isEqualTo(1);
        assertThat(confirmed.get("items").get(0).get("tieuDe").asText()).contains("xác nhận");
        assertThat(call(HttpMethod.PUT,"/api/thong-bao/doc-tat-ca",null,owner).getStatusCode().value()).isEqualTo(200);
        assertThat(tree(call(HttpMethod.GET,"/api/thong-bao",null,owner)).get("totalUnread").asInt()).isZero();
        assertThat(call(HttpMethod.GET,"/api/thong-bao",null,null).getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void reviewsRequirePaidCompletedOwnedBookingAndAreUniquePerCourt() throws Exception {
        String owner=user(),other=user(),admin=admin();
        long id=tree(book(owner,slots.subList(0,2))).get("id").asLong();
        var input=Map.of("sanId",court,"diem",4,"binhLuan","Sân sạch sẽ, ánh sáng tốt, phục vụ nhiệt tình.");
        assertThat(call(HttpMethod.POST,"/api/dat-san/"+id+"/danh-gia",input,owner).getStatusCode().value()).isEqualTo(409);
        assertThat(call(HttpMethod.POST,"/api/dat-san/"+id+"/danh-gia",input,other).getStatusCode().value()).isEqualTo(403);
        call(HttpMethod.PUT,"/api/quan-tri/don-dat/"+id+"/xac-nhan",null,admin);
        call(HttpMethod.POST,"/api/quan-tri/don-dat/"+id+"/thanh-toan",Map.of("phuongThuc","TIEN_MAT"),admin);
        org.mockito.Mockito.doReturn(tomorrow.atTime(23,59).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant()).when(businessClock).instant();
        assertThat(call(HttpMethod.PUT,"/api/quan-tri/don-dat/"+id+"/hoan-thanh",null,admin).getStatusCode().value()).isEqualTo(200);
        assertThat(tree(call(HttpMethod.GET,"/api/dat-san/"+id+"/danh-gia",null,owner)).get(0).get("coTheDanhGia").asBoolean()).isTrue();
        assertThat(call(HttpMethod.POST,"/api/dat-san/"+id+"/danh-gia",Map.of("sanId",court+99999,"diem",4,"binhLuan","Bình luận kiểm thử"),owner).getStatusCode().value()).isEqualTo(400);
        assertThat(call(HttpMethod.POST,"/api/dat-san/"+id+"/danh-gia",Map.of("sanId",court,"diem",6,"binhLuan","Bình luận kiểm thử"),owner).getStatusCode().value()).isEqualTo(400);
        assertConcurrentStatuses(() -> call(HttpMethod.POST,"/api/dat-san/"+id+"/danh-gia",input,owner).getStatusCode().value(),201,409);
        var publicReviews=call(HttpMethod.GET,"/api/san/"+court+"/danh-gia",null,null);
        assertThat(publicReviews.getStatusCode().value()).isEqualTo(200);
        assertThat(tree(publicReviews).get("tongDanhGia").asInt()).isEqualTo(1);
        assertThat(tree(publicReviews).get("diemTrungBinh").asDouble()).isEqualTo(4.0);
        assertThat(publicReviews.getBody()).doesNotContain("email","matKhau","datSanId","nguoiDungId");
        var publicCourt=tree(call(HttpMethod.GET,"/api/san/"+court,null,null));
        assertThat(publicCourt.get("tongDanhGia").asInt()).isEqualTo(1);
        assertThat(publicCourt.get("diemTrungBinh").asDouble()).isEqualTo(4.0);
        assertThat(publicCourt.get("danhGiaMoiNhat").get("binhLuan").asText()).isEqualTo(input.get("binhLuan"));
        assertThat(tree(call(HttpMethod.GET,"/api/dat-san/"+id+"/danh-gia",null,owner)).get(0).get("coTheDanhGia").asBoolean()).isFalse();
    }

    @Test
    void passwordResetMailFailureDoesNotRevealAccountAndRollsBackOtp() throws Exception {
        String email="reset-mail-failure-"+UUID.randomUUID()+"@example.com";user(email);
        org.mockito.Mockito.doThrow(new org.springframework.web.server.ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"SMTP error"))
                .when(mail).guiOtp(org.mockito.ArgumentMatchers.eq(email),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.eq("DAT_LAI_MAT_KHAU"));
        var existing=call(HttpMethod.POST,"/api/xac-thuc/quen-mat-khau",Map.of("email",email),null);
        var missing=call(HttpMethod.POST,"/api/xac-thuc/quen-mat-khau",Map.of("email","missing-"+UUID.randomUUID()+"@example.com"),null);
        assertThat(existing.getStatusCode().value()).isEqualTo(200);
        assertThat(missing.getStatusCode()).isEqualTo(existing.getStatusCode());
        assertThat(missing.getBody()).isEqualTo(existing.getBody());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ma_otp o JOIN nguoi_dung n ON n.id=o.nguoi_dung_id WHERE n.email=? AND o.muc_dich='DAT_LAI_MAT_KHAU'",Integer.class,email)).isZero();
    }

    @Test
    void publicCourtCardsAggregateRatingsAndLatestCommentWithoutPrivateData() throws Exception {
        String name=jdbc.queryForObject("SELECT ten_san FROM san WHERE id=?",String.class,court);
        var empty=tree(call(HttpMethod.GET,"/api/san/"+court,null,null));
        assertThat(empty.get("tongDanhGia").asInt()).isZero();
        assertThat(empty.get("diemTrungBinh").asDouble()).isZero();
        assertThat(empty.get("danhGiaMoiNhat").isNull()).isTrue();
        String email="public-rating-"+UUID.randomUUID()+"@example.com";user(email);
        for (int score : List.of(3,5)) {
            seedLoyaltyOrder(email,"DA_HOAN_THANH","DA_THANH_TOAN");
            long order=jdbc.queryForObject("SELECT MAX(d.id) FROM dat_san d JOIN nguoi_dung n ON n.id=d.nguoi_dung_id WHERE n.email=?",Long.class,email);
            jdbc.update("INSERT INTO danh_gia_san(dat_san_id,san_id,diem,binh_luan,ngay_tao) VALUES (?,?,?,?,?)",order,court,score,
                    score==5 ? "Nhận xét mới nhất - sân tốt và sạch sẽ." : "Nhận xét trước - dịch vụ ổn định.",java.time.LocalDateTime.now());
        }
        var listResponse=call(HttpMethod.GET,"/api/san?ten="+name,null,null);
        assertThat(listResponse.getStatusCode().value()).isEqualTo(200);
        var list=tree(listResponse);assertThat(list).hasSize(1);
        var card=list.get(0);
        assertThat(card.get("id").asLong()).isEqualTo(court);
        assertThat(card.get("diemTrungBinh").asDouble()).isEqualTo(4.0);
        assertThat(card.get("tongDanhGia").asInt()).isEqualTo(2);
        assertThat(card.get("danhGiaMoiNhat").get("diem").asInt()).isEqualTo(5);
        assertThat(card.get("danhGiaMoiNhat").get("binhLuan").asText()).startsWith("Nhận xét mới nhất");
        assertThat(listResponse.getBody()).doesNotContain(email,"soDienThoai","matKhau","datSanId","nguoiDungId");
        assertThat(tree(call(HttpMethod.GET,"/api/san/"+court,null,null))).isEqualTo(card);
        jdbc.update("UPDATE san SET dang_hoat_dong=FALSE WHERE id=?",court);
        assertThat(tree(call(HttpMethod.GET,"/api/san?ten="+name,null,null))).isEmpty();
        assertThat(call(HttpMethod.GET,"/api/san/"+court,null,null).getStatusCode().value()).isEqualTo(404);
    }

    private String staff() throws Exception {
        String email="staff-"+UUID.randomUUID()+"@example.com";
        String token=user(email);
        jdbc.update("UPDATE nguoi_dung SET vai_tro_id=(SELECT id FROM vai_tro WHERE ma='STAFF') WHERE email=?",email);
        return token;
    }


    private String admin() throws Exception {
        String email="admin-"+UUID.randomUUID()+"@example.com";
        String token=user(email);
        jdbc.update("UPDATE nguoi_dung SET vai_tro_id=(SELECT id FROM vai_tro WHERE ma='ADMIN') WHERE email=?",email);
        return token;
    }

    private void assertConcurrentStatuses(Callable<Integer> action, int expectedFirst, int expectedSecond) throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(2);
        CountDownLatch start=new CountDownLatch(1);
        try {
            Callable<Integer> gated=() -> { start.await(); return action.call(); };
            var first=pool.submit(gated); var second=pool.submit(gated);
            start.countDown();
            assertThat(List.of(first.get(20,TimeUnit.SECONDS),second.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(expectedFirst,expectedSecond);
        } finally { pool.shutdownNow(); }
    }

    private String user() throws Exception {
        return user("customer-" + UUID.randomUUID() + "@example.com");
    }

    private String user(String email) throws Exception {
        var payload = Map.of("email", email, "matKhau", password, "hoTen", "Khách kiểm thử");
        assertThat(call(HttpMethod.POST, "/api/xac-thuc/dang-ky", payload, null).getStatusCode().value()).isEqualTo(201);
        verifyEmail(email);
        var login = call(HttpMethod.POST, "/api/xac-thuc/dang-nhap", payload, null);
        assertThat(login.getStatusCode().value()).isEqualTo(200);
        return tree(login).get("token").asText();
    }

    private ResponseEntity<String> book(String token, List<Long> ids) {
        return call(HttpMethod.POST, "/api/dat-san", Map.of("danhSachLichSanIds", ids), token);
    }

    private String lastOtp(String email,String purpose) {
        return org.mockito.Mockito.mockingDetails(mail).getInvocations().stream()
                .filter(i -> i.getMethod().getName().equals("guiOtp") && i.getArgument(0).equals(email) && i.getArgument(2).equals(purpose))
                .reduce((first,second) -> second).orElseThrow().getArgument(1);
    }

    private void verifyEmail(String email) {
        assertThat(call(HttpMethod.POST,"/api/xac-thuc/xac-minh-email",Map.of("email",email,"otp",lastOtp(email,"XAC_MINH_EMAIL")),null).getStatusCode().value()).isEqualTo(200);
    }

    private JsonNode available() throws Exception {
        var response = call(HttpMethod.GET, "/api/lich-san/trong?sanId=" + court + "&ngay=" + tomorrow, null, null);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        return tree(response);
    }

    private JsonNode tree(ResponseEntity<String> response) throws Exception {
        return json.readTree(response.getBody());
    }

    private ResponseEntity<String> call(HttpMethod method, String path, Object body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) headers.setBearerAuth(token);
        return http.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }
}
