package com.tss.wvms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.rowset.spi.SyncResolver;

@Service
public class WVMSICPInterface {

    private static final Logger logger = LoggerFactory.getLogger(WVMSICPInterface.class);

    // Properties from application.properties
    @Value("${wvms.config.url}")
    private String configUrl;

    @Value("${wvms.recharge.template}")
    private String rechargeTemplate;

    @Value("${wvms.query.template}")
    private String queryTemplate;

    @Value("${VMS_HOME}")
    private static String vmsHome;

    @Value("")
    private static String vmsCfgDir;

    @Value("${config.filename}")
    private static String configFilename;

    private static String queryTransReq;

    private final RestTemplate restTemplate;
    private final JdbcTemplate jdbcTemplate;

    public WVMSICPInterface(RestTemplate restTemplate, JdbcTemplate jdbcTemplate) {
        this.restTemplate = restTemplate;
        this.jdbcTemplate = jdbcTemplate;
        try {
            this.queryTransReq = readReq("queryTransReq");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @PostConstruct
    public void init() {
        // Load templates on startup (optional if preloaded via @Value)
        logger.info("WVMSICPInterface initialized with config URL: {}", configUrl);
    }

    public String recharge(
            String subNos, String transId, String faceVal, String serialNum,
            String batchNum, String comment, String channel, String vAmount,
            String voucherNumber, Map<String, String> hash) {

        logger.info("Recharge:: Input Parameters - Subscriber: {}, Transaction: {}", subNos, transId);

        // Replace placeholders in the recharge template
        String[] templates = rechargeTemplate.split("::::");
        String mainReq = templates[0];
        String bucketReq = templates[1];

        StringBuilder finalBucket = new StringBuilder();
        for (Map.Entry<String, String> entry : hash.entrySet()) {
            String key = entry.getKey();
            String[] values = entry.getValue().split("\\");
            String bucketReqModified = bucketReq
                    .replace("__BID__", key)
                    .replace("__BAMT__", values[1])
                    .replace("__NOD__", values[6])
                    .replace("__BALEXPDATE__", values[3]);
            finalBucket.append(bucketReqModified).append(",");
        }
        if (finalBucket.length() > 0) {
            finalBucket.setLength(finalBucket.length() - 1); // Remove trailing comma
        }

        mainReq = mainReq
                .replace("__ACCESSNO__", subNos)
                .replace("__MOBNOS__", subNos)
                .replace("__APPREFID__", transId)
                .replace("__BALANCES__", finalBucket.toString())
                .replace("__FACEVAL__", faceVal)
                .replace("__SERIALNUM__", serialNum)
                .replace("__BATCHNUM__", batchNum)
                .replace("__COMMENT__", comment)
                .replace("__CHANNEL__", channel)
                .replace("__VOUCHERNUM__", voucherNumber);

        logger.info("Recharge:: Final Request = {}", mainReq);

        // Send HTTP request
        String response = restTemplate.postForObject(configUrl, mainReq, String.class);
        logger.info("Recharge:: Response = {}", response);
        return response;
    }

    public String queryTrans(String subNos, String transId, String refId) {
        // System.out.println("
        // -------------------------------------------------------check status
        // --------------------------\n queryTrans :: subNos:" + subNos + " transId:" +
        // transId + " ICPrefId:" + refId);
        String mainReq = queryTransReq;
        String url;
        String resp = "";

        try {
            url = readConf("WVMS_WICP_TRSNACTION_URL");
            url = url.replaceAll("__REFID__", refId);
            mainReq = mainReq.replaceAll("__MOBNOS__", subNos);
            mainReq = mainReq.replaceAll("__APPREFID__", refId);
            resp = sendRequestGet(mainReq, url);
            System.out.println("Recharge:: Return value is " + resp + "\n");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        ;
        // System.out.println(" -------------------------------------------------------
        // end ----------------------- ----------------------- ");
        return resp;
    }

    public String getConfigValue(String key) {
        String sql = "SELECT config_value FROM config_table WHERE config_key = ?";
        return jdbcTemplate.queryForObject(sql, new Object[] { key }, String.class);
    }

    public String readReq(String searchString) throws IOException {

        String filePath = vmsHome + vmsCfgDir + "/icpRequest/" + searchString;
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            throw new FileNotFoundException("Cannot open file: " + filePath);
        }

        // Read file
        StringBuilder value = new StringBuilder();
        try (RandomAccessFile file = new RandomAccessFile(filePath, "r");
                FileChannel fileChannel = file.getChannel();
                FileLock lock = fileChannel.lock(0, Long.MAX_VALUE, true)) { // Shared lock (READ mode)

            String line;
            while ((line = file.readLine()) != null) {
                value.append(line).append("\n");
            }
        }

        // Trim any leading or trailing spaces
        return value.toString().trim();
    }

    public static String readConf(String searchString) throws IOException {
        // creating file path
        String filePath = vmsHome + "/" + vmsCfgDir + "/" + configFilename;
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            throw new FileNotFoundException("Cannot open file: " + filePath);
        }
        Pattern pattern = Pattern.compile("^" + searchString + "\s*=\s*(.*?)\s*$", Pattern.CASE_INSENSITIVE);
        String value = "0"; // Default value

        // Read file with locking
        try (RandomAccessFile file = new RandomAccessFile(filePath, "r");
                FileChannel fileChannel = file.getChannel();
                FileLock lock = fileChannel.lock(0, Long.MAX_VALUE, true)) { // Shared lock (READ mode)

            String line;
            while ((line = file.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    value = matcher.group(1);
                    break;
                }
            }
        }

        return value;
    }

    public static String sendRequest(String msg, String url) throws IOException {
        System.out.println(" sendRequest : msg = " + msg + " | url=" + url);

        // Auth Value
        String authVal = readConf("WVMS_AUTH_HEADER");
        String authNam = readConf("WVMS_APPLICATION_NAME");
        System.out.println(" sendRequest : authVal = " + authVal + " | authNam=" + authNam + "|url=" + url);

        String auth1 = Base64.getEncoder().encodeToString(authVal.getBytes(StandardCharsets.UTF_8));

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Basic " + auth1);
        connection.setRequestProperty("AppliacationName", authNam);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setDoOutput(true);
        connection.setConnectTimeout(40000);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = msg.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        String strResponse;
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            strResponse = new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            System.out.println(" sendRequest : Response :: " + strResponse);
        } else {
            strResponse = connection.getResponseMessage();
            System.out.println(" sendRequest : Response- :: " + strResponse);
        }

        return strResponse;
    }

