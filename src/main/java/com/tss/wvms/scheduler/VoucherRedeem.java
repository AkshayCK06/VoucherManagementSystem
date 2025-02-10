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

@Slf4j
@Service
public class VoucherRedeem {
	
	@Value("${WVMS_BUCKETID_MULTIPLY_FACTOR}")
	private String bucketIdFactor;
	
	@Value("${WVMS_BUCKETID_MULTIPLY_FACTOR_FROM_IN}")
	private String bucketIdFactorIn;
	
	@Value("${WVMS_REQ_MODE_MAP}")
	private String requestModeMap;
	
	@Value("${WVMS_USSD_SUCCESS_MSG}")
	private String successMessage;
	
	@Value("${WVMS_BALANCE_DETAIL}")
	private String balanceMessage;
	
	@Value("${WVMS_SEND_MESSAGE_NOTIFICATION_ENABLE}")
	private int sendMessageEnable;
	
	@Autowired
	private NamedParameterJdbcTemplate namedDbJdbcTemplate;
	
	@Autowired
	Generic generic;
	
	@Autowired
	ICPInterface icpInterface;
	
	

	@Scheduled(fixedDelay = 2000) // Runs every 2 seconds
    public void voucherRedeem() throws Exception {
		
		log.info(":::::::::[voucherRedeem(Scheduler Process)]:::::::::::::::::");
		String query="",rechargeDetails="";
		boolean isRecordUpdated=false;
		long subTransactionId=0;
		
		HashMap<String,String> params = new HashMap<String,String>();
		
		HashMap<String,String> bucketIdFactorHash = (HashMap<String, String>) Generic.stringToHash(bucketIdFactor,",");
		HashMap<String,String> bucketIdFactorInHash = (HashMap<String, String>) Generic.stringToHash(bucketIdFactorIn,",");
		HashMap<String,String> serviceHash = new HashMap<String,String>();
		
 		//to fetch the transaction details from TRASACTION_MAST where status=0(New)
		
		query="SELECT SEQ_ID,TRANSACTION_ID,to_char(REQ_DATE,'dd-mm-yyyy hh24:mi:ss') as REQ_DATE,SUBSCRIBER_MSISDN,STATUS,BATCH_NUMBER,REQ_MODE,VOUCHER_NUMBER,APPLICABLE_COS,SERVICE_FULFILLMENT_COS,VOUCHER_AMOUNT,SERIAL_NUMBER FROM TRANSACTION_MAST WHERE STATUS=0 and ROWNUM<10";	
		log.info("[voucherRedeem(Scheduler Process)]:::::::::::::query to fetch the transaction details from TRANSACTION_MAST :::::::::::"+query);
		
		List<TransactionMast> transactionDetails = namedDbJdbcTemplate.query(query, new HashMap<>(),
				
				(rs,rowNum) -> new TransactionMast( 
						       rs.getInt("SEQ_ID"),
						       rs.getLong("TRANSACTION_ID"),
						       rs.getString("REQ_DATE"),
						       rs.getString("SUBSCRIBER_MSISDN"),
						       rs.getInt("STATUS"),
						       rs.getInt("BATCH_NUMBER"),
						       rs.getInt("REQ_MODE"),
						       rs.getLong("VOUCHER_NUMBER"),
						       rs.getInt("APPLICABLE_COS"),
						       rs.getInt("SERVICE_FULFILLMENT_COS"),
						       rs.getInt("VOUCHER_AMOUNT"),
						       rs.getInt("SERIAL_NUMBER")
						)
		);
		
		for(TransactionMast transaction:transactionDetails)
		{	
			 log.info("[voucherRedeem(Scheduler Process)]::::::::::transaction:::::::::::::"+transaction);
			 
			 //to update TRANSACTION_MAST table to status=1(Processing)
			 
			 query="UPDATE TRANSACTION_MAST SET STATUS=:status,RESP_CODE=:responseCode WHERE TRANSACTION_ID=:transactionId";
			 
			 params.put("status","1");
			 params.put("responseCode","");
			 params.put("transactionId",String.valueOf(transaction.getTransactionId()));
			 
			 log.info("[voucherRedeem(Scheduler Process)]:::::::query to update TRANSACTION_MAST(STATUS=1):::::::::"+query);
			 
			 try {
				 isRecordUpdated = namedDbJdbcTemplate.update(query,params)>0;
				 log.info("[voucherRedeem(Scheduler Process)]:::::::isRecordUpdated in TRANSACTION_MAST(STATUS=1):::::::::"+isRecordUpdated);
			 }
			 catch(Exception e)
			 {
				 log.error("[voucherRedeem(Scheduler Process)]:::::::Exception in recordUpdate to TRANSACTION_MAST:::::::"+e.getMessage());
				 throw new Exception("1004");
			 }
			 
			 if(isRecordUpdated)
			 {
				 //to update processDate in TRANSACT_DET table
				 
				 query="UPDATE TRANSACTION_DET SET PROCESS_DATE=sysdate WHERE MAIN_TRANS_ID=:transactionId and TRANS_TYPE=1 ";
				 params.put("transactionId",String.valueOf(transaction.getTransactionId()));
				 
				 log.info("[voucherRedeem(Scheduler Process)]:::::::query to update TRANSACTION_DET(TRANS_TYPE=1):::::::::"+query);
				 
				 try {
					 isRecordUpdated = namedDbJdbcTemplate.update(query,params)>0;
					 log.info("[voucherRedeem(Scheduler Process)]:::::::isRecordUpdated in TRANSACTION_DET(TRANS_TYPE=1):::::::::"+isRecordUpdated);
				 }
				 catch(Exception e)
				 {
					 log.error("[voucherRedeem(Scheduler Process)]:::::::Exception in recordUpdate to TRANSACTION_DET(TRANS_TYPE=1):::::::"+e.getMessage());
					 throw new Exception("1004");
				 }
			 }
			 
			 //to fetch details from TRANSACT_DET table
			 
			 query="SELECT SERVICE_TYPE,SERVICE_NAME,SERVICE_UNIT,SERVICE_STATUS,SERVICE_VALIDITY,to_char(REQUEST_DATE,'dd-mm-yyyy hh24:mi:ss') AS REQUEST_DATE,TRANS_TYPE,TRANSACTION_ID,to_char(sysdate+SERVICE_VALIDITY,'yyyymmdd hh24:mi:ss') AS SERVICE_VALIDITY_DATE FROM TRANSACTION_DET WHERE MAIN_TRANS_ID=:transactionId and TRANS_TYPE=1";
			 params.put("transactionId",String.valueOf(transaction.getTransactionId()));
			 
			 
			 log.info("[voucherRedeem(Scheduler Process)]:::::::query to fetch details from TRANSACT_DET(TRANS_TYPE=1):::::::::"+query);
			 

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
							        rs.getString("SERVICE_VALIDITY_DATE")
					 )
			 );
			 
