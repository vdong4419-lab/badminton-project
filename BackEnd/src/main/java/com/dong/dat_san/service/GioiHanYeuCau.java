package com.dong.dat_san.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GioiHanYeuCau {
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final TransactionTemplate tx;
    public GioiHanYeuCau(JdbcTemplate jdbc,Clock clock,PlatformTransactionManager manager) {
        this.jdbc=jdbc;this.clock=clock;tx=new TransactionTemplate(manager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
    public void kiemTra(String key,int limit,int seconds) {
        String hashed=hash(key);
        boolean allowed=Boolean.TRUE.equals(tx.execute(status -> {
            var now=LocalDateTime.now(clock);
            jdbc.update("INSERT INTO gioi_han_yeu_cau(khoa,bat_dau,so_lan) VALUES (?,?,0) ON DUPLICATE KEY UPDATE khoa=gioi_han_yeu_cau.khoa",hashed,now);
            var row=jdbc.queryForObject("SELECT bat_dau,so_lan FROM gioi_han_yeu_cau WHERE khoa=? FOR UPDATE",
                    (r,i) -> new CuaSo(r.getTimestamp("bat_dau").toLocalDateTime(),r.getInt("so_lan")),hashed);
            if (!row.batDau().plusSeconds(seconds).isAfter(now)) {
                jdbc.update("UPDATE gioi_han_yeu_cau SET bat_dau=?,so_lan=1 WHERE khoa=?",now,hashed);return true;
            }
            if (row.soLan()>=limit) return false;
            jdbc.update("UPDATE gioi_han_yeu_cau SET so_lan=so_lan+1 WHERE khoa=?",hashed);return true;
        }));
        if (!allowed) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Bạn đã thử quá nhiều lần. Vui lòng thử lại sau.");
    }
    public void xoa(String key) { tx.executeWithoutResult(s -> jdbc.update("DELETE FROM gioi_han_yeu_cau WHERE khoa=?",hash(key))); }
    private record CuaSo(LocalDateTime batDau,int soLan) {}
    private static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }
}
