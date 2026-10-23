package com.dong.dat_san.controller;

import com.dong.dat_san.dto.*;
import com.dong.dat_san.service.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quan-tri")
public class VanHanhController {
    private final DichVuQuanLySan courts;
    private final DichVuTaiKhoan users;
    private final DichVuBaoCao reports;
    private final DichVuDatTaiQuay desk;
    private final DichVuHetHanGiuCho expiry;
    public VanHanhController(DichVuQuanLySan courts,DichVuTaiKhoan users,DichVuBaoCao reports,
            DichVuDatTaiQuay desk,DichVuHetHanGiuCho expiry) {
        this.courts=courts;this.users=users;this.reports=reports;this.desk=desk;this.expiry=expiry;
    }
    @GetMapping("/san") public Object san() { return courts.danhSach(); }
    @PostMapping("/san") @ResponseStatus(HttpStatus.CREATED)
    public Object taoSan(@Valid @RequestBody YeuCauSuaSan input) { return courts.luu(null,input); }
    @PutMapping("/san/{id}")
    public Object suaSan(@PathVariable Long id,@Valid @RequestBody YeuCauSuaSan input) { return courts.luu(id,input); }
    @GetMapping("/lich-san")
    public Object lich(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate ngay) {
        expiry.quet();return courts.lich(ngay);
    }
    @PutMapping("/lich-san/{id}")
    public Map<String,String> suaLich(@PathVariable Long id,@Valid @RequestBody YeuCauSuaLich input) {
        courts.suaLich(id,input);return Map.of("message","Đã cập nhật suất lịch.");
    }
    @GetMapping("/tai-khoan")
    public Object taiKhoan(@RequestParam(defaultValue="") String tim,@RequestParam(defaultValue="0") int page) {
        return users.danhSach(tim,page);
    }
    @PostMapping("/tai-khoan") @ResponseStatus(HttpStatus.CREATED)
    public Object taoTaiKhoan(@Valid @RequestBody YeuCauTaiKhoan input) { return users.luu(null,input); }
    @PutMapping("/tai-khoan/{id}")
    public Object suaTaiKhoan(@PathVariable Long id,@Valid @RequestBody YeuCauTaiKhoan input) { return users.luu(id,input); }
    @GetMapping("/nhat-ky")
    public Object nhatKy(@RequestParam(defaultValue="0") int page) { return users.nhatKy(page); }
    @GetMapping("/tong-quan")
    public Object tongQuan(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate ngay) {
        expiry.quet();return reports.tongQuan(ngay);
    }
    @GetMapping("/bao-cao")
    public Object baoCao(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate denNgay) { return reports.tien(tuNgay,denNgay); }
    @GetMapping(value="/bao-cao.csv",produces="text/csv;charset=UTF-8")
    public ResponseEntity<String> csv(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate denNgay) {
        var result=reports.tien(tuNgay,denNgay);
        StringBuilder csv=new StringBuilder("\uFEFFNgay,Tien thu,Tien hoan,Thu rong\r\n");
        result.theoNgay().forEach(d -> csv.append(d.ngay()).append(',').append(d.thu().toPlainString()).append(',')
                .append(d.hoan().toPlainString()).append(',').append(d.rong().toPlainString()).append("\r\n"));
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=thu-hoan.csv").body(csv.toString());
    }
    @GetMapping("/thu-ca") public Object thuCa() { return reports.thuCa(); }
    @GetMapping("/tim-khach") public Object timKhach(@RequestParam String tim) { return desk.timKhach(tim); }
    @PostMapping("/dat-tai-quay") @ResponseStatus(HttpStatus.CREATED)
    public Object taiQuay(@Valid @RequestBody YeuCauDatTaiQuay input) { expiry.quet();return desk.tao(input); }
}