			 log.info("[voucherRedeem(Scheduler Process)]::::::::::transactionDetDetails:::::::::::::::::::"+transactionDetDetails);
			 
			 for(TransactionDet t:transactionDetDetails)
			 { 
				 subTransactionId = t.getTransactionId();
				 if(!t.toString().isEmpty())
				 { 
					 rechargeDetails+=t.getServiceName()+":"+t.getServiceUnit()+"-"+t.getServiceValidity()+",";
				 }
				 log.info("[voucherRedeem(Scheduler Process)]:::::::::::::::::rechargeDetails:::::::::::::"+rechargeDetails);
			 
				 int centCovertFactor=1,serviceUnitT=0;
			     if(bucketIdFactorHash.containsKey(String.valueOf(t.getServiceType())));
			     {
			    	 centCovertFactor= Integer.parseInt(bucketIdFactorHash.get(String.valueOf(t.getServiceType())));
			     }
			     
			     serviceUnitT=t.getServiceUnit()*centCovertFactor;
			     
			     log.info("[voucherRedeem(Scheduler Process)]:::::::centCovertFactor:::::::::"+centCovertFactor);
			     log.info("[voucherRedeem(Scheduler Process)]:::::::serviceUnitT:::::::::::::"+serviceUnitT);
			     
			     String serviceHashValue = t.getServiceName()+"|"+serviceUnitT+"|"+t.getServiceStatus()+"|"+t.getServiceValididtyDate()+"|"+t.getRequestDate()+"|"+t.getTransactionType()+"|"+t.getServiceValidity();
			     serviceHash.put(String.valueOf(t.getServiceType()), serviceHashValue);
			 }
			 log.info("[voucherRedeem(Scheduler Process)]:::::::::::::::transaction::::::::::::::"+transaction.getMsisdn());
			 creditRequest(transaction,subTransactionId,serviceHash,rechargeDetails);
			 log.info("[voucherRedeem(Scheduler Process)]:::::::::::::::serviceHash::::::::::::::"+serviceHash);
		}
	}
	
	
	public ICPResponseMapper creditRequest(TransactionMast transactionMastDetails,long subTransactionId,HashMap<String,String> serviceHash,String rechargeDetails) throws Exception
	{
		
		log.info("[voucherRedeem(creditRequest)] ::::::::::::::CREDIT REQUEST::::::::::");
	    log.info("[voucherRedeem(creditRequest)] :::::::::::::::transactionMastDetails:::::::::"+transactionMastDetails);
	    log.info("[voucherRedeem(creditRequest)] :::::::::::::::subTransactionId::::::::::"+subTransactionId);
	    log.info("[voucherRedeem(creditRequest)] :::::::::::::;:serviceHash::::::::::::::::::::"+serviceHash);
	    log.info("[voucherRedeem(creditRequest)] ::::::::::::rechargeDetails:::::::::::::::"+rechargeDetails);
	    
	    String query="",refNos="",errorCode="",status="",desc="",ocsRespTxn="",accountVal="",ivrBalanceInfo="";
	   
	    String respCode="",ussdMsg=successMessage,newBalanceStr="",balanceString="",balanceDetails="",messageId="",voucherStatus="";
	    int validFlag= 0, mastStatus=1, intStatus = 0,centCovertFactor=0,bonusRequestCount=0;
	    float newBalance = 0;
	    boolean isRecordUpdated=false,isRecordInserted=false;
	    
	    String comment = "Voucher Recharge Operation";
	    ICPResponseMapper responseMapper = new ICPResponseMapper();
	    
	    HashMap<String,String> params = new  HashMap<String,String>();
	    HashMap<String,String> bucketIdFactorInHash = (HashMap<String, String>) Generic.stringToHash(bucketIdFactorIn,",");
	    Map<String,String> requestMapHash = Generic.stringToHash(requestModeMap,",");
	    
	    String channel = requestMapHash.get(String.valueOf(transactionMastDetails.getRequestMode()));	    
	    String voucherNumber = String.format("%012d",transactionMastDetails.getVoucherNumber());
	    
	    HashMap<String, String> statusCombo = new HashMap<String, String>();
        statusCombo.put("2", "6");
        statusCombo.put("3", "7");
        statusCombo.put("4", "4");

        // Message ID Mapping
        HashMap<String, String> msgIdCombo = new HashMap<String, String>();
        msgIdCombo.put("2", "1");
        msgIdCombo.put("3", "2");
        msgIdCombo.put("4", "3");

        // Voucher Status Mapping
        HashMap<String, String> voucherStatCombo = new HashMap<String, String>();
        voucherStatCombo.put("2", "3");
        voucherStatCombo.put("3", "1");
        voucherStatCombo.put("4", "4");
	    
        log.info("[voucherRedeem(creditRequest)]::::::::::::::::MSISDN::::::::::::::::"+transactionMastDetails.getMsisdn());
	    
	    responseMapper = icpInterface.recharge(transactionMastDetails.getMsisdn(),subTransactionId,transactionMastDetails.getVoucherAmount(),transactionMastDetails.getSerialNumber(),transactionMastDetails.getBatchNumber(),comment,channel,transactionMastDetails.getVoucherAmount(),voucherNumber,serviceHash);
	    
	    if(responseMapper.getIcpResponse().getDescription().contains("500 read timeout") || responseMapper.getIcpResponse().getDescription().contains("timeout"))
	    {
	    	 validFlag = 2;
             intStatus =4;
             respCode ="1009";
             log.error("[voucherRedeem(creditRequest)]::::::Voucher_redeem TRANSACTIONID : "+subTransactionId+"DESTMSISDN : "+transactionMastDetails.getMsisdn() +"VOUCHERPIN :"+transactionMastDetails.getVoucherNumber()+"DESCRIPTION : TIMEOUT FROM ICP  EVENTTYPE:1 EVENTDESC : REDEEM REQMODE:"+transactionMastDetails.getRequestMode() +"STATUS : 2");
	    }
	    else if(responseMapper.getIcpResponse().getDescription().contains("500 Can't connect") || responseMapper.getIcpResponse().getDescription().contains("Internal Server Error") || responseMapper.getIcpResponse().getDescription().contains("404 Not FoundConnection") || responseMapper.getIcpResponse().getDescription().contains("Failed - System Error"))
	    {
	    	intStatus =3;
            respCode ="1000";
            
            log.error("[voucherRedeem(creditRequest)]::::::Voucher_redeem TRANSACTIONID : "+subTransactionId+"DESTMSISDN : "+transactionMastDetails.getMsisdn() +"VOUCHERPIN :"+transactionMastDetails.getVoucherNumber()+"DESCRIPTION : CONNECTION ERROR  EVENTTYPE:10 EVENTDESC : REDEEM:"+transactionMastDetails.getRequestMode() +"STATUS : 3");

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
	    		respCode ="1000";
	    		log.error("[voucherRedeem(creditRequest)]::::::Voucher_redeem TRANSACTIONID : "+subTransactionId+"DESTMSISDN : "+transactionMastDetails.getMsisdn() +"VOUCHERPIN :"+transactionMastDetails.getVoucherNumber()+" DESCRIPTION : INVALID JSON RESPONSE  EVENTTYPE:10 EVENTDESC : REDEEM:"+transactionMastDetails.getRequestMode() +"STATUS : 3");
	    	}
	    	else
	    	{
	    		log.info("[voucherRedeem(creditRequest)]::::::Voucher_redeem TRANSACTIONID : "+subTransactionId+"::::::::::::Valid JSON Response:::::::::::");
	    		ocsRespTxn = responseMapper.getOcsTxnRefId();
	    		errorCode = String.valueOf(responseMapper.getIcpResponse().getErrorCode());
	    		status = String.valueOf(responseMapper.getIcpResponse().getStatus());
	    		desc = responseMapper.getIcpResponse().getDescription();
	    		
	    		if(status.equals("0"))
	    		{	
	    			List<BalanceInfo> balance  = responseMapper.getBalances();
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
	    				
	    				log.info("[voucherRedeem(creditRequest)]::::::balanceDetails::::::"+balanceDetails);
	    				log.info("[voucherRedeem(creditRequest)]::::::ivrBalanceInfo::::::"+ivrBalanceInfo);
	    				log.info("[voucherRedeem(creditRequest)]::::::Voucher_redeem TRANSACTIONID : "+subTransactionId+"DESTMSISDN : "+transactionMastDetails.getMsisdn() +"VOUCHERPIN :"+transactionMastDetails.getVoucherNumber()+" DESCRIPTION : SUCCESS JSON RESPONSE  EVENTTYPE:10 EVENTDESC : REDEEM:"+transactionMastDetails.getRequestMode() +"STATUS : 1");
	    				
	    			}
	    			   			
	    		}
	    		else
	    		{	
	    			log.info("[voucherRedeem(creditRequest)]:::::::::::FAILED STATUS:::::::::::::::::::::::");
	    			intStatus =3;
	    			respCode ="1014";
	    			
	    			log.info("[voucherRedeem(creditRequest)]::::::Voucher_redeem TRANSACTIONID : "+subTransactionId+"DESTMSISDN : "+transactionMastDetails.getMsisdn() +"VOUCHERPIN :"+transactionMastDetails.getVoucherNumber()+" DESCRIPTION : FAILURE JSON RESPONSE  EVENTTYPE:10 EVENTDESC : REDEEM:"+transactionMastDetails.getRequestMode() +"STATUS : 3");
	    			
	    		}		
	    	}
	    }
	    
	    //To check in transaction_det, whether any bonus request is present
	    query = "SELECT COUNT(*) FROM TRANSACTION_DET WHERE MAIN_TRANS_ID = :transactionId AND TRANS_TYPE = 0";
	    params.put("transactionId", String.valueOf(transactionMastDetails.getTransactionId()));
	    
	    log.info("[voucherRedeem(creditRequest)]::::::::query to check in transaction_det, whether any bonus request is present::::::"+query);
	    try 
	    {
	    	bonusRequestCount = namedDbJdbcTemplate.queryForObject(query, params, Integer.class); 
	    }
	    catch(Exception e)
	    {
	    	 bonusRequestCount = 0;
	         log.error("[voucherRedeem(creditRequest)]::::::Exception in select query:::::::::"+e.getMessage());	
	    }
	    
	    messageId = msgIdCombo.get(String.valueOf(intStatus));
	    voucherStatus = voucherStatCombo.get(String.valueOf(intStatus));
	    mastStatus = intStatus;
	    
	    log.info("[voucherRedeem(creditRequest)]:::::::messageId:::::::"+messageId+":::::::voucherStatus:::::"+voucherStatus+"::::::mastStatus:::::::::"+mastStatus);
	    
	    if(bonusRequestCount == 0 )
        {
	    	  mastStatus= Integer.parseInt(statusCombo.get(String.valueOf(intStatus)));
              log.error("[voucherRedeem(creditRequest)]::::::Voucher_redeem TRANSACTIONID : "+subTransactionId+"::::::::transaction mast status:"+mastStatus+"::::::Status changed as there is no bonus::::::");
        }	
	    
	    //to update TRANSACTION_MAST 
	    query = "UPDATE TRANSACTION_MAST SET STATUS=:mastStatus,RESP_CODE=:responseCode,RESP_DESC=:responseDescription,IVR_BALANCE_INFO=:ivrBalanceInfo WHERE TRANSACTION_ID=:transactionId";
	    
	    params.put("mastStatus",String.valueOf(mastStatus));
	    params.put("responseCode",respCode);
	    params.put("responseDescription",ussdMsg);
	    params.put("ivrBalanceInfo",ivrBalanceInfo);
	    params.put("transactionId", String.valueOf(transactionMastDetails.getTransactionId()));
	    
	    log.info("[voucherRedeem(creditRequest)]:::::::query to update TRANSACTION_MAST::::::::::"+query);
	    
	    try {
	    	isRecordUpdated = namedDbJdbcTemplate.update(query, params)>0;
	    	log.info("[voucherRedeem(creditRequest)]:::::isRecordUpdated into TRANSACTION_MAST:::::::::"+isRecordUpdated);
	    }
	    catch(Exception e)
	    {
	    	log.error("[voucherRedeem(creditRequest)]:::::Exception in updating record into TRANSACTION_MAST table::::::::"+e.getMessage());
	    }
	    
	    
	    //to update TRANSACTION_DET
	    query="UPDATE TRANSACTION_DET SET SERVICE_STATUS =:intStatus,RESP_CODE=:responseCode, RESP_DESC=:responseDescription,ICP_REF_ID=:icpReferenceId where TRANSACTION_ID =:transactionId and TRANS_TYPE=1";
	  
	    params.put("intStatus",String.valueOf(intStatus));
	    params.put("responseCode",respCode);
	    params.put("responseDescription",responseMapper.getIcpResponse().getDescription());
	    params.put("icpReferenceId",String.valueOf(responseMapper.getAppTxnRefId()));
	    params.put("transactionId",String.valueOf(transactionMastDetails.getTransactionId()));
	 
	    log.info("[voucherRedeem(creditRequest)]:::::::query to update TRANSACTION_DET::::::::::"+query);
	    
	    try {
	    	isRecordUpdated = namedDbJdbcTemplate.update(query, params)>0;
	    	log.info("[voucherRedeem(creditRequest)]:::::isRecordUpdated into TRANSACTION_DET:::::::::"+isRecordUpdated);
	    }
	    catch(Exception e)
	    {
	    	log.error("[voucherRedeem(creditRequest)]:::::Exception in updating record into TRANSACTION_DET table::::::::"+e.getMessage());
	    }
	    
	    
	    //to update VOUCHER_DET
	    query="UPDATE VOUCHER_DET SET STATUS=:voucherStatus WHERE VOUCHER_NUMBER=voucherEncrypt(:voucherNumber)";
	    params.put("voucherStatus",voucherStatus);
	    params.put("voucherNumber",voucherNumber);
	    
	    log.info("[voucherRedeem(creditRequest)]:::::::query to update VOUCHER_DET::::::::::"+query);
	    
	    try {
	    	isRecordUpdated = namedDbJdbcTemplate.update(query, params)>0;
	    	log.info("[voucherRedeem(creditRequest)]:::::isRecordUpdated into VOUCHER_DET:::::::::"+isRecordUpdated);
	    }
	    catch(Exception e)
	    {
	    	log.error("[voucherRedeem(creditRequest)]:::::Exception in updating record into VOUCHER_DET table::::::::"+e.getMessage());
	    }
	    
	    //If send message notification is enabled
	    if(sendMessageEnable==1 && !messageId.equals(""))
	    {	
	    	query="INSERT INTO WVMS_TRANSACTION_MESSAGES(SEQ_ID,MESSAGE_ID,STATUS,TRANSACTION_ID,BALANCE_DET,RECHARGE_DET) VALUES (TRANS_MSG_SEQ.NEXTVAL,:messageId,0,:transactionId,:balanceDetails,:rechargeDetails)";
	       
	    	params.put("messageId", messageId);
	    	params.put("transactionId",String.valueOf(transactionMastDetails.getTransactionId()));
	    	params.put("balanceDetails", balanceDetails);
	    	params.put("rechargeDetails", rechargeDetails);
	    	
	    	 log.info("[voucherRedeem(creditRequest)]:::::::query to insert into WVMS_TRANSACTION_MESSAGES::::::::::"+query);
	 	    
	 	    try {
	 	    	isRecordInserted = namedDbJdbcTemplate.update(query, params)>0;
	 	    	log.info("[voucherRedeem(creditRequest)]:::::isRecordInserted into WVMS_TRANSACTION_MESSAGES:::::::::"+isRecordUpdated);
	 	    }
	 	    catch(Exception e)
	 	    {
	 	    	log.error("[voucherRedeem(creditRequest)]:::::Exception in inserting record into WVMS_TRANSACTION_MESSAGES table::::::::"+e.getMessage());
	 	    }
	    
	    }
	    
	    return responseMapper;
	}
}
