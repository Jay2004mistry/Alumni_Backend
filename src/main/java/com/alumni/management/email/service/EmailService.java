package com.alumni.management.email.service;

import com.alumni.management.verification.enums.VerificationPurpose;
import com.alumni.management.verification.exception.EmailDeliveryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:your_email@gmail.com}")
    private String fromEmail;

    /**
     * Send professional email verification OTP for account registration/activation.
     * Never logs sensitive OTP values to ensure production security.
     */
    public void sendVerificationOtpEmail(String toEmail, String otp, VerificationPurpose purpose) {
        if (mailSender == null) {
            logger.error("JavaMailSender bean is not configured.");
            throw new EmailDeliveryException("Mail service is currently unavailable. Please contact the administrator.");
        }

        if (fromEmail == null || fromEmail.isBlank() || "your_email@gmail.com".equalsIgnoreCase(fromEmail.trim())) {
            logger.error("SMTP credentials not configured: spring.mail.username is missing or set to placeholder. Please set MAIL_USERNAME and MAIL_PASSWORD environment variables or configure application-local.properties.");
            throw new EmailDeliveryException("Mail service is not configured with valid credentials. Please configure MAIL_USERNAME and MAIL_PASSWORD.");
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);

            String subject;
            if (purpose == VerificationPurpose.ACCOUNT_REGISTRATION) {
                subject = "GLS Connect - Account Registration Verification";
            } else if (purpose == VerificationPurpose.ACCOUNT_ACTIVATION) {
                subject = "GLS Connect - Account Activation Verification";
            } else {
                subject = "GLS Connect - Verification Code";
            }
            message.setSubject(subject);

            String text = "Dear User,\n\n"
                    + "Thank you for using GLS Connect.\n\n"
                    + "Your One-Time Password (OTP) for verification is:\n\n"
                    + "  " + otp + "\n\n"
                    + "This code is valid for 5 minutes. Please do not share this code with anyone.\n\n"
                    + "If you did not request this verification, please ignore this email.\n\n"
                    + "Best regards,\n"
                    + "GLS Connect Team";
            message.setText(text);

            mailSender.send(message);
            logger.info("Verification email sent successfully for purpose: {}", purpose);
        } catch (Exception e) {
            logger.error("Failed to send verification email: {}", e.getMessage(), e);
            throw new EmailDeliveryException("Failed to send verification email. Please try again later.", e);
        }
    }

    /**
     * Existing forgot-password OTP email sender preserved for backward compatibility.
     */
    public void sendOtpEmail(String toEmail, String otp) {
        if (mailSender == null) {
            logger.warn("JavaMailSender bean is not configured properly.");
            return;
        }

        if (fromEmail == null || fromEmail.isBlank() || "your_email@gmail.com".equalsIgnoreCase(fromEmail.trim())) {
            logger.error("SMTP credentials not configured: spring.mail.username is missing or set to placeholder. Please set MAIL_USERNAME and MAIL_PASSWORD.");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("GLS Connect - Password Reset OTP");
            message.setText("Dear User,\n\n"
                    + "Your One-Time Password (OTP) for resetting your password on GLS Connect is:\n\n"
                    + "  " + otp + "\n\n"
                    + "This OTP is valid for 15 minutes. Please do not share this OTP with anyone.\n\n"
                    + "Best regards,\n"
                    + "GLS Connect Team");

            mailSender.send(message);
            logger.info("Password reset OTP email sent successfully.");
        } catch (Exception e) {
            logger.error("Failed to send password reset OTP email: {}", e.getMessage());
        }
    }
}
