package com.dong.dat_san.controller;

import com.dong.dat_san.dto.*;
import com.dong.dat_san.service.DichVuHoSo;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ho-so")
public class HoSoController {
    private final DichVuHoSo service;
    public HoSoController(DichVuHoSo service) { this.service=service; }
    @GetMapping public DichVuHoSo.HoSo xem(Principal principal) { return service.xem(principal.getName()); }
    @PutMapping public DichVuHoSo.HoSo sua(Principal principal, @Valid @RequestBody YeuCauHoSo request) {
        return service.sua(principal.getName(),request);
    }
    @PutMapping("/mat-khau") public Map<String,String> doiMatKhau(Principal principal, @Valid @RequestBody YeuCauDoiMatKhau request) {
        service.doiMatKhau(principal.getName(),request);
        return Map.of("message","Đã đổi mật khẩu. Vui lòng đăng nhập bằng mật khẩu mới.");
    }
}
