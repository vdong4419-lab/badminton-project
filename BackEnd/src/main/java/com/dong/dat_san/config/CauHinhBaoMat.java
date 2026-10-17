package com.dong.dat_san.config;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.Customizer;
@Configuration
@EnableWebSecurity
public class CauHinhBaoMat {

    @Autowired
    private BoLocXacThucJwt boLocXacThucJwt;

            @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                .csrf(csrf -> csrf.disable()) // Tắt CSRF vì dùng JWT
                .cors(Customizer.withDefaults()) // Kích hoạt CORS đã cấu hình
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/xac-thuc/**", "/api/san/**").permitAll() // Cho phép mọi người gọi API đăng ký/đăng nhập/xem sân
                    .anyRequest().authenticated() // Các API còn lại yêu cầu có token
                );
            return http.build();
        }
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}