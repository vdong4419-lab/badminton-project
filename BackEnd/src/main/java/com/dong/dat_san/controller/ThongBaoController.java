package com.dong.dat_san.controller;
import com.dong.dat_san.service.DichVuThongBao;
import java.security.Principal;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/thong-bao")
public class ThongBaoController {
    private final DichVuThongBao service;
    public ThongBaoController(DichVuThongBao service) { this.service=service; }
    @GetMapping public Object danhSach(Principal p,@RequestParam(defaultValue="0") int page) { return service.danhSach(p.getName(),page); }
    @PutMapping("/{id}/da-doc") public Object doc(Principal p,@PathVariable Long id) { service.doc(p.getName(),id);return Map.of("message","Đã đọc thông báo."); }
    @PutMapping("/doc-tat-ca") public Object docTatCa(Principal p) { service.docTatCa(p.getName());return Map.of("message","Đã đánh dấu tất cả là đã đọc."); }
}
