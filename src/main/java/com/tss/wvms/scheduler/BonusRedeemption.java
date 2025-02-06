package com.tss.wvms.scheduler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import com.tss.wvms.generic.Generic;
import com.tss.wvms.model.TransactionDet;
import com.tss.wvms.model.TransactionMast;
import com.tss.wvms.requestResponse.ICPResponseMapper;
import com.tss.wvms.requestResponse.ICPResponseMapper.BalanceInfo;
import com.tss.wvms.service.ICPInterface;

@Service
@Slf4j
public class BonusRedeemption {
	
	
	 @Autowired
	 private NamedParameterJdbcTemplate namedDbJdbcTemplate;
	 
	 @Autowired
	 private Generic generic;
	 
	 @Value("${WVMS_BUCKETID_MULTIPLY_FACTOR}")
	 private String bucketIdFactor;
		
	 @Value("${WVMS_BUCKETID_MULTIPLY_FACTOR_FROM_IN}")
	 private String bucketIdFactorIn;
	
	 @Value("${WVMS_REQ_MODE_MAP}")
	 private String requestModeMap;
	
	 @Value("${WVMS_USSD_BONUS_SUCCESS_MSG}")
	 private String successMessage;
	
	 @Value("${WVMS_BALANCE_DETAIL}")
	 private String balanceMessage;
	
	 @Value("${WVMS_SEND_MESSAGE_NOTIFICATION_ENABLE}")
	 private int sendMessageEnable;
	 
	 @Value("${WVMS_SEND_BONUS_FAILURE}")
	 private int sendBonusFailure;
	 
	 @Autowired
	 ICPInterface icpInterface;
		
	 
	 @Scheduled(fixedDelay=2000) // Runs for every 2 seconds
	 
	 //This module to send the voucher redemption request to ICP system for bonus
	 //Request comes here once the main denomination request is success
	 public void bonusRedeemption() throws Exception
	 {

		 String query="";
		 HashMap<String,String> params = new HashMap<String,String>();
		 boolean isRecordUpdated = false,isRecordUpdatedInTransactionDet=false;
	     
	     HashMap<String,String> serviceHash = new HashMap<String,String>();
	     HashMap<String,String> bucketIdFactorHash = (HashMap<String, String>) Generic.stringToHash(bucketIdFactor,",");
	  
	     String rechargeDetails="";
	     //to retrive the records from TRANSACTION_MAST where STATUS=2(Inserted based on the bonus count after successful talktime redeemption response from ICP API-VoucherRedeemption) 
	     query =" SELECT SEQ_ID,TRANSACTION_ID,TO_CHAR(REQ_DATE,'DD-MM-YYYY HH24:MI:SS') AS REQ_DATE,SUBSCRIBER_MSISDN,STATUS,BATCH_NUMBER,REQ_MODE,VOUCHER_NUMBER,APPLICABLE_COS,SERVICE_FULFILLMENT_COS,VOUCHER_AMOUNT,SERIAL_NUMBER,RESP_DESC FROM TRANSACTION_MAST WHERE STATUS=2 AND ROWNUM<10";
	     
	     log.info("[bonusRedeemption(Scheduler Process)]:::::::::query to retrive the records from TRANSACTION_MAST where STATUS=2::::::::"+query);
	     List<TransactionMast> transactionMastDetails = namedDbJdbcTemplate.query(query,new HashMap<String, String>(), 
	    		 
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
	    				 rs.getInt("VOUCHER_AMOUNT"),
	    				 rs.getInt("SERIAL_NUMBER"),
	    				 rs.getString("RESP_DESC")
	    				 
	    	)
	     );
	     
