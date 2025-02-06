package com.tss.wvms.scheduler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.tss.wvms.generic.Generic;
import com.tss.wvms.model.TransactionMast;
import com.tss.wvms.requestResponse.CWSResponse;
import com.tss.wvms.service.CWSInterface;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class VoucherRedeemptionRTBS 
{
	
	 @Autowired
	 private NamedParameterJdbcTemplate namedDbJdbcTemplate;
	
	 @Autowired
	 CWSInterface cwsInterface;
	 
	 @Autowired
	 Generic generic;
	 
	 @Value("${WVMS_SEND_MESSAGE_NOTIFICATION_ENABLE}")
	 private int sendMessage;
	 
	 @Value("${WVMS_RTBSVOUCHER_SUCCMSG}")
	 private String successMessage;
	 
	 @Value("${WVMS_VOUCHER_STATUS_UPDATE}")
	 private String voucherStatusStr;
	 
	 @Value("${WVMS_MIGRATION_FLAG}")
	 private String migrationFlag;
	 
	 //This module to send the voucher redemption request to RTBS system
     //@Scheduled(fixedDelay = 2000)
     public void voucherRedeemptionRTBS() throws Exception
     {
    	 String query = "";
    	 boolean isRecordsUpdated = false,isRecordInserted=false;
    	 HashMap<String,String> params = new HashMap<String,String>();

    	 //# status 2 -success ; 3 - failure ; 4 -timeout
	     String statusComboStr = "2,6,3,7,4,4";
		 String messageIDStr =  "2,1,3,2,4,3";
		 String voucherStatComboStr = "2,3,3,1,4,4";
		
		 Map<String, String> statusCombo = generic.stringToHash(statusComboStr,",");

	     // Message ID Mapping
	     Map<String, String> msgIdCombo = generic.stringToHash(messageIDStr,",");
	         
	     //Voucher Status Mapping
	     Map<String, String> voucherStatCombo = generic.stringToHash(voucherStatComboStr,",");
	     
	     //Voucher Status update(Response code)
	     Map<String,String> voucherStatusMap = generic.stringToHash(voucherStatusStr,",");
		    
		 String messageId="",status ="",mastStatus= "",voucherStatus="",responseDescription=successMessage;
    	 //to fetch details from TRANSACTION_MAST where STATUS=0
    	 query = "SELECT SEQ_ID,TRANSACTION_ID,to_char(REQ_DATE,'dd-mm-yyyy hh24:mi:ss') AS REQ_DATE,SUBSCRIBER_MSISDN,STATUS,BATCH_NUMBER,REQ_MODE,VOUCHER_NUMBER,APPLICABLE_COS,SERVICE_FULFILLMENT_COS,VOUCHER_AMOUNT,SERIAL_NUMBER FROM TRANSACTION_MAST WHERE STATUS=0 and ROWNUM<10";

    	 log.info("[voucherRedeemptionRTBS(Scheduler Process)]:::::::query to fetch details from TRANSACTION_MAST where STATUS=0::::::::::"+query);
    	 List<TransactionMast> transactionMastList = namedDbJdbcTemplate.query(query, new HashMap<String,String>(),
    		
    			 (rs,rowNum) -> new TransactionMast( 
    					 
    			       rs.getInt("SEQ_ID"),
    			       rs.getLong("TRANSACTION_ID"),
    			       rs.getString("REQ_DATE"),
    			       rs.getInt("SUBSCRIBER_MSISDN"),
    			       rs.getInt("STATUS"),
    			       rs.getInt("BATCH_NUMBER"),
    			       rs.getInt("REQ_MODE"),
    			       rs.getLong("VOUCHER_NUMBER"),
    			       rs.getInt("APPLICABLE_COS"),
    			       rs.getInt("SERVICE_FULFILLMENT_COS"),
    			       rs.getInt("SERIAL_NUMBER"),
    			       rs.getInt("VOUCHER_AMOUNT")
    			 )
    	);  
    	if(transactionMastList.size()>0)
    	{	
    		for(TransactionMast transactionMast : transactionMastList)
        	{	
        		log.info("[voucherRedeemptionRTBS(Scheduler Process)]:::::::transactionMast:::::::::"+transactionMast);
        		
        		//to update the TRANSACTION_MAST with status as 1(In processing)
        		query = "UPDATE TRANSACTION_MAST SET STATUS=:status,RESP_CODE=:responseCode WHERE TRANSACTION_ID=:transactionId";
        		
                params.put("status","1");
                params.put("responseCode","");
                params.put("transactionId",String.valueOf(transactionMast.getTransactionId()));
                
                log.info("[voucherRedeemptionRTBS(Scheduler Process)]:::::::query to update the TRANSACTION_MAST with status as 1(In processing):::::::::"+query);
                
                try 
                {
                	isRecordsUpdated = namedDbJdbcTemplate.update(query, params) > 0;
                	log.info("[voucherRedeemptionRTBS(Scheduler Process)]:::::::isRecordsUpdated into TRANSACTION_MAST :::::::::::::::::::::"+isRecordsUpdated);
                }
                catch(Exception e)
                {
                	log.error("[voucherRedeemptionRTBS(Scheduler Process)]:::::::::Exception in updating record into TRANSACTION_MAST::::::::::"+e.getMessage());
                }
                
                
                String voucherNumber = String.format("%012d",transactionMast.getVoucherNumber());
                
                CWSResponse cwsResponse = cwsInterface.rechargeAccountBySubscriber(String.valueOf(transactionMast.getMsisdn()),"Personal", voucherNumber, "Voucher Recharge");
        	    
                messageId = msgIdCombo.get(cwsResponse.getStatus());
                if(cwsResponse.getResponseCode().equals("-1"))
                {	
                	log.error("[voucherRedeemptionRTBS(Scheduler Process)]:::::::::Fail Response::::::::::"+cwsResponse);
                	status="3";
                }
                mastStatus = cwsResponse.getResponseCode().equals("-1") ? statusCombo.get(status) :  statusCombo.get(cwsResponse.getStatus());
                
                
                if(migrationFlag.equals("1"))
                {	
                	voucherStatus = voucherStatusMap.get(cwsResponse.getResponseCode());
                }
                
                if(voucherStatus.equals(""))
                {	
                	voucherStatus = voucherStatCombo.get(cwsResponse.getStatus());
                }
                
                if(cwsResponse.getResponseCode().equals("0000"))
                {	
                	responseDescription = responseDescription.replaceAll("__TRANSID__",String.valueOf(transactionMast.getTransactionId()));
                	responseDescription = responseDescription.replaceAll("__AMOUNT__",String.valueOf(transactionMast.getVoucherAmount()));
                }
                
                log.info("[voucherRedeemptionRTBS(Scheduler Process)]:::::::::::::cwsResponse::::::::::::::::::"+cwsResponse);
                log.info("[voucherRedeemptionRTBS(Scheduler Process)]:::::::::::::mastStatus::::::::::::::::::"+mastStatus);
                log.info("[voucherRedeemptionRTBS(Scheduler Process)]:::::::::::::voucherStatus::::::::::::::::::"+voucherStatus);
                log.info("[voucherRedeemptionRTBS(Scheduler Process)]:::::::::::::messageId::::::::::::::::::"+messageId);
                log.info("[voucherRedeemptionRTBS(Scheduler Process)]:::::::::::::transactionId::::::::::::::::::"+transactionMast.getTransactionId());
                
                
                //to update TRANSACTION_MAST with final status
                query = "UPDATE TRANSACTION_MAST SET STATUS=:status,RESP_CODE=:responseCode,RESP_DESC=:responseDescription WHERE TRANSACTION_ID=:transactionId";
                
                params.put("status",mastStatus);
                params.put("responseCode",cwsResponse.getResponseCode());
                params.put("responseDescription", cwsResponse.getResponseCode().equals("0000") ? responseDescription : cwsResponse.getResponseDescription());
                params.put("transactionId",String.valueOf(transactionMast.getTransactionId()));
                
                log.info("[voucherRedeemptionRTBS(Scheduler Process)]:::::::query to update the TRANSACTION_MAST with final status :::::::::"+query);
                
                try 
                {
                	isRecordsUpdated = namedDbJdbcTemplate.update(query, params) > 0;
                	log.info("[voucherRedeemptionRTBS(Scheduler Process)]:::::::isRecordsUpdated into TRANSACTION_MAST :::::::::::::::::::::"+isRecordsUpdated);
                }
                catch(Exception e)
                {
                	isRecordsUpdated = false;
                	log.error("[voucherRedeemptionRTBS(Scheduler Process)]:::::::::Exception in updating record into TRANSACTION_MAST::::::::::"+e.getMessage());
                }
                
                
                if(migrationFlag.equals("1"))
                {	
                	//to update voucherStatus in VOUCHER_DET
                	query = "UPDATE VOUCHER_DET SET STATUS=:status WHERE VOUCHER_NUMBER=voucherEncrypt(:voucherNumber)";
                	
                	params.put("status",voucherStatus);
                	params.put("voucherNumber",voucherNumber);
                	
                	log.info("[voucherRedeemptionRTBS(Scheduler Process)]:::::::query to update voucherStatus in VOUCHER_DET :::::::::"+query);
                     
                    try 
                    {
                     	isRecordsUpdated = namedDbJdbcTemplate.update(query, params) > 0;
                     	log.info("[voucherRedeemptionRTBS(Scheduler Process)]:::::::isRecordsUpdated into VOUCHER_DET :::::::::::::::::::::"+isRecordsUpdated);
                    }
                    catch(Exception e)
                    {
                    	isRecordsUpdated = false;
                     	log.error("[voucherRedeemptionRTBS(Scheduler Process)]:::::::::Exception in updating record into VOUCHER_DET::::::::::"+e.getMessage());
                    }
                	
                	
                }
                
                if(sendMessage==1 && !messageId.equals(""))
                {	
                	query="INSERT INTO WVMS_TRANSACTION_MESSAGES(SEQ_ID,MESSAGE_ID,STATUS,TRANSACTION_ID,PROCESS_ID) VALUES (TRANS_MSG_SEQ.NEXTVAL,:messageId,0,:transactionId,1)";
                	
                    params.put("messageId",messageId);
                    params.put("transactionId",String.valueOf(transactionMast.getTransactionId()));
                    
                    log.info("[voucherRedeemptionRTBS(Scheduler Process)]:::::::query to insert into WVMS_TRANSACTION_MESSAGES :::::::::"+query);
                    
                    try 
                    {
                    	isRecordInserted = namedDbJdbcTemplate.update(query, params) > 0;
                     	log.info("[voucherRedeemptionRTBS(Scheduler Process)]:::::::isRecordsUpdated into WVMS_TRANSACTION_MESSAGES :::::::::::::::::::::"+isRecordInserted);
                    }
                    catch(Exception e)
                    {
                    	isRecordInserted=false;
                     	log.error("[voucherRedeemptionRTBS(Scheduler Process)]:::::::::Exception in updating record into WVMS_TRANSACTION_MESSAGES::::::::::"+e.getMessage());
                    }
                }
                
        	}
    		try {
    	        Thread.sleep(1000); // Pause execution for 1 second
    	    } catch (InterruptedException e) {
    	        e.printStackTrace();
    	    }
    	
    	}
    	
     
     }
}
