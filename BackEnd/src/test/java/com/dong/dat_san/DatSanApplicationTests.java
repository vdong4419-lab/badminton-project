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
    private long court;
    private List<Long> slots;
    private LocalDate tomorrow;
    private final String password = "Test-password-2026";

    @BeforeEach
    void fixture() {
        assertThat(jdbc.queryForObject("SELECT DATABASE()", String.class)).isEqualTo("dat_san_kiem_thu");
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
    void cancellationRejectsOtherUsersCompletedStartedAndPaidOrders() throws Exception {
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
        assertThat(call(HttpMethod.PUT, path, null, token).getStatusCode().value()).isEqualTo(409);
        assertThat(tree(call(HttpMethod.GET, "/api/dat-san/lich-su", null, token)).get(0).get("coTheHuy").asBoolean()).isFalse();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM giu_cho WHERE lich_san_id=?", Integer.class, slots.get(0))).isEqualTo(1);
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

    private String user() throws Exception {
        return user("customer-" + UUID.randomUUID() + "@example.com");
    }

    private String user(String email) throws Exception {
        var payload = Map.of("email", email, "matKhau", password, "hoTen", "Khách kiểm thử");
        assertThat(call(HttpMethod.POST, "/api/xac-thuc/dang-ky", payload, null).getStatusCode().value()).isEqualTo(201);
        var login = call(HttpMethod.POST, "/api/xac-thuc/dang-nhap", payload, null);
        assertThat(login.getStatusCode().value()).isEqualTo(200);
        return tree(login).get("token").asText();
    }

    private ResponseEntity<String> book(String token, List<Long> ids) {
        return call(HttpMethod.POST, "/api/dat-san", Map.of("danhSachLichSanIds", ids), token);
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
