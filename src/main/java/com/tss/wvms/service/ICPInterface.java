package com.tss.wvms.service;

import java.io.BufferedReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
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

	@Autowired
    private GenericFunctions genericFunction;
	
	private String logFileName ="WVMS_VoucherRedemption.log";
	
	public ICPResponseMapper recharge(String subsNo,long transactionId,int faceValue,int serialNumber,int batchNumber,String comment,String channel,int voucherAmount,String voucherNumber,HashMap<String,String> serviceHash)
	{
		
		genericFunction.logFunction(logFileName,"::::::::::::::::::recharge::::::::::::::::::::");
		ICPResponseMapper response = new ICPResponseMapper();
		
		genericFunction.logFunction(logFileName,"======== Recharge Method Called ========");
		genericFunction.logFunction(logFileName,"Subscriber No:"+ subsNo);
		genericFunction.logFunction(logFileName,"Transaction ID:"+transactionId);
		genericFunction.logFunction(logFileName,"Face Value:"+faceValue);
		genericFunction.logFunction(logFileName,"Serial Number:"+serialNumber);
		genericFunction.logFunction(logFileName,"Batch Number"+batchNumber);
		genericFunction.logFunction(logFileName,"Comment:"+comment);
		genericFunction.logFunction(logFileName,"Channel:"+channel);
		genericFunction.logFunction(logFileName,"Voucher Amount:"+voucherAmount);
		genericFunction.logFunction(logFileName,"Voucher Number:"+voucherNumber);
		genericFunction.logFunction(logFileName,"Service Hash:"+serviceHash);
        
		
		String[]  requestArray =icpRequest.split("::::"); 
		genericFunction.logFunction(logFileName,"::::::::::::::::::requestArray::::::::::::::::::::"+requestArray[0]);
		String mainRequest = requestArray[0],bucketRequest = requestArray[1];
		
		
		String finalBucketRequest="";
		

		for (Map.Entry<String, String> entry : serviceHash.entrySet()) {  
			
			String key = entry.getKey();
		    String values = entry.getValue();
		    
			String bucketReq = bucketRequest;
			String[] serviceArray = values.split("\\|");

		    if (serviceArray.length < 7) {
		    	genericFunction.logFunction(logFileName,"[recharge]::::::::::::::Invalid service data format: " + values);
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
            
		    genericFunction.logFunction(logFileName,"[recharge]::::::::::bucketReq::::::::::::::"+bucketReq);
            finalBucketRequest+=bucketReq+",";
		   } 
            
			finalBucketRequest=finalBucketRequest.replaceAll(",$","");
            
            genericFunction.logFunction(logFileName,"[recharge]::::::::::finalBucketRequest::::::::::::::"+finalBucketRequest);
            

            mainRequest = mainRequest.replaceAll("__APPREFID__", String.valueOf(transactionId));
            mainRequest = mainRequest.replaceAll("__BALANCES__", finalBucketRequest);  
            mainRequest = mainRequest.replaceAll("__FACEVAL__", String.valueOf(faceValue));
            mainRequest = mainRequest.replaceAll("__SERIALNUM__", String.valueOf(serialNumber));
            mainRequest = mainRequest.replaceAll("__BATCHNUM__", String.valueOf(batchNumber));
            mainRequest = mainRequest.replaceAll("__COMMENT__", comment);
            mainRequest = mainRequest.replaceAll("__CHANNEL__", channel);
            
           
            genericFunction.logFunction(logFileName,"[recharge]::::::::::mainRequest::::::::::::::"+mainRequest);
		   
		
		
		response  = sendRequest(subsNo,mainRequest,icpRechageURL);
		genericFunction.logFunction(logFileName,"[recharge]:::::::::::response::::::::::::::::::"+response);
		return response;
	}
	
	
	
	public ICPResponseMapper sendRequest(String subsNo,String mainRequest,String urlString)
	{
		genericFunction.logFunction(logFileName,"::::::::::::::::::sendRequest::::::::::::::::::::");	
		
		urlString = urlString.replaceAll("__MSISDN__",subsNo);
		genericFunction.logFunction(logFileName,"[sendRequest]:::::::::urlString::::::::"+urlString);
		genericFunction.logFunction(logFileName,"[sendRequest]:::::::::mainRequest::::::::"+mainRequest);
		genericFunction.logFunction(logFileName,"[sendRequest]:::::::::icpAuthorizationValue::::::::"+icpAuthorizationValue+"::::::::::icpAuthorizationName::::::"+icpAuthorizationName);
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
            connection.setRequestProperty("X-Real-IP", "192.168.120.6");

            // Send request body
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = mainRequest.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Read response
            int responseCode = connection.getResponseCode();
            
            genericFunction.logFunction(logFileName,"[sendRequest]: connection :: " + connection);
            genericFunction.logFunction(logFileName,"[sendRequest]: responseCode :: " + responseCode);
            InputStream is = (responseCode == 200) ? connection.getInputStream() : connection.getErrorStream();
            
            ObjectMapper objectMapper = new ObjectMapper();
	         // Deserialize JSON into the ResponseWrapper object
	         responseWrapper = objectMapper.readValue(is, ICPResponseMapper.class);

	         genericFunction.logFunction(logFileName,"[sendRequest]: Response :: " + responseWrapper);

        } catch (IOException e) {
        	genericFunction.logFunction(logFileName,"[sendRequest]: Exception :: " + e.getMessage());
        }
		return responseWrapper;
	}
	
	
}
