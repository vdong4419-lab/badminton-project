package com.dong.dat_san;
import com.dong.dat_san.service.DichVuGuiMail;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DichVuGuiMailTests {
    @Test void sendsOtpFromConfiguredGmail() {
        var sender=mock(JavaMailSender.class);
        new DichVuGuiMail(sender,true,"vdong4419@gmail.com").guiOtp("customer@example.com","123456","XAC_MINH_EMAIL");
        var captor=org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);verify(sender).send(captor.capture());
        assertThat(captor.getValue().getFrom()).isEqualTo("vdong4419@gmail.com");
        assertThat(captor.getValue().getTo()).containsExactly("customer@example.com");
        assertThat(captor.getValue().getText()).contains("123456","10 phút");
    }
    @Test void disabledMailDoesNotSendOrExposeCode() {
        var sender=mock(JavaMailSender.class);
        assertThatThrownBy(() -> new DichVuGuiMail(sender,false,"vdong4419@gmail.com").guiOtp("a@example.com","123456","XAC_MINH_EMAIL"))
                .isInstanceOf(ResponseStatusException.class).hasMessageNotContaining("123456");
        verifyNoInteractions(sender);
    }
    @Test void smtpErrorsAreNotExposedToCustomers() {
        var sender=mock(JavaMailSender.class);doThrow(new MailSendException("secret-password-from-server")).when(sender).send(any(SimpleMailMessage.class));
        assertThatThrownBy(() -> new DichVuGuiMail(sender,true,"vdong4419@gmail.com").guiOtp("a@example.com","123456","DAT_LAI_MAT_KHAU"))
                .isInstanceOf(ResponseStatusException.class).hasMessageNotContaining("secret-password");
    }
}
