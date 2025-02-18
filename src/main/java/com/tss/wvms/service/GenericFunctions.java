package com.tss.wvms.service;

import java.io.BufferedWriter;
import java.io.File;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GenericFunctions {

    private static JdbcTemplate jdbcTemplate;

    @Value("${VMS_LOG_PATH}")
    private String vmsLogPath;

    @Value("${VMS_HOME}")
    private String vmsHome;

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
            System.out.println("exception in sql query::" + e.getMessage());
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
            // toEmailId = toEmailId.replaceAll("\\s", "").replaceAll("\n",
            // "").replaceAll(",", ", ");
            // cc = cc != null ? cc.replaceAll("\\s", "").replaceAll("\n",
            // "").replaceAll(",", ", ") : null;
            // bcc = bcc != null ? bcc.replaceAll("\\s", "").replaceAll("\n",
            // "").replaceAll(",", ", ") : null;
            // Sanitize email addresses
            toEmailId = toEmailId.replaceAll("\\s", "").replaceAll("\n", "").replaceAll(",", ", ");
            cc = (cc != null && !cc.isBlank()) ? cc.replaceAll("\\s", "").replaceAll("\n", "").replaceAll(",", ", ")
                    : null;
            bcc = (bcc != null && !bcc.isBlank()) ? bcc.replaceAll("\\s", "").replaceAll("\n", "").replaceAll(",", ", ")
                    : null;

            // Create MimeMessage
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            // Set email details
            helper.setFrom(fromEmailId, fromName);
            helper.setTo(toEmailId.split(","));
            // if (cc != null)
            // helper.setCc(cc.split(","));
            // if (bcc != null)
            // helper.setBcc(bcc.split(","));
            if (cc != null && !cc.isEmpty()) {
                helper.setCc(cc.split(","));
            }

            if (bcc != null && !bcc.isEmpty()) {
                helper.setBcc(bcc.split(","));
            }
            helper.setSubject(subject);
            helper.setSentDate(new Date());

            // Email body
            if (resultFilePath == null || resultFilePath.isEmpty()) {
                // Plain email without attachment
                helper.setText(alert, true); // true for HTML content
            } else {
                // Email with attachment
                StringBuilder emailBody = new StringBuilder();

                // File attachment processing
                File file = new File(resultFilePath);
                if (file.exists()) {
                    String encodedContent = encodeFileToBase64(file);
                    String contentType = java.nio.file.Files.probeContentType(file.toPath());

                    helper.addAttachment(file.getName(), file);
                }
                helper.setText(emailBody.toString(), true);
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

    public void logFunction(String fileName, String contentToFile) {
    	log.info("::::::log:::::::"+contentToFile);
        fileName = fileName.replaceAll("\\s", ""); // Remove spaces from filename

        // Get current date for log file name
        String logDate = new SimpleDateFormat("dd-MM-yyyy").format(new Date());

        // Get timestamp with milliseconds
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss:SSS");
        String logHours = timeFormat.format(new Date());

        if (vmsHome == null || vmsLogPath == null) {
			System.err.println("Environment variables VMS_HOME or VMS_LOG_PATH are not set!");
            return;
        }

        String logFilePath = vmsHome + "/" + vmsLogPath + "/" + logDate + "-" + fileName;
        //log.info("[logFunction]::::::::logFilePath:::::::::::::"+logFilePath);

        // Append log content to the file
        try {
            File logFile = new File(logFilePath);
            Files.createDirectories(Paths.get(logFile.getParent())); // Ensure directory exists

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                String logEntry = logHours + " :: => " + contentToFile + "\n";
                writer.write(logEntry);
            }

            //System.out.println("Log written to: " + logFilePath);
        } catch (IOException e) {
            System.err.println("Error writing log: " + e.getMessage());
        }
    }

}
