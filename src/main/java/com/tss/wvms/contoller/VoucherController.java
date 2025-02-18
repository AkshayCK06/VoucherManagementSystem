

package com.tss.wvms.contoller;


import org.springframework.http.MediaType;

import java.util.Base64;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tss.wvms.requestResponse.Response;
import com.tss.wvms.requestResponse.VoucherRedeemRequest;
import com.tss.wvms.requestResponse.VoucherRedeemResponse;
import com.tss.wvms.service.VoucherGenerationService;
import com.tss.wvms.service.VoucherRedeemptionService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequestMapping("/wvms/voucher")
public class VoucherController {
	 

	 @Autowired
	 private VoucherGenerationService voucherGenerationService;
	 
	 @Autowired
	 private VoucherRedeemptionService voucherRedeemptionService;
	 

	 @PostMapping("/generateVoucher")
	 public ResponseEntity<Response> generateVouchers(@RequestParam int batchId) {
	    log.info("VoucherGenerationController :: generateVouchers :: received request :: batchId={}",batchId);
	    Response response = null;
	    try {
	       response = voucherGenerationService.generateVouchers(batchId);

	       if (response.getErrorCode() == 0) {
	          return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
	       } else {

	          return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
	       }

	    } catch (Exception e) {

	       log.error("An error occurred While Inserting the Data", e.getMessage());

	       return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
	    }
	 }
	 
	 
	 @PostMapping(value = "/redeem", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE}, 
             produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
	 
	 public VoucherRedeemResponse voucherRedeem(@Valid @RequestBody VoucherRedeemRequest request,HttpServletRequest httpRequest) throws Exception {
	   
		log.info("::::::::::::::Received VoucherRedeemRequest:::::::::::::");
		log.info("::::::::::::::::::::::request:::::::::::::::::::::::::::"+request);

		VoucherRedeemResponse voucherRedeemResponse = new VoucherRedeemResponse();
		String username="",password="";
		
		String contentType = httpRequest.getContentType();
        int mediaTypeArgument = 0; 
        

        String authHeader = httpRequest.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Basic ")) {
           
            String base64Credentials = authHeader.substring("Basic ".length());
            byte[] decodedBytes = Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(decodedBytes);
            
            String[] values = credentials.split(":", 2);
            username = values[0];
            password = values[1];

            log.info("Authenticated Username: {}", username);
            log.info("Authenticated Password: {}", password);
        } else {
            log.warn("No Authorization header found");
        }

        if (MediaType.APPLICATION_JSON_VALUE.equals(contentType)) {
            mediaTypeArgument = 2; // JSON
            log.info("Received request with content type: JSON");
            
        } else if (MediaType.APPLICATION_XML_VALUE.equals(contentType)) {
            mediaTypeArgument = 1; // XML
            log.info("Received request with content type: XML");
        }
        else {
            log.warn("Invalid Media Type");
            log.info("Request Content Type: {}", contentType);
  
            voucherRedeemResponse.setRespCode("1005");
			voucherRedeemResponse.setRespDesc("Unsupported Media Type: " + contentType);
			//throw new UnsupportedOperationException("Unsupported Media Type: " + contentType);
            return voucherRedeemResponse;
            
        }
        
        if(request.getMsisdn() == null || request.getMsisdn().isEmpty() || request.getMsisdn().isBlank())
        {	
        	voucherRedeemResponse.setRespCode("1005");
 			voucherRedeemResponse.setRespDesc("Invalid Request MSISDN is mandatory");
 			return voucherRedeemResponse;
        }
        else if(request.getVoucherFlag()==null || request.getVoucherFlag().isBlank() || request.getVoucherFlag().isEmpty())
		{		
        	voucherRedeemResponse.setRespCode("1005");
 			voucherRedeemResponse.setRespDesc("Invalid Request VOUCHERFLAG is mandatory");
 			return voucherRedeemResponse;
		}
        else if(request.getVoucherNo() == null || request.getVoucherNo().isBlank() || request.getVoucherNo().isEmpty())
        {
        	voucherRedeemResponse.setRespCode("1005");
 			voucherRedeemResponse.setRespDesc("Invalid Request VOUCHERNO is mandatory");
 			return voucherRedeemResponse;
        }
        else
        {
        	voucherRedeemResponse = voucherRedeemptionService.redeemVoucher(request,username);
        	return voucherRedeemResponse;
        }
	    
	 }
     
	 
}