    public String getBucketDetails(String subNos, String refId) {
        String url;
        String resp = "";
        try {
            url = readConf("WVMS_ICP_GETBUCKET_DETAILS");
            url = url.replaceAll("__MSISDN__", subNos);
            url = url.replaceAll("__APPREFID__", refId);
            resp = sendRequestGet("", url);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return resp;

    }

    public String sendRequestGet(String msg, String url) throws Exception {
        System.out.println(" sendRequest : msg = " + msg + " | url=" + url);
        String authVal = readConf("WVMS_AUTH_HEADER");
        String authNam = readConf("WVMS_APPLICATION_NAME");
        System.out.println(" sendRequest : authVal = " + authVal + " | authNam=" + authNam + "|url=" + url);
        String auth1 = Base64.getEncoder().encodeToString(authVal.getBytes());

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Basic " + auth1);
        connection.setRequestProperty("AppliacationName", authNam);
        connection.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(msg.getBytes());
            os.flush();
        }

        System.out.println("Request:: request = " + connection);
        StringBuilder strResponse = new StringBuilder();
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    strResponse.append(inputLine);
                }
            }
            System.out.println(" sendRequest : Response :: " + strResponse);
        } else {
            System.out.println(" sendRequest : Response- :: " + connection.getResponseMessage());
        }
        return strResponse.toString();
    }

}
