package com.tss.wvms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

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

    private final RestTemplate restTemplate;
    private final JdbcTemplate jdbcTemplate;

    public WVMSICPInterface(RestTemplate restTemplate, JdbcTemplate jdbcTemplate) {
        this.restTemplate = restTemplate;
        this.jdbcTemplate = jdbcTemplate;
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
            String[] values = entry.getValue().split("\\|");
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

    public String queryTransaction(String transId) {
        logger.info("QueryTransaction:: Transaction ID = {}", transId);

        // Replace placeholders in the query template
        String queryReq = queryTemplate.replace("__APPREFID__", transId);

        // Send HTTP request
        String response = restTemplate.postForObject(configUrl, queryReq, String.class);
        logger.info("QueryTransaction:: Response = {}", response);
        return response;
    }

    public String getConfigValue(String key) {
        // Example of database query to fetch configuration
        String sql = "SELECT config_value FROM config_table WHERE config_key = ?";
        return jdbcTemplate.queryForObject(sql, new Object[]{key}, String.class);
    }
}
