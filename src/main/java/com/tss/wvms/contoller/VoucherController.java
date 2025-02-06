

package com.tss.wvms.contoller;


import org.springframework.http.MediaType;

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
	 
	 public VoucherRedeemResponse voucherRedeem(@RequestBody VoucherRedeemRequest request,HttpServletRequest httpRequest) throws Exception {
	   
		log.info("::::::::::::::Received VoucherRedeemRequest:::::::::::::");
		log.info("::::::::::::::::::::::request:::::::::::::::::::::::::::"+request);

		VoucherRedeemResponse voucherRedeemResponse = new VoucherRedeemResponse();
		String contentType = httpRequest.getContentType();
        int mediaTypeArgument = 0; 

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
        
		VoucherRedeemResponse response = voucherRedeemptionService.redeemVoucher(request);
	    return response;
	 }
     
	 
}
