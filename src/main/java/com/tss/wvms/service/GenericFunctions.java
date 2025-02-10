package com.tss.wvms.service;


import java.io.File;


import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class GenericFunctions {

    private static JdbcTemplate jdbcTemplate;

    @Autowired
    private JavaMailSender javaMailSender;

    public GenericFunctions(JdbcTemplate jdbcTemplate) {
        GenericFunctions.jdbcTemplate = jdbcTemplate;
    }

    public void sendMessage(String self, String msisdn, String transId, String msgContent) {
        String seqGen = "SELECT OUT_SMS_Q_SEQ.nextval FROM dual";
        int seqId = jdbcTemplate.queryForObject(seqGen, Integer.class);
        System.out.println("the seqID from the db fetched is:: " + seqId);
        int processId = 1;
        String fromMsisnd = "vms";
        int port = 123;

        Map<String, String> params = new HashMap<String, String>();

        try {

            String sql = "INSERT INTO OUT_SMS_Q (MSG_ID, MESSAGE, FROM_MSISDN, DEST_MSISDN) VALUES (?, ?, ?, ?)";

            System.out.println("Insert query: " + sql);

            jdbcTemplate.update(sql, seqId, msgContent, 1122, 1122);
            System.out.println("Message successfully inserted into out_sms_q with MSG_ID: " + seqId);
    
        } catch (Exception e) {
            System.out.println("exception in sql query::"+ e.getMessage());
        }
    }
        

    public String sendMail(
            String toEmailId,
            String fromEmailId,
            String alert,
            String subject,
            String resultFilePath,
            String cc,
            String bcc,
            String fromName) {
        String returnMessage = "1";

        try {
            // Format the date
            String date = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z (z)")
                    .format(new Date());

            // Sanitize email addresses
            toEmailId = toEmailId.replaceAll("\\s", "").replaceAll("\n", "").replaceAll(",", ", ");
            cc = cc != null ? cc.replaceAll("\\s", "").replaceAll("\n", "").replaceAll(",", ", ") : null;
            bcc = bcc != null ? bcc.replaceAll("\\s", "").replaceAll("\n", "").replaceAll(",", ", ") : null;

            // Create MimeMessage
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            // Set email details
            helper.setFrom(fromEmailId, fromName);
            helper.setTo(toEmailId.split(","));
            if (cc != null)
                helper.setCc(cc.split(","));
            if (bcc != null)
                helper.setBcc(bcc.split(","));
            helper.setSubject(subject);
            helper.setSentDate(new Date());

            // Email body
            if (resultFilePath == null || resultFilePath.isEmpty()) {
                // Plain email without attachment
                helper.setText(alert, true); // true for HTML content
            } else {
                // Email with attachment
                //String boundary = "PREMIUM-ATTACH-BOUNDARY------";
               // StringBuilder emailBody = new StringBuilder();
//                emailBody.append("MIME-Version: 1.0\n")
//                        .append("Content-Type: multipart/mixed; boundary=\"")
//                        .append(boundary)
//                        .append("\"\n")
//                        .append("\n--")
//                        .append(boundary)
//                        .append("\n")
//                        .append("Content-type: text/html\n\n")
//                        .append(alert)
//                        .append("\n");

                // File attachment processing
                File file = new File(resultFilePath);
                if (file.exists()) {
                    String encodedContent = encodeFileToBase64(file);
                    String contentType = java.nio.file.Files.probeContentType(file.toPath());

//                    emailBody.append("\n--")
//                            .append(boundary)
//                            .append("\n")
//                            .append("Content-Type: ")
//                            .append(contentType)
//                            .append("; name=\"")
//                            .append(file.getName())
//                            .append("\"\n")
//                            .append("Content-Transfer-Encoding: base64\n")
//                            .append("Content-Disposition: attachment; filename=\"")
//                            .append(file.getName())
//                            .append("\"\n\n")
//                            .append(encodedContent)
//                            .append("\n--")
//                            .append(boundary)
//                            .append("--\n");
                    helper.addAttachment(file.getName(), file);
                }
                //helper.setText(emailBody.toString(), true);
            }

            // Send email
            javaMailSender.send(message);

        } catch (MessagingException | IOException e) {
            e.printStackTrace();
            returnMessage = e.getMessage();
        }

        return returnMessage;
    }

    private String encodeFileToBase64(File file) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            byte[] bytes = fileInputStream.readAllBytes();
            return Base64.getEncoder().encodeToString(bytes);
        }
    }

}
