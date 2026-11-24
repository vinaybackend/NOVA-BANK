package com.nova.notification.services;
import com.nova.notification.consumer.AccountEvent;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class AccountEmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.login-url}")
    private String loginUrl;

    public AccountEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendAccountOpenedEmail(AccountEvent event) {

        if (event.getEmail() == null || event.getEmail().isBlank()) {
            throw new IllegalArgumentException(
                    "Customer email is missing from AccountEvent"
            );
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(event.getEmail());
            helper.setSubject(
                    "Nova Bank - Your Account Has Been Opened Successfully"
            );

            String customerName = escape(event.getName());
            String accountNumber = escape(event.getAccountNumber());
            String branchName = escape(event.getBranchName());
            String ifsc = escape(event.getIfsc());
            String username = escape(event.getUsername());
            String password = escape(event.getPassword());
            String safeLoginUrl = escape(loginUrl);

            String balance = event.getBalance() == null
                    ? "Not provided"
                    : NumberFormat.getCurrencyInstance(
                            Locale.forLanguageTag("en-IN"))
                    .format(event.getBalance());

            String createdAt = event.getCreatedAt() == null
                    ? "Not provided"
                    : event.getCreatedAt().format(
                    DateTimeFormatter.ofPattern(
                            "dd MMM yyyy, hh:mm a"
                    )
            );

            String loginSection;

            if (username.isBlank()) {
                loginSection = """
                        <p>
                            Your online banking username is not available
                            in this notification. Please contact Nova Bank
                            through its official support channel.
                        </p>
                        """;
            } else {
                loginSection = """
                        <table style="width:100%%;
                                      border-collapse:collapse;">
                            <tr>
                                <td style="padding:10px;
                                           border-bottom:1px solid #ddd;">
                                    Username
                                </td>
                                <td style="padding:10px;
                                           border-bottom:1px solid #ddd;">
                                    <b>%s</b>
                                </td>
                            </tr>
                            <tr>
                                <td style="padding:10px;
                                           border-bottom:1px solid #ddd;">
                                    Password
                                </td>
                                <td style="padding:10px;
                                           border-bottom:1px solid #ddd;">
                                    <b>%s</b>
                                </td>
                            </tr>
                        </table>

                        <p>
                            Use the button below to access online banking.
                        </p>

                        <div style="text-align:center;margin:28px 0;">
                            <a href="%s"
                               style="background:#123b70;color:#ffffff;
                                      padding:14px 26px;
                                      text-decoration:none;
                                      border-radius:6px;
                                      display:inline-block;
                                      font-weight:bold;">
                                Access Nova Bank
                            </a>
                        </div>
                        """.formatted(username, password, safeLoginUrl);
            }

            String html = """
                    <!DOCTYPE html>
                    <html>
                    <body style="margin:0;padding:24px;
                                 background:#f1f4f8;
                                 font-family:Arial,sans-serif;">

                      <div style="max-width:600px;margin:auto;
                                  background:#ffffff;
                                  border-radius:12px;overflow:hidden;
                                  border:1px solid #e5e7eb;">

                        <div style="background:#123b70;
                                    padding:24px;color:#ffffff;">
                          <h1 style="margin:0;">NOVA BANK</h1>
                          <p style="margin-bottom:0;">
                            Your banking journey starts here
                          </p>
                        </div>

                        <div style="padding:28px;">

                          <h2 style="color:#16834a;">
                            Account Opened Successfully!
                          </h2>

                          <p>Dear %s,</p>

                          <p>
                            Congratulations! Your Nova Bank account
                            has been opened successfully.
                          </p>

                          <p>
                            Your account details are provided below.
                            You can use your online banking credentials
                            to access the services available to you.
                          </p>

                          <h3>Account Details</h3>

                          <table style="width:100%%;
                                        border-collapse:collapse;">
                            <tr>
                              <td style="padding:10px;
                                         border-bottom:1px solid #ddd;">
                                Account Number
                              </td>
                              <td style="padding:10px;
                                         border-bottom:1px solid #ddd;">
                                <b>%s</b>
                              </td>
                            </tr>

                            <tr>
                              <td style="padding:10px;
                                         border-bottom:1px solid #ddd;">
                                Account Balance
                              </td>
                              <td style="padding:10px;
                                         border-bottom:1px solid #ddd;">
                                <b>%s</b>
                              </td>
                            </tr>

                            <tr>
                              <td style="padding:10px;
                                         border-bottom:1px solid #ddd;">
                                Branch
                              </td>
                              <td style="padding:10px;
                                         border-bottom:1px solid #ddd;">
                                %s
                              </td>
                            </tr>

                            <tr>
                              <td style="padding:10px;
                                         border-bottom:1px solid #ddd;">
                                IFSC
                              </td>
                              <td style="padding:10px;
                                         border-bottom:1px solid #ddd;">
                                %s
                              </td>
                            </tr>

                            <tr>
                              <td style="padding:10px;">
                                Account Created At
                              </td>
                              <td style="padding:10px;">
                                %s
                              </td>
                            </tr>
                          </table>

                          <h3 style="color:#123b70;">
                            Online Banking Login
                          </h3>

                          %s

                          <p style="margin-top:24px;">
                            For your security, never share your password,
                            OTP, PIN, or debit card details with anyone.
                          </p>

                          <p>
                            Regards,<br>
                            <b>Nova Bank Team</b>
                          </p>

                        </div>

                        <div style="background:#f8fafc;padding:16px;
                                    text-align:center;color:#64748b;
                                    font-size:12px;">
                          This is an automated email.
                          Please do not reply to this message.
                        </div>

                      </div>
                    </body>
                    </html>
                    """.formatted(
                    customerName,
                    accountNumber,
                    balance,
                    branchName,
                    ifsc,
                    createdAt,
                    loginSection
            );

            helper.setText(html, true);
            mailSender.send(message);

            System.out.println(
                    "Account opening email sent to customer: "
                            + event.getEmail()
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to send account-opening email",
                    e
            );
        }
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(
                value == null ? "" : value
        );
    }
}
