package com.dong.dat_san.controller;

import com.dong.dat_san.dto.*;
import com.dong.dat_san.service.DatSanService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dat-san")
public class DatSanController {
    private final DatSanService service;

    public DatSanController(DatSanService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<DatSanDto> taoDatSan(@Valid @RequestBody YeuCauDatSan request, Principal principal) {
        return ResponseEntity.status(201).body(service.taoDatSan(principal.getName(), request.getDanhSachLichSanIds()));
    }

    @GetMapping("/lich-su")
    public List<DatSanDto> xemLichSuDatSan(Principal principal) {
        return service.xemLichSu(principal.getName());
    }

    @PutMapping("/{id}/huy")
    public String huyDatSan(@PathVariable Long id, Principal principal,
                            @Valid @RequestBody(required = false) YeuCauHuyDatSan request) {
        return service.huyDatSan(principal.getName(), id, request == null ? null : request.lyDoHuy());
    }
}
