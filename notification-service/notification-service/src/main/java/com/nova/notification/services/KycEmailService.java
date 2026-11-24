
package com.nova.notification.services;

import com.nova.notification.consumer.KycEvent;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.format.DateTimeFormatter;

@Service
public class KycEmailService {

    private final JavaMailSender mailSender;

    public KycEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendAccountOpeningRequestEmail(KycEvent event) {

        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(event.getEmail());

            helper.setSubject(
                    "Nova Bank - Account Opening Request Submitted Successfully"
            );

            String name = escape(
                    event.getName() != null
                            ? event.getName()
                            : "Customer"
            );

            String referenceId = escape(
                    event.getKycId() != null
                            ? event.getKycId()
                            : "Not available"
            );

            String branchName = escape(
                    event.getBranchName() != null
                            ? event.getBranchName()
                            : "Not assigned"
            );

            String status = escape(
                    event.getVerificationStatus() != null
                            ? event.getVerificationStatus()
                            : "PENDING"
            );

            String submittedAt = event.getCreatedAt() != null
                    ? event.getCreatedAt().format(
                    DateTimeFormatter.ofPattern(
                            "dd MMM yyyy, hh:mm a"
                    )
            )
                    : "Not available";

            String htmlContent = """
                    <!DOCTYPE html>
                    <html>
                    <body style="margin:0;padding:20px;
                                 background-color:#f3f5f7;
                                 font-family:Arial,sans-serif;">

                      <div style="max-width:600px;margin:auto;
                                  background:#ffffff;
                                  border:1px solid #e5e7eb;
                                  border-radius:10px;overflow:hidden;">

                        <div style="background:#0b3b2e;
                                    color:#ffffff;
                                    padding:28px;text-align:center;">

                          <h1 style="margin:0;letter-spacing:2px;">
                            NOVA BANK
                          </h1>

                          <p style="margin-bottom:0;">
                            Banking made simpler. Banking made safer.
                          </p>
                        </div>

                        <div style="padding:30px;color:#263238;">

                          <h2 style="color:#0b3b2e;">
                            Account Opening Request Submitted!
                          </h2>

                          <p>Dear %s,</p>

                          <p>
                            Thank you for choosing Nova Bank!
                            We have successfully received your account
                            opening request along with your KYC
                            application and documents.
                          </p>

                          <p>
                            Your request is currently awaiting
                            verification. Our team will review your
                            KYC details before making a decision
                            regarding your account opening request.
                          </p>

                          <div style="background:#f3f7f5;
                                      padding:20px;border-radius:8px;
                                      margin:24px 0;">

                            <h3 style="color:#0b3b2e;margin-top:0;">
                              Request Details
                            </h3>

                            <table style="width:100%%;
                                          border-collapse:collapse;">

                              <tr>
                                <td style="padding:10px 0;
                                           color:#64748b;">
                                  Reference ID
                                </td>

                                <td style="padding:10px 0;
                                           text-align:right;
                                           font-weight:bold;">
                                  %s
                                </td>
                              </tr>

                              <tr>
                                <td style="padding:10px 0;
                                           color:#64748b;">
                                  Request Status
                                </td>

                                <td style="padding:10px 0;
                                           text-align:right;
                                           color:#b7791f;
                                           font-weight:bold;">
                                  %s
                                </td>
                              </tr>

                              <tr>
                                <td style="padding:10px 0;
                                           color:#64748b;">
                                  Preferred Branch
                                </td>

                                <td style="padding:10px 0;
                                           text-align:right;">
                                  %s
                                </td>
                              </tr>

                              <tr>
                                <td style="padding:10px 0;
                                           color:#64748b;">
                                  Submitted On
                                </td>

                                <td style="padding:10px 0;
                                           text-align:right;">
                                  %s
                                </td>
                              </tr>

                            </table>
                          </div>

                          <h3 style="color:#0b3b2e;">
                            What happens next?
                          </h3>

                          <p>
                            1. Our team will verify your KYC documents.
                          </p>

                          <p>
                            2. You will be notified when your request
                            is approved or if additional information
                            is required.
                          </p>

                          <p>
                            3. After approval, your account will be
                            opened according to Nova Bank's process.
                          </p>

                          <p>
                            Please keep your reference ID for future
                            communication.
                          </p>

                          <p>
                            <strong>Important:</strong> This email
                            confirms receipt of your request only.
                            Your bank account has not yet been opened.
                          </p>

                          <p>
                            Thank you for choosing Nova Bank.
                          </p>

                          <p>
                            Regards,<br>
                            <strong>Nova Bank Team</strong>
                          </p>

                        </div>

                        <div style="background:#f8fafc;
                                    padding:16px;text-align:center;
                                    color:#64748b;font-size:12px;">

                          This is an automated email.
                          Please do not reply to this message.<br><br>

                          For your security, never share your OTP,
                          PIN, or password with anyone.

                        </div>

                      </div>
                    </body>
                    </html>
                    """.formatted(
                    name,
                    referenceId,
                    status,
                    branchName,
                    submittedAt
            );

            helper.setText(htmlContent, true);

            mailSender.send(message);

            System.out.println(
                    "Account opening request email sent to: "
                            + event.getEmail()
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to send account opening request email for KYC ID: "
                            + event.getKycId(),
                    e
            );
        }
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(value);
    }
}