	     if(transactionMastDetails.size()>0)
	     {	 
	    	 for(TransactionMast transaction:transactionMastDetails)
	    	 {	 
	    		 log.info("[bonusRedeemption(Scheduler Process)]:::::::::transaction:::::::::::"+transaction);
	    		 
	    		 //to updated TRANSACTION_MAST STATUS=5
	    		 query="UPDATE TRANSACTION_MAST SET STATUS=:status,RESP_CODE=:responseCode  WHERE TRANSACTION_ID=:transactionId";
	    		 
	    		 params.put("status","5");
	    		 params.put("responseCode","");
	    		 params.put("transactionId",String.valueOf(transaction.getTransactionId()));
	    		 
	    		 
	    		 try {
	    			 isRecordUpdated = namedDbJdbcTemplate.update(query, params)>0;
	    			 log.info("[bonusRedeemption(Scheduler Process)]:::::::::isRecordUpdated in TRANSACTION_MAST table:::::::::::"+isRecordUpdated);
	    		 }
	    		 catch(Exception e)
	    		 {
	    			 isRecordUpdated = false;
	    			 log.error("[bonusRedeemption(Scheduler Process)]:::::::::Exception in updating record into TRANSACTION_MAST table:::::::::::"+e.getMessage());
	    		 }
	    		 
	    		 if(isRecordUpdated)
	    		 {	 
	    			 
	    			 //to update the TRANSACTION_DET PROCESS_DATE=SYSDATE
	    			 query ="UPDATE TRANSACTION_DET SET PROCESS_DATE=SYSDATE WHERE MAIN_TRANS_ID=:transactionId AND TRANS_TYPE=1";
	    			 params.put("transactionId",String.valueOf(transaction.getTransactionId()));
	    			 
	    			 log.info("[bonusRedeemption(Scheduler Process)]:::::::::query to update TRANSACTION_DET table:::::::::::"+query);
	    			 
	    			 try {
	    				 isRecordUpdatedInTransactionDet = namedDbJdbcTemplate.update(query, params)>0;
		    			 log.info("[bonusRedeemption(Scheduler Process)]:::::::::isRecordUpdated in TRANSACTION_DET table:::::::::::"+isRecordUpdatedInTransactionDet);
		    		 }
		    		 catch(Exception e)
		    		 {
		    			 isRecordUpdatedInTransactionDet = false;
		    			 log.error("[bonusRedeemption(Scheduler Process)]:::::::::Exception in updating record into TRANSACTION_DET table:::::::::::"+e.getMessage());
		    		 }
	    			 
	    		 }
	    		 
	    		 if(isRecordUpdatedInTransactionDet)
	    		 {	 
	    			 //to fetch details from TRANSACTION_DET TRANS_TYPE=0(Bonus)
	    			 query = "SELECT SERVICE_TYPE,SERVICE_NAME,SERVICE_UNIT,SERVICE_STATUS,SERVICE_VALIDITY,to_char(REQUEST_DATE,'dd-mm-yyyy hh24:mi:ss') AS REQUEST_DATE,TRANS_TYPE,TRANSACTION_ID,to_char(sysdate+SERVICE_VALIDITY,'yyyymmdd hh24:mi:ss') AS SERVICE_VALIDITY_SYSDATE FROM TRANSACTION_DET WHERE MAIN_TRANS_ID=:transactionId and TRANS_TYPE=0";
	    			 params.put("transactionId",String.valueOf(transaction.getTransactionId()));
	    			 
	    			 log.info("[bonusRedeemption(Scheduler Process)]:::::::::query to to fetch details from TRANSACTION_DET where TRANS_TYPE=0(Bonus)::::::::"+query);
	    			 
	    			 List<TransactionDet> transactionDetDetails = namedDbJdbcTemplate.query(query, params, 
	    					 
	    			      (rs,rowNum) -> new TransactionDet(
	    			    		  
	    			    		  rs.getInt("SERVICE_TYPE"),
	    			    		  rs.getString("SERVICE_NAME"),
	    			    		  rs.getInt("SERVICE_UNIT"),
	    			    		  rs.getInt("SERVICE_STATUS"),
	    			    		  rs.getInt("SERVICE_VALIDITY"),
	    			    		  rs.getString("REQUEST_DATE"),
	    			    		  rs.getInt("TRANS_TYPE"),
	    			    		  rs.getLong("TRANSACTION_ID"),
	    			    		  rs.getString("SERVICE_VALIDITY_SYSDATE")
	    			    		  
	    			       )	
	    			 );
	    			 
	    			 if(transactionDetDetails.size()>0)
	    			 {
	    				 for(TransactionDet transactionDet : transactionDetDetails)
		    			 {	 
	    					 log.info("[bonusRedeemption(Scheduler Process)]:::::::::transactionDet:::::::::::"+transactionDet);
	    					 rechargeDetails+=transactionDet.getServiceName()+":"+transactionDet.getServiceUnit()+"-"+transactionDet.getServiceValidity()+",";
	    					 
	    					 log.info("[bonusRedeemption(Scheduler Process)]:::::::::rechargeDetails:::::::::::"+rechargeDetails);
	    					 int centCovertFactor = 1,serviceUnitT=0;
	    					 
	    					 if(bucketIdFactorHash.containsKey(String.valueOf(transactionDet.getServiceType())))
	    					 {	 
	    						 centCovertFactor = transactionDet.getServiceType();
	    					 }
	    					 serviceUnitT = transactionDet.getServiceUnit() * centCovertFactor;
	    					 
	    					 serviceHash.put(String.valueOf(transactionDet.getServiceType()),transactionDet.getServiceName()+"|"+serviceUnitT+"|"+transactionDet.getServiceStatus()+"|"+transactionDet.getServiceValididtyDate()+"|"+transactionDet.getRequestDate()+"|"+transactionDet.getTransactionType()+"|"+transactionDet.getServiceValidity());
		    			 
		    			     log.info("[bonusRedeemption(Scheduler Process)]:::::::::serviceHash:::::::::::"+serviceHash);
		    			 
		    			 
		    			     creditRequest(transaction,transactionDet.getTransactionId(),serviceHash,rechargeDetails);
		    				 
		    			 }
		    		   
	    			 }
	    			 
	    		 }
	    			 
	    	 }
	     }
	     try 
	     {
		        Thread.sleep(1000); // Pause execution for 1 second
		 } catch (InterruptedException e) {
		        e.printStackTrace();
		 }
	     
	 }
	 
	 public ICPResponseMapper creditRequest(TransactionMast transactionMastDetails,long subTransactionId,HashMap<String,String> serviceHash,String rechargeDetails) throws Exception
	 {
			
			log.info("[bonusRedeemption(creditRequest)] ::::::::::::::CREDIT REQUEST::::::::::");
		    log.info("[bonusRedeemption(creditRequest)] :::::::::::::::transactionMastDetails:::::::::"+transactionMastDetails);
		    log.info("[bonusRedeemption(creditRequest)] :::::::::::::::subTransactionId::::::::::"+subTransactionId);
		    log.info("[bonusRedeemption(creditRequest)] :::::::::::::;:serviceHash::::::::::::::::::::"+serviceHash);
		    log.info("[bonusRedeemption(creditRequest)] ::::::::::::rechargeDetails:::::::::::::::"+rechargeDetails);
		    
		    String query="",refNos="",errorCode="",status="",desc="",ocsRespTxn="",accountVal="",ivrBalanceInfo="";
		   
		    String respCode="",ussdMsg=successMessage,newBalanceStr="",balanceString="",balanceDetails="",messageId="";
		    int validFlag= 0, mastStatus=1, intStatus = 0,centCovertFactor=0,bonusRequestCount=0,voucherStatus=0,responseInMast=0;
		    float newBalance = 0;
		    boolean isRecordUpdated=false,isRecordInserted=false;
		    
		    String comment = "Voucher Recharge Operation";
		    ICPResponseMapper responseMapper = new ICPResponseMapper();
		    
		    HashMap<String,String> params = new  HashMap<String,String>();
		    HashMap<String,String> bucketIdFactorInHash = (HashMap<String, String>) Generic.stringToHash(bucketIdFactorIn,",");
		    Map<String,String> requestMapHash = Generic.stringToHash(requestModeMap,",");
		    
		    String channel = requestMapHash.get(String.valueOf(transactionMastDetails.getRequestMode()));	    
		    String voucherNumber = String.format("%012d",transactionMastDetails.getVoucherNumber());
		    
		    String statusComboStr = "2,6,3,7,4,4";
			String messageIDStr = sendBonusFailure != 1 ? "3,2,4,3" : "2, ,3, ,4,6";
			String responseHashStr = "1009,1015,1000,1017,0000,1016,1014,1018";
			String voucherStatComboStr = "2,3,3,1,4,4";
			
			Map<String, String> statusCombo = generic.stringToHash(statusComboStr,",");

		    // Message ID Mapping
		    Map<String, String> msgIdCombo = generic.stringToHash(messageIDStr,",");
		     
		    //Response ID Mapping
		    Map<String, String> responseHash = generic.stringToHash(responseHashStr,",");
		     
		    //Voucher Status Mapping
		    Map<String, String> voucherStatCombo = generic.stringToHash(voucherStatComboStr,",");
		    
		   
		    
		    responseMapper = icpInterface.recharge(transactionMastDetails.getMsisdn(),subTransactionId,transactionMastDetails.getVoucherAmount(),transactionMastDetails.getSerialNumber(),transactionMastDetails.getBatchNumber(),comment,channel,transactionMastDetails.getVoucherAmount(),voucherNumber,serviceHash);
		    
		    if(responseMapper.getIcpResponse().getDescription().contains("500 read timeout") || responseMapper.getIcpResponse().getDescription().contains("timeout"))
		    {
		    	 validFlag = 2;
	             intStatus =4;
	             voucherStatus=4;
	             respCode ="1009";
	             log.error("[bonusRedeemption(creditRequest)]::::::Voucher_redeem TRANSACTIONID : "+subTransactionId+"DESTMSISDN : "+transactionMastDetails.getMsisdn() +"VOUCHERPIN :"+transactionMastDetails.getVoucherNumber()+"DESCRIPTION : TIMEOUT FROM ICP  EVENTTYPE:1 EVENTDESC : REDEEM REQMODE:"+transactionMastDetails.getRequestMode() +"STATUS : 4");
		    }
		    else if(responseMapper.getIcpResponse().getDescription().contains("500 Can't connect") || responseMapper.getIcpResponse().getDescription().contains("Internal Server Error") || responseMapper.getIcpResponse().getDescription().contains("404 Not FoundConnection") || responseMapper.getIcpResponse().getDescription().contains("Failed - System Error"))
		    {
		    	intStatus =3;
		    	voucherStatus=1;
	            respCode ="1000";
	            
	            log.error("[bonusRedeemption(creditRequest)]::::::Voucher_redeem TRANSACTIONID : "+subTransactionId+"DESTMSISDN : "+transactionMastDetails.getMsisdn() +"VOUCHERPIN :"+transactionMastDetails.getVoucherNumber()+"DESCRIPTION : CONNECTION ERROR  EVENTTYPE:10 EVENTDESC : REDEEM:"+transactionMastDetails.getRequestMode() +"STATUS : 3");

		    }
		    else
		    {
		    	if(responseMapper.toString().isEmpty())
		    	{	
		    		validFlag=1;
		    		log.error("[voucherRedeem(creditRequest)]:::::::::Invalid JSON Response::::::::::::::");
		    	}
		    	if(validFlag ==1)
		    	{	
		    		intStatus =3;
		    		voucherStatus=1;
		    		respCode ="1000";
		    		log.error("[voucherRedeem(creditRequest)]::::::Voucher_redeem TRANSACTIONID : "+subTransactionId+"DESTMSISDN : "+transactionMastDetails.getMsisdn() +"VOUCHERPIN :"+transactionMastDetails.getVoucherNumber()+" DESCRIPTION : INVALID JSON RESPONSE  EVENTTYPE:10 EVENTDESC : REDEEM:"+transactionMastDetails.getRequestMode() +"STATUS : 3");
		    	}
		    	else
		    	{
		    		log.info("[voucherRedeem(creditRequest)]::::::Voucher_redeem TRANSACTIONID : "+subTransactionId+"::::::::::::Valid JSON Response:::::::::::"+responseMapper);
		    		
		    		refNos = responseMapper.getAppTxnRefId();
		    		ocsRespTxn = responseMapper.getOcsTxnRefId();
		    		errorCode = String.valueOf(responseMapper.getIcpResponse().getErrorCode());
		    		status = String.valueOf(responseMapper.getIcpResponse().getStatus());
		    		desc = responseMapper.getIcpResponse().getDescription();
		    		

		    		if(status.equals("0"))
		    		{	
		    			List<BalanceInfo> balance  = responseMapper.getBalances();
		    			ussdMsg = ussdMsg.replaceAll("__TRANSID__",String.valueOf(transactionMastDetails.getTransactionId()));
		    			ussdMsg = ussdMsg.replaceAll("__FACEVALUE__",String.valueOf(transactionMastDetails.getVoucherAmount()));
		    		
		    			for(BalanceInfo b:balance)
		    			{	
		    				if(bucketIdFactorInHash.containsKey(String.valueOf(b.getId())))
		    				{	
		    					centCovertFactor=b.getId(); 
		    				}
		    				newBalance = newBalance / centCovertFactor;			
		    				newBalanceStr = String.format("%.2f",newBalance);
		    				
		    				if(String.valueOf(b.getId()).equals("1"))
			    			{	
		    					ussdMsg = ussdMsg.replaceAll("__COREVALIDITY__",responseMapper.getAccountValidity());
		    					ussdMsg = ussdMsg.replaceAll("__COREVALUE__",newBalanceStr);
		    					
			    			}
		    					
		    				balanceString=balanceMessage;
		    				balanceString = balanceString.replaceAll("__BALANCE__", newBalanceStr);
		    				balanceString = balanceString.replaceAll("__VALIDITY__",b.getExpiry());
		    				balanceString = balanceString.replaceAll("__BUCKETNAME__",b.getName());
		    				
		    				ivrBalanceInfo = ivrBalanceInfo+"<_>"+b.getName()+"||"+newBalanceStr+"||"+b.getExpiry();
		    				
		    				balanceDetails+=balanceString+",";
		    				
		    				intStatus =2;
		    				respCode ="0000";
		    				
		    				log.info("[bonusRedeemption(creditRequest)]::::::balanceDetails::::::"+balanceDetails);
		    				log.info("[bonusRedeemption(creditRequest)]::::::ivrBalanceInfo::::::"+ivrBalanceInfo);
		    				log.info("[bonusRedeemption(creditRequest)]::::::Voucher_redeem TRANSACTIONID : "+subTransactionId+"DESTMSISDN : "+transactionMastDetails.getMsisdn() +"VOUCHERPIN :"+transactionMastDetails.getVoucherNumber()+" DESCRIPTION : SUCCESS JSON RESPONSE  EVENTTYPE:10 EVENTDESC : REDEEM:"+transactionMastDetails.getRequestMode() +"STATUS : 1");
		    				
		    			}
		    			   			
		    		}
		    		else
		    		{	
		    			log.info("[bonusRedeemption(creditRequest)]:::::::::::FAILED STATUS:::::::::::::::::::::::");
		    			intStatus =3;
		    			respCode ="1000";
		    			voucherStatus = 1;
		    			log.info("[bonusRedeemption(creditRequest)]::::::Voucher_redeem TRANSACTIONID : "+subTransactionId+"DESTMSISDN : "+transactionMastDetails.getMsisdn() +"VOUCHERPIN :"+transactionMastDetails.getVoucherNumber()+" DESCRIPTION : FAILURE JSON RESPONSE  EVENTTYPE:10 EVENTDESC : REDEEM:"+transactionMastDetails.getRequestMode() +"STATUS : 3");
		    			
		    		}		
		    	}
		    }
		    
		    
		    
		    messageId = msgIdCombo.get(String.valueOf(intStatus));
		    voucherStatus = Integer.parseInt(voucherStatCombo.get(String.valueOf(intStatus)));
		    mastStatus= Integer.parseInt(statusCombo.get(String.valueOf(intStatus)));
		    responseInMast = Integer.parseInt(responseHash.get(String.valueOf(voucherStatus)));
		    
		    balanceDetails = transactionMastDetails.getResponseDescription() + balanceDetails;
		    ussdMsg=ussdMsg.replaceAll("__BALDET__", balanceDetails);
		    
		    //to update TRANSACTION_MAST 
		    query = "UPDATE TRANSACTION_MAST SET STATUS=:mastStatus,RESP_CODE=:responseCode WHERE TRANSACTION_ID=:transactionId";
		    
		    params.put("mastStatus",String.valueOf(mastStatus));
		    params.put("responseCode",respCode);
		    params.put("transactionId", String.valueOf(transactionMastDetails.getTransactionId()));
		    
		    log.info("[bonusRedeemption(creditRequest)]:::::::query to update TRANSACTION_MAST::::::::::"+query);
		    
		    try {
		    	isRecordUpdated = namedDbJdbcTemplate.update(query, params)>0;
		    	log.info("[bonusRedeemption(creditRequest)]:::::isRecordUpdated into TRANSACTION_MAST:::::::::"+isRecordUpdated);
		    }
		    catch(Exception e)
		    {
		    	log.error("[bonusRedeemption(creditRequest)]:::::Exception in updating record into TRANSACTION_MAST table::::::::"+e.getMessage());
		    }
		    
		    
		    //to update TRANSACTION_DET
		    query="UPDATE TRANSACTION_DET SET SERVICE_STATUS =:intStatus,RESP_CODE=:responseCode, RESP_DESC=:responseDescription,ICP_REF_ID=:icpReferenceId where TRANSACTION_ID =:transactionId and TRANS_TYPE=0";
		  
		    params.put("intStatus",String.valueOf(intStatus));
		    params.put("responseCode",respCode);
		    params.put("responseDescription",responseMapper.getIcpResponse().getDescription());
		    params.put("icpReferenceId",String.valueOf(responseMapper.getAppTxnRefId()));
		    params.put("transactionId",String.valueOf(transactionMastDetails.getTransactionId()));
		 
		    log.info("[bonusRedeemption(creditRequest)]:::::::query to update TRANSACTION_DET::::::::::"+query);
		    
		    try {
		    	isRecordUpdated = namedDbJdbcTemplate.update(query, params)>0;
		    	log.info("[bonusRedeemption(creditRequest)]:::::isRecordUpdated into TRANSACTION_DET:::::::::"+isRecordUpdated);
		    }
		    catch(Exception e)
		    {
		    	log.error("[bonusRedeemption(creditRequest)]:::::Exception in updating record into TRANSACTION_DET table::::::::"+e.getMessage());
		    }
		    
		    
		    //If send message notification is enabled
		    if(sendMessageEnable==1 && !messageId.equals(""))
		    {	
		    	query="INSERT INTO WVMS_TRANSACTION_MESSAGES(SEQ_ID,MESSAGE_ID,STATUS,TRANSACTION_ID,BALANCE_DET,RECHARGE_DET) VALUES (TRANS_MSG_SEQ.NEXTVAL,:messageId,0,:transactionId,:balanceDetails,:rechargeDetails)";
		       
		    	params.put("messageId", messageId);
		    	params.put("transactionId",String.valueOf(transactionMastDetails.getTransactionId()));
		    	params.put("balanceDetails", balanceDetails);
		    	params.put("rechargeDetails", rechargeDetails);
		    	
		    	 log.info("[bonusRedeemption(creditRequest)]:::::::query to insert into WVMS_TRANSACTION_MESSAGES::::::::::"+query);
		 	    
		 	    try {
		 	    	isRecordInserted = namedDbJdbcTemplate.update(query, params)>0;
		 	    	log.info("[bonusRedeemption(creditRequest)]:::::isRecordInserted into WVMS_TRANSACTION_MESSAGES:::::::::"+isRecordUpdated);
		 	    }
		 	    catch(Exception e)
		 	    {
		 	    	log.error("[bonusRedeemption(creditRequest)]:::::Exception in inserting record into WVMS_TRANSACTION_MESSAGES table::::::::"+e.getMessage());
		 	    }
		    
		    }
		    
		    return responseMapper;
	}

}
