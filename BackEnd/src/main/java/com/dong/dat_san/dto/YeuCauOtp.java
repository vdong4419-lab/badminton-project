package com.dong.dat_san.dto;
import jakarta.validation.constraints.*;
public record YeuCauOtp(@NotBlank @Email @Size(max=254) String email,
                       @NotBlank @Pattern(regexp="[0-9]{6}",message="OTP phải gồm 6 chữ số") String otp) {
    @Override public String toString() { return "YeuCauOtp[REDACTED]"; }
}
