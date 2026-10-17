package com.dong.dat_san.controller;

import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class XuLyLoi {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> nghiepVu(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode())
                .body(Map.of("message", error.getReason() == null ? "Yêu cầu không hợp lệ." : error.getReason()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> xacThuc(AuthenticationException error) {
        return ResponseEntity.status(401).body(Map.of("message", "Email hoặc mật khẩu không chính xác, hoặc tài khoản đã bị khóa."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> duLieu(MethodArgumentNotValidException error) {
        String message = error.getBindingResult().getFieldErrors().stream()
                .map(field -> field.getDefaultMessage()).findFirst().orElse("Dữ liệu không hợp lệ.");
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<?> dinhDang(Exception error) {
        return ResponseEntity.badRequest().body(Map.of("message", "Dữ liệu hoặc định dạng ngày không hợp lệ."));
    }

    @ExceptionHandler({DataIntegrityViolationException.class, PessimisticLockingFailureException.class})
    public ResponseEntity<?> xungDot(Exception error) {
        return ResponseEntity.status(409).body(Map.of("message", "Dữ liệu bị trùng hoặc suất vừa được người khác đặt. Vui lòng tải lại."));
    }
}
