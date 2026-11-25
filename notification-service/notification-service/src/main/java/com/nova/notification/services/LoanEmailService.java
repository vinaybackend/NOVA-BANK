package com.nova.notification.services;

import com.nova.notification.consumer.LoanEvent;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class LoanEmailService {
    private final JavaMailSender mailSender;

    public LoanEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendLoanEmail(LoanEvent event) {
        if (event.getEmail() == null || event.getEmail().isBlank()) return;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(event.getEmail());
            helper.setSubject(subjectFor(event));
            helper.setText(htmlFor(event), true);
            mailSender.send(message);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to send loan email", exception);
        }
    }

    private String subjectFor(LoanEvent event) {
        return switch (event.getEventType()) {
            case "LOAN_APPROVED" -> "Nova Bank - Loan Approved";
            case "LOAN_DISBURSED" -> "Nova Bank - Loan Amount Disbursed";
            case "EMI_PAYMENT_SUCCESS" -> "Nova Bank - EMI Payment Successful";
            case "EMI_REMINDER" -> "Nova Bank - EMI Payment Reminder";
            case "LOAN_APPLICATION_SUBMITTED" -> "Nova Bank - Loan Application Received";
            case "LOAN_OFFER_ACCEPTED" -> "Nova Bank - Loan Offer Accepted";
            case "LOAN_AGREEMENT_ACCEPTED" -> "Nova Bank - Loan Agreement Accepted";
            case "LOAN_REJECTED" -> "Nova Bank - Loan Application Update";
            case "LOAN_RETURNED" -> "Nova Bank - Additional Review Required";
            case "ADDITIONAL_DOCUMENTS_REQUESTED" -> "Nova Bank - Documents Required for Loan";
            default -> "Nova Bank - Loan Notification";
        };
    }

    private String htmlFor(LoanEvent event) {
        String title = switch (event.getEventType()) {
            case "LOAN_APPROVED" -> "Loan Approved";
            case "LOAN_DISBURSED" -> "Loan Amount Disbursed";
            case "EMI_PAYMENT_SUCCESS" -> "EMI Payment Successful";
            case "EMI_REMINDER" -> "EMI Payment Reminder";
            case "LOAN_APPLICATION_SUBMITTED" -> "Loan Application Received";
            case "LOAN_OFFER_ACCEPTED" -> "Loan Offer Accepted";
            case "LOAN_AGREEMENT_ACCEPTED" -> "Loan Agreement Accepted";
            case "LOAN_REJECTED" -> "Loan Application Update";
            case "LOAN_RETURNED" -> "Additional Review Required";
            case "ADDITIONAL_DOCUMENTS_REQUESTED" -> "Documents Required for Loan";
            default -> "Loan Notification";
        };
        String due = event.getDueDate() == null ? "" : "<p><strong>Due date:</strong> " + event.getDueDate() + "</p>";
        String amount = event.getAmount() == null ? "" : "<p><strong>Amount:</strong> INR " + event.getAmount() + "</p>";
        return """
            <!DOCTYPE html><html><body style='font-family:Arial,sans-serif'>
            <div style='max-width:600px;margin:auto;border:1px solid #ddd;border-radius:8px;padding:24px'>
            <h2 style='color:#073b32'>NOVA BANK</h2><h3>%s</h3>
            <p>Dear %s,</p><p>%s</p>%s%s
            <p>Regards,<br><strong>Nova Bank Team</strong></p>
            </div></body></html>
            """.formatted(title,
                event.getCustomerName() == null ? "Customer" : event.getCustomerName(),
                event.getMessage() == null ? "There is an update on your loan account." : event.getMessage(),
                amount, due);
    }
}