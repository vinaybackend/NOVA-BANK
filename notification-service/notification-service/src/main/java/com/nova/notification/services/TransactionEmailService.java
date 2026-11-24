package com.nova.notification.services;

import com.nova.notification.consumer.TransactionEvent;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

@Service
public class TransactionEmailService {

    private final JavaMailSender mailSender;

    public TransactionEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendTransactionSuccessEmail(TransactionEvent event) {

        if (!"SUCCESS".equalsIgnoreCase(event.getTransactionStatus())) {
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(event.getEmail());

            String customerName = escape(event.getCustomerName());
            String transactionId = escape(event.getTransactionId());
            String reference = escape(event.getReference());
            String transactionType = escape(event.getTransactionType());
            String description = escape(event.getDescription());
            String fromAccount = maskAccount(event.getFromAccountNumber());
            String toAccount = maskAccount(event.getToAccountNumber());

            String currency = event.getCurrency() == null ? "INR" : event.getCurrency();

            NumberFormat formatter = NumberFormat.getNumberInstance(Locale.ENGLISH);
            formatter.setMinimumFractionDigits(2);
            formatter.setMaximumFractionDigits(2);
            formatter.setRoundingMode(RoundingMode.HALF_UP);

            String amount = escape(currency) + " " + formatter.format(event.getAmount());

            String html = """
                    <!DOCTYPE html>
                    <html>
                    <body style="margin:0;padding:20px;background:#f2f5f7;
                                 font-family:Arial,sans-serif;color:#24312d;">
                      <div style="max-width:600px;margin:auto;background:#ffffff;
                                  border:1px solid #dddddd;border-radius:10px;
                                  overflow:hidden;">
                        <div style="background:#073b32;color:#ffffff;
                                    padding:28px;text-align:center;">
                          <h1 style="margin:0;">NOVA BANK</h1>
                          <p>Banking made simpler. Banking made safer.</p>
                          <h2>Transaction Successful!</h2>
                        </div>

                        <div style="padding:25px;">
                          <p>Dear %s,</p>
                          <p>Your transaction has been completed successfully.</p>

                          <div style="background:#eef7f1;padding:20px;
                                      text-align:center;border-radius:8px;">
                            <p style="margin:0;color:#52665c;">Amount transferred</p>
                            <h1 style="color:#087443;margin:10px 0;">%s</h1>
                            <strong style="color:#087443;">SUCCESSFUL</strong>
                          </div>

                          <h3>Transaction Details</h3>
                          <table style="width:100%%;border-collapse:collapse;">
                            <tr><td style="padding:9px;border-bottom:1px solid #eee;">Transaction ID</td>
                                <td style="padding:9px;border-bottom:1px solid #eee;">%s</td></tr>
                            <tr><td style="padding:9px;border-bottom:1px solid #eee;">Type</td>
                                <td style="padding:9px;border-bottom:1px solid #eee;">%s</td></tr>
                            <tr><td style="padding:9px;border-bottom:1px solid #eee;">From account</td>
                                <td style="padding:9px;border-bottom:1px solid #eee;">%s</td></tr>
                            <tr><td style="padding:9px;border-bottom:1px solid #eee;">To account</td>
                                <td style="padding:9px;border-bottom:1px solid #eee;">%s</td></tr>
                            <tr><td style="padding:9px;border-bottom:1px solid #eee;">Reference</td>
                                <td style="padding:9px;border-bottom:1px solid #eee;">%s</td></tr>
                            <tr><td style="padding:9px;">Description</td>
                                <td style="padding:9px;">%s</td></tr>
                          </table>

                          <p style="margin-top:25px;font-size:13px;color:#555;">
                            Never share your PIN, password, or OTP with anyone.
                            If you did not authorize this transaction, contact
                            Nova Bank through its official support channel.
                          </p>

                          <p>Regards,<br><strong>Nova Bank Team</strong></p>
                        </div>
                      </div>
                    </body>
                    </html>
                    """.formatted(
                    customerName,
                    amount,
                    transactionId,
                    transactionType,
                    fromAccount,
                    toAccount,
                    reference,
                    description
            );
            helper.setSubject("Nova Bank - Transaction Successful");
            helper.setText(html, true);

            mailSender.send(message);

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to send transaction email for transaction ID: "
                            + event.getTransactionId(),
                    exception
            );
        }
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "-" : value);
    }

    private String maskAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return "-";
        }

        if (accountNumber.length() <= 4) {
            return "****";
        }

        return "******" + accountNumber.substring(
                accountNumber.length() - 4
        );
    }
}
