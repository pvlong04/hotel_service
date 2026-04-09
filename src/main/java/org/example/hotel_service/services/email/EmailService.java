package org.example.hotel_service.services.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.hotel_service.entities.Reservation;
import org.example.hotel_service.entities.ReservationItem;
import org.example.hotel_service.entities.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:no-reply@hotel.local}")
    private String fromEmail;

    public void sendVerificationEmail(User user, String verificationLink) {
        if (!mailEnabled || user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(user.getEmail());
        message.setSubject("Xac nhan tai khoan khach san");
        message.setText("Xin chao " + safe(user.getProfile() != null ? user.getProfile().getFullName() : user.getUsername())
                + ",\n\n"
                + "Cam on ban da dang ky tai khoan. Vui long bam vao link ben duoi de kich hoat tai khoan:\n"
                + verificationLink + "\n\n"
                + "Link co hieu luc trong 24 gio.\n"
                + "Neu ban khong thuc hien thao tac nay, vui long bo qua email nay.");

        mailSender.send(message);
    }

    public void sendResetPasswordEmail(User user, String resetLink) {
        if (!mailEnabled || user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(user.getEmail());
        message.setSubject("Dat lai mat khau tai khoan khach san");
        message.setText("Xin chao " + safe(user.getProfile() != null ? user.getProfile().getFullName() : user.getUsername())
                + ",\n\n"
                + "Chung toi da nhan duoc yeu cau dat lai mat khau cho tai khoan cua ban.\n"
                + "Vui long bam vao link ben duoi de dat lai mat khau:\n"
                + resetLink + "\n\n"
                + "Link co hieu luc trong thoi gian ngan.\n"
                + "Neu ban khong yeu cau dat lai mat khau, vui long bo qua email nay.");

        mailSender.send(message);
    }

    public void sendBookingConfirmationEmail(Reservation reservation) {
        if (!mailEnabled || reservation == null || reservation.getGuest() == null) {
            return;
        }

        User guest = reservation.getGuest();
        if (guest.getEmail() == null || guest.getEmail().isBlank()) {
            return;
        }

        StringBuilder content = new StringBuilder();
        content.append("Xin chao ")
                .append(safe(guest.getProfile() != null ? guest.getProfile().getFullName() : guest.getUsername()))
                .append(",\n\n")
                .append("Dat phong cua ban da duoc tao thanh cong.\n")
                .append("Ma dat phong: ").append(safe(reservation.getReservationCode())).append("\n")
                .append("Ngay nhan phong: ").append(formatDate(reservation.getCheckInDate())).append("\n")
                .append("Ngay tra phong: ").append(formatDate(reservation.getCheckOutDate())).append("\n")
                .append("Tong tien tam tinh: ").append(safeNumber(reservation.getTotalAmount())).append(" VND\n")
                .append("Trang thai: ").append(reservation.getStatus() != null ? reservation.getStatus().name() : "PENDING")
                .append("\n\nChi tiet phong:\n");

        for (ReservationItem item : reservation.getItems()) {
            content.append("- Phong ")
                    .append(item.getRoom() != null ? safe(item.getRoom().getRoomNumber()) : "N/A")
                    .append(" | Loai ")
                    .append(item.getRoomType() != null ? safe(item.getRoomType().getName()) : "N/A")
                    .append(" | ")
                    .append(safeNumber(item.getAmount()))
                    .append(" VND\n ");
        }

        content.append("\nCam on ban da su dung dich vu cua chung toi.");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(guest.getEmail());
        message.setSubject("Xac nhan dat phong #" + safe(reservation.getReservationCode()));
        message.setText(content.toString());

        mailSender.send(message);
    }

    private String formatDate(java.time.LocalDate date) {
        return date == null ? "N/A" : DATE_FORMATTER.format(date);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private long safeNumber(Long value) {
        return value == null ? 0L : value;
    }
}

