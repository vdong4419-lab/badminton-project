package com.dong.dat_san.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DichVuGuiMail {
    private final JavaMailSender sender;
    private final boolean enabled;
    private final String from;
    public DichVuGuiMail(JavaMailSender sender,@Value("${app.mail.enabled:false}") boolean enabled,
                        @Value("${spring.mail.username}") String from) {
        this.sender=sender;this.enabled=enabled;this.from=from;
    }
    public void guiOtp(String email,String code,String purpose) {
        if (!enabled) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Gửi email chưa được cấu hình. Vui lòng liên hệ chủ sân.");
        boolean reset="DAT_LAI_MAT_KHAU".equals(purpose);
        var message=new SimpleMailMessage();
        message.setFrom(from); message.setTo(email);
        message.setSubject("BadmintonHub - "+(reset ? "Mã đặt lại mật khẩu" : "Xác minh email đăng ký"));
        message.setText("Xin chào,\n\nMã OTP "+(reset ? "đặt lại mật khẩu" : "xác minh email")+" của bạn là: "+code
                +"\n\nMã có hiệu lực 10 phút và chỉ dùng một lần. Không chia sẻ mã này với người khác."
                +"\nNếu bạn không yêu cầu thao tác này, hãy bỏ qua email.\n\nBadmintonHub");
        try { sender.send(message); }
        catch (MailException error) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Chưa gửi được email. Vui lòng thử lại sau."); }
    }
}
