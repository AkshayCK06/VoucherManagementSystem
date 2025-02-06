package com.tss.wvms.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.tss.wvms.requestResponse.CWSResponse;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CWSInterface 
{
	@Value("${WCCWS_USERNAME}")
	private String userName;
	
	@Value("${WCCWS_PASSWORD}")
	private String password;
	
	@Value("${WCCWS_SOAP_ACTION}")
	private String soapAction;
	
	@Value("${WCCWS_GENERAL_XML_MESSAGE}")
	private String globalXmlMessage;
	
	@Value("${WCCWS_URL_TIMEOUT}")
	private int urlTimeout;
	
	@Value("${WCCWS_URL")
	private String url;
	
	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .withZone(ZoneOffset.UTC);
    

	public CWSResponse rechargeAccountBySubscriber(String subscriberMsisdn,String identity,String secretCode,String rechargeComment) throws NoSuchAlgorithmException
	{		
		log.info("::::::::::::::::rechargeAccountBySubscriber:::::::::::::");
		CWSResponse cwsResponse = new CWSResponse();
		
		int processId = 1;
		String getEncryptedPassReturnValue = getEncryptedPass(processId);
		String[] getArray = getEncryptedPassReturnValue.split("\\|_\\^_\\|");
		String cwsUserName=getArray[0],cwsBase64Password = getArray[1],timestamp=getArray[2],nonce=getArray[3];
		
		String functionText = "<RechargeAccountBySubscriber xmlns=\"http://comverse-in.com/prepaid/ccws\"><subscriberId>"+subscriberMsisdn+"</subscriberId><identity>"+identity+"</identity><secretCode>"+secretCode+"</secretCode><rechargeComment>"+rechargeComment+"</rechargeComment></RechargeAccountBySubscriber>";
		String message = globalXmlMessage;
		String returnValue="";
		
        message = message.replaceAll("__FUN_NAME__","RechargeAccountBySubscriber");
        message = message.replaceAll("__USERNAME__", cwsUserName);
        message = message.replaceAll("__PASSWORD__", cwsBase64Password);
        message = message.replaceAll("__NOUNCE__", nonce);
        message = message.replaceAll("__TIMESTAMP__", timestamp);
        message = message.replaceAll("__FUNCTIONCALL__", functionText);
        
        log.info("[rechargeAccountBySubscriber]:::::::::::soapAction:::::::::::::"+soapAction);
        log.info("[rechargeAccountBySubscriber]:::::::::::::message:::::::::::::"+message);
        
        returnValue = connectUsingHttpURLConnection(soapAction,message,processId);
        cwsResponse = parseCWSResponse(returnValue,subscriberMsisdn,processId);
        
        log.info("[rechargeAccountBySubscriber]:::::::::::cwsResponse:::::::::::::"+cwsResponse);
		;
		return cwsResponse;
	}
	
	public String connectUsingHttpURLConnection(String soapAction, String xmlMessage, int processId) {
        
		log.info("[connectUsingHttpURLConnection]:::::::::::::url:::::::::::::"+url);
        HttpURLConnection connection = null;
        StringBuilder response = new StringBuilder();
        
        try {
            URL endpoint = new URL(url);
            connection = (HttpURLConnection) endpoint.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("SOAPAction", soapAction);
            connection.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
            connection.setConnectTimeout(urlTimeout); // Timeout for connection establishment
            connection.setReadTimeout(urlTimeout); // Timeout for reading data
            connection.setDoOutput(true);
            
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = xmlMessage.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
                os.flush();
            }
            
            int responseCode = connection.getResponseCode();
            log.error("[connectUsingHttpURLConnection]:::::::::::Response Code:::::::::::::::" + responseCode);
            
            if (responseCode != HttpURLConnection.HTTP_OK) {
                log.error("[connectUsingHttpURLConnection]:::::::Failed to connect, response code::::::::::::" + responseCode);
                return "Error: " + responseCode;
            }
            
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine).append("\n");
                }
            }
            
        } catch (SocketTimeoutException e) {
            log.error("[connectUsingHttpURLConnection]::::::::::::Timeout occurred while connecting to server:::::::::::::::::::");
            return "-1"; // Timeout case
        } catch (IOException e) {
            log.error("[connectUsingHttpURLConnection]::::::::::::Error in connecting to server:::::::::::::::::::" + e.getMessage());
            return "-1";
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        
        log.info("[connectUsingHttpURLConnection]:::::::::::::::::LWP Response value == " + response.toString() + "==");
        return response.toString();
     }
	
	 
	 public CWSResponse parseCWSResponse(String response,String subscriberMsisdn,int processId)
	 {
		 log.info("[parseCWSResponse]:::::::::::::url:::::::::::::"+url);
		 CWSResponse cwsResponse = new CWSResponse();
		 
		 String responseValue = response.replaceAll("[\r\n\t]", "");
		 
		 Pattern pattern = Pattern.compile("<detail><ErrorCode>(.*?)</ErrorCode><ErrorDescription>(.*?)</ErrorDescription>");
	     Matcher matcher = pattern.matcher(responseValue);
		 
		 if(responseValue.equals("XXX") || responseValue.equals("-1") || responseValue.contains("500 Can't connect") || responseValue.contains("404 Not FoundConnection") || responseValue.equals("") || responseValue.equals("500 Internal Server"))
		 {
			 cwsResponse.setResponseCode("1000");
			 cwsResponse.setResponseDescription("500 Internal error");
			 cwsResponse.setStatus("3");
		 }
		 else if(responseValue.contains("500 read timeout") || responseValue.contains("timeout"))
		 {	 
			 cwsResponse.setResponseCode("1009");
			 cwsResponse.setResponseDescription("500 read timeout");
			 cwsResponse.setStatus("4");
		 }
		 else if(matcher.find())
		 {
			 cwsResponse.setResponseCode(matcher.group(1));
			 cwsResponse.setResponseDescription(matcher.group(2));
			 cwsResponse.setStatus("3");
		 }
		 else
		 {	 
			 cwsResponse.setResponseCode("0000");
			 cwsResponse.setResponseDescription("Success");
			 cwsResponse.setStatus("2");
		 }
		 return cwsResponse;
	 }
	
	 public String getEncryptedPass(int processId) throws NoSuchAlgorithmException {
		 
		    log.info("[parseCWSResponse]:::::::::::::getEncryptedPass:::::::::::::");
		    
	        String nonce = getNonce() + processId + "==";
	        String timestamp = formatter.format(Instant.now());
	        String expiry = formatter.format(Instant.now().plusSeconds(5 * 60));
	        
	        byte[] decodedNonce = Base64.getDecoder().decode(nonce);
	        String text = new String(decodedNonce, StandardCharsets.UTF_8) + timestamp + password;
	        text = text.replaceAll("\\n", "");
	        
	        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
	        byte[] hashedBytes = sha1.digest(text.getBytes(StandardCharsets.UTF_8));
	        String base64Password = Base64.getEncoder().encodeToString(hashedBytes);
	        
	        return userName + "|_^_|" + base64Password + "|_^_|" + timestamp + "|_^_|" + nonce;
	}
	
	
	private String getNonce() {
        Random random = new Random();
        int nonce = 100000000 + random.nextInt(900000000); // Generate a 9-digit random number
        return String.valueOf(nonce);
    }
}
