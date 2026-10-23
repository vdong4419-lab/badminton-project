package com.dong.dat_san.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class CauHinhBaoMat {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, BoLocXacThucJwt jwtFilter) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/quan-tri/don-dat/**", "/api/quan-tri/don-dat", "/api/quan-tri/lich-san", "/api/quan-tri/thu-ca", "/api/quan-tri/dat-tai-quay", "/api/quan-tri/tim-khach").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/quan-tri/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/xac-thuc/dang-nhap", "/api/xac-thuc/dang-ky",
                                "/api/xac-thuc/gui-lai-otp", "/api/xac-thuc/xac-minh-email",
                                "/api/xac-thuc/quen-mat-khau", "/api/xac-thuc/dat-lai-mat-khau").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/san", "/api/san/**", "/api/lich-san/trong").permitAll()
                        .requestMatchers("/api/san", "/api/san/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) ->
                                ghiLoi(response, 401, "Vui lòng đăng nhập lại để tiếp tục."))
                        .accessDeniedHandler((request, response, exception) ->
                                ghiLoi(response, 403, "Bạn không có quyền thực hiện thao tác này.")))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    // Chỉ chạy trong chuỗi Spring Security, tránh đăng ký lần hai ở servlet container.
    @Bean
    public FilterRegistrationBean<BoLocXacThucJwt> jwtFilterRegistration(BoLocXacThucJwt filter) {
        FilterRegistrationBean<BoLocXacThucJwt> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    private static void ghiLoi(HttpServletResponse response, int status, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
