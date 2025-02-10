package com.tss.wvms.service;

import java.io.BufferedReader;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import com.tss.wvms.requestResponse.ICPResponse;
import com.tss.wvms.requestResponse.ICPResponseMapper;

import lombok.extern.slf4j.Slf4j;
@Service
@Slf4j
public class ICPInterface {

	@Value("${WVMS_ICP_REQUEST}")
	private String icpRequest;
	
	@Value("${WVMS_QUERY_TRANSACTION_REQUEST")
	private String icpQueryTransactionRequest;
	
	@Value("${WVMS_ICP_URL}")
	private String icpRechageURL;
	
	@Value("${WVMS_AUTH_HEADER}")
	private String icpAuthorizationValue;
	
	@Value("${WVMS_APPLICATION_NAME}")
	private String icpAuthorizationName;

	
	public ICPResponseMapper recharge(String subsNo,long transactionId,int faceValue,int serialNumber,int batchNumber,String comment,String channel,int voucherAmount,String voucherNumber,HashMap<String,String> serviceHash)
	{
		
		log.info("::::::::::::::::::recharge::::::::::::::::::::");
		ICPResponseMapper response = new ICPResponseMapper();
		
		log.info("======== Recharge Method Called ========");
        log.info("Subscriber No: {}", subsNo);
        log.info("Transaction ID: {}", transactionId);
        log.info("Face Value: {}", faceValue);
        log.info("Serial Number: {}", serialNumber);
        log.info("Batch Number: {}", batchNumber);
        log.info("Comment: {}", comment);
        log.info("Channel: {}", channel);
        log.info("Voucher Amount: {}", voucherAmount);
        log.info("Voucher Number: {}", voucherNumber);
        log.info("Service Hash: {}", serviceHash);
        
		
		String[]  requestArray =icpRequest.split("::::"); 
		log.info("::::::::::::::::::requestArray::::::::::::::::::::"+requestArray[0]);
		String mainRequest = requestArray[0],bucketRequest = requestArray[1];
		
		
		String finalBucketRequest="";
		

		for (Map.Entry<String, String> entry : serviceHash.entrySet()) {  
			
			String key = entry.getKey();
		    String values = entry.getValue();
		    
			String bucketReq = bucketRequest;
			String[] serviceArray = values.split("\\|");

		    if (serviceArray.length < 7) {
		        log.error("[recharge]::::::::::::::Invalid service data format: " + values);
		        continue;
		    }

		    String serviceName = serviceArray[0];
		    String serviceUnit = serviceArray[1];
		    String serviceStatus = serviceArray[2];
		    String serviceValidityDate = serviceArray[3];
		    String requestDate = serviceArray[4];
		    String transactionType = serviceArray[5];
		    String serviceValidity = serviceArray[6];

		    bucketReq = bucketReq.replaceAll("__BID__",key);
		    bucketReq = bucketReq.replaceAll("__BAMT__",serviceUnit);
		    bucketReq = bucketReq.replaceAll("__BALEXPDATE__",serviceValidityDate);
            
            finalBucketRequest+=bucketReq;
            
           // finalBucketRequest.replaceAll(",$","");
            
            log.info("[recharge]::::::::::finalBucketRequest::::::::::::::"+finalBucketRequest);
            

            mainRequest = mainRequest.replaceAll("__APPREFID__", String.valueOf(transactionId));
            mainRequest = mainRequest.replaceAll("__BALANCES__", finalBucketRequest);  
            mainRequest = mainRequest.replaceAll("__FACEVAL__", String.valueOf(faceValue));
            mainRequest = mainRequest.replaceAll("__SERIALNUM__", String.valueOf(serialNumber));
            mainRequest = mainRequest.replaceAll("__BATCHNUM__", String.valueOf(batchNumber));
            mainRequest = mainRequest.replaceAll("__COMMENT__", comment);
            mainRequest = mainRequest.replaceAll("__CHANNEL__", channel);
            
           
            log.info("[recharge]::::::::::mainRequest::::::::::::::"+mainRequest);
		   
		}
		
		response  = sendRequest(subsNo,mainRequest,icpRechageURL);
		log.info("[recharge]:::::::::::response::::::::::::::::::"+response);
		return response;
	}
	
	
	
	public ICPResponseMapper sendRequest(String subsNo,String mainRequest,String urlString)
	{
		log.info("::::::::::::::::::sendRequest::::::::::::::::::::");	
		
		urlString = urlString.replaceAll("__MSISDN__",subsNo);
		log.info("[sendRequest]:::::::::urlString::::::::"+urlString);
		log.info("[sendRequest]:::::::::mainRequest::::::::"+mainRequest);
		log.info("[sendRequest]:::::::::icpAuthorizationValue::::::::"+icpAuthorizationValue+"::::::::::icpAuthorizationName::::::"+icpAuthorizationName);
		ICPResponseMapper responseWrapper = new ICPResponseMapper();
		String authorize = Base64.getEncoder().encodeToString(icpAuthorizationValue.getBytes());
		
		try {
			
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(40000); // 40 seconds timeout
            connection.setReadTimeout(40000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Authorization", "Basic " + authorize);
            connection.setRequestProperty("ApplicationName", icpAuthorizationName);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

            // Send request body
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = mainRequest.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Read response
            int responseCode = connection.getResponseCode();
            InputStream is = (responseCode == 200) ? connection.getInputStream() : connection.getErrorStream();
            
            ObjectMapper objectMapper = new ObjectMapper();
	         // Deserialize JSON into the ResponseWrapper object
	         responseWrapper = objectMapper.readValue(is, ICPResponseMapper.class);

            log.info("[sendRequest]: Response :: " + responseWrapper);

        } catch (IOException e) {
            log.info("[sendRequest]: Exception :: " + e.getMessage());
        }
		return responseWrapper;
	}
	
}
