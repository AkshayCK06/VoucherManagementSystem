package com.tss.wvms.scheduler;

import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import com.tss.wvms.generic.Generic;
import com.tss.wvms.model.TransactionDet;
import com.tss.wvms.model.TransactionMast;

@Slf4j
@Service
public class VoucherRedeem {
	
	@Value("${WVMS_BUCKETID_MULTIPLY_FACTOR}")
	private String bucketIdFactor;
	
	@Value("${WVMS_BUCKETID_MULTIPLY_FACTOR}")
	private String bucketIdFactorIn;
	
	@Autowired
	private NamedParameterJdbcTemplate namedDbJdbcTemplate;
	
	@Autowired
	Generic generic;

	@Scheduled(fixedDelay = 5000) // Runs every 10 seconds
    public void voucherRedeem() throws Exception {
		
		log.info(":::::::::[voucherRedeem(Scheduler Process)]:::::::::::::::::");
		String query="",rechargeDetails="";
		int centCovertFactor=1,serviceUnitT=0;
		boolean isRecordUpdated=false;
		
		HashMap<String,String> params = new HashMap<String,String>();
		
		HashMap<String,String> bucketIdFactorHash = (HashMap<String, String>) generic.stringToHash(bucketIdFactor,",");
		HashMap<String,String> bucketIdFactorInHash = (HashMap<String, String>) generic.stringToHash(bucketIdFactorIn,",");
		HashMap<String,String> serviceHash = new HashMap<String,String>();
		
 		//to fetch the transaction details from TRASACTION_MAST where status=0(New)
		
		query="SELECT SEQ_ID,TRANSACTION_ID,to_char(REQ_DATE,'dd-mm-yyyy hh24:mi:ss') as REQ_DATE,SUBSCRIBER_MSISDN,STATUS,BATCH_NUMBER,REQ_MODE,VOUCHER_NUMBER,APPLICABLE_COS,SERVICE_FULFILLMENT_COS,VOUCHER_AMOUNT,SERIAL_NUMBER FROM TRANSACTION_MAST WHERE STATUS=0 and ROWNUM<10";	
		log.info("[voucherRedeem(Scheduler Process)]:::::::::::::query to fetch the transaction details from TRANSACTION_MAST :::::::::::"+query);
		
		List<TransactionMast> transactionDetails = namedDbJdbcTemplate.query(query, new HashMap<>(),
				
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
				 if(!t.toString().isEmpty())
				 { 
					 rechargeDetails+=t.getServiceName()+":"+t.getServiceUnit()+"-"+t.getServiceValidity()+",";
				 }
				 log.info("[voucherRedeem(Scheduler Process)]:::::::::::::::::rechargeDetails:::::::::::::"+rechargeDetails);
			 
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
			 
			 log.info("[voucherRedeem(Scheduler Process)]:::::::::::::::serviceHash::::::::::::::"+serviceHash);
		}
	}
}
