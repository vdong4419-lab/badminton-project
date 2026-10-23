package com.dong.dat_san.dto;
import jakarta.validation.constraints.*;
public record YeuCauEmail(@NotBlank @Email @Size(max=254) String email) {}
