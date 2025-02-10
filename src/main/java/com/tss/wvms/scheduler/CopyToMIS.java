package com.tss.wvms.scheduler;

import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.tss.wvms.model.TransactionDet;
import com.tss.wvms.model.TransactionMast;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CopyToMIS {

	@Autowired
	private NamedParameterJdbcTemplate namedDbJdbcTemplate;
	
	//@Scheduled(fixedDelay=5000) // Runs for every 5 seconds 
	public void copyToMisTables() throws Exception {
		
		log.info(":::::::::::copyToMisTables::::::::::::::::");
		
		String query ="";
		boolean isRecordsUpdated = false,isRecordInserted=false;
		HashMap<String,String> params = new HashMap<String,String>();
		
		//to fetch the records from TRANSACTION_MAST where STATUS IN (4,6,7,8,9,10) and BACKUP_FLAG=0
		
		query="SELECT SEQ_ID,TRANSACTION_ID,to_char(REQ_DATE,'dd-mm-yyyy hh24:mi:ss') AS REQ_DATE,to_char(REQ_DATE,'mm_yyyy') AS REQ_MONTH_YEAR_FORMAT,SUBSCRIBER_MSISDN,STATUS,BATCH_NUMBER,REQ_MODE,VOUCHER_NUMBER,APPLICABLE_COS,SERVICE_FULFILLMENT_COS,RESP_CODE,SERIAL_NUMBER,REQ_TYPE,VOUCHER_AMOUNT FROM TRANSACTION_MAST WHERE BACKUP_FLAG=0 AND STATUS IN (4,6,7,8,9,10) and ROWNUM<10";
	    log.info("[copyToMisTables(Scheduler Process)]::::::query to fetch the records from TRANSACTION_MAST:::::::"+query);
	    
	    List<TransactionMast> transactionDetails = namedDbJdbcTemplate.query(query, new HashMap<String,String>(), 
	    		         (rs,rowNum) -> new TransactionMast( 
	    		              
	    		        		 rs.getInt("SEQ_ID"),
	    		        		 rs.getLong("TRANSACTION_ID"),
	    		        		 rs.getString("REQ_DATE"),
	    		        		 rs.getString("REQ_MONTH_YEAR_FORMAT"),
	    		        		 rs.getString("SUBSCRIBER_MSISDN"),
	    		        		 rs.getInt("STATUS"),
	    		        		 rs.getInt("BATCH_NUMBER"),
	    		        		 rs.getInt("REQ_MODE"),
	    		        		 rs.getLong("VOUCHER_NUMBER"),
	    		        		 rs.getInt("APPLICABLE_COS"),
	    		        		 rs.getInt("SERVICE_FULFILLMENT_COS"),
	    		        		 rs.getInt("RESP_CODE"),
	    		        		 rs.getInt("SERIAL_NUMBER"),
	    		        		 rs.getInt("REQ_TYPE"),
	    		        		 rs.getInt("VOUCHER_AMOUNT")
	    		        		 
	    		        )
	    );
	    
	    if(transactionDetails.size()>0)
	    {	
	    	for(TransactionMast transaction:transactionDetails)
	    	{	
	    		
	    		log.info("[copyToMisTables(Scheduler Process)]::::::::::Transaction details:::::::::"+transaction);
	    		
	    		//to update TRANSCATION_MAST table with BACKUP_FLAG= 
	    		query="UPDATE TRANSACTION_MAST SET BACKUP_FLAG=:backupFlag WHERE SEQ_ID=:seqId";
	    		
	    		params.put("backupFlag","1");
	    		params.put("seqId",String.valueOf(transaction.getTransactionSequenceId()));
	    		
	    		log.info("[copyToMisTables(Scheduler Process)]:::::::query to update TRANSACTION_MAST table:::::::::"+query);
	    		
	    		try
	    		{
	    			isRecordsUpdated = namedDbJdbcTemplate.update(query, params)>0;
	    			log.info("[copyToMisTables(Scheduler Process)]:::::::isRecordsUpdated into TRANSACTION_MAST table:::::::::"+isRecordsUpdated);
	    		}
	    		catch(Exception e)
	    		{
	    			isRecordsUpdated=false;
	    			log.error("[copyToMisTables(Scheduler Process)]:::::::Exception in updating records in TRANSACTION_MAST table:::::::::"+e.getMessage());
	    
	    		}
	    		
	    	
	    		if(isRecordsUpdated)
	    		{	
	    			//to fetch the records from TRANSACTION_DET table
	    			query="SELECT SERVICE_TYPE,SERVICE_NAME,SERVICE_UNIT,SERVICE_STATUS,SERVICE_VALIDITY,TO_CHAR(REQUEST_DATE,'DD-MM-YYYY HH24:MI:SS') AS REQ_DATE,TO_CHAR(PROCESS_DATE,'DD-MM-YYYY HH24:MI:SS') AS PROCESS_DATE,TRANS_TYPE,TRANSACTION_ID,ICP_REF_ID,PENDING_RETRY,TO_CHAR(RETRY_DATE,'DD-MM-YYYY HH24:MI:SS') AS RETRY_DATE,RESP_CODE,RESP_DESC,TO_CHAR(RESP_DATE,'DD-MM-YYYY HH24:MI:SS') AS RESPONSE_DATE FROM TRANSACTION_DET WHERE MAIN_TRANS_ID=:transactionId";
	    		    params.put("transactionId",String.valueOf(transaction.getTransactionId()));
	    		    
	    		    log.info("[copyToMisTables(Scheduler Process)]::::::query to fetch the records from TRANSACTION_DET:::::::"+query);
	    		    
	    		    List<TransactionDet> transactionDetDetails = namedDbJdbcTemplate.query(query,params,
	    		        (rs,rowNum) -> new TransactionDet(
	    		        		
	    		        		   rs.getInt("SERVICE_TYPE"),
	    		        		   rs.getString("SERVICE_NAME"),
	    		        		   rs.getInt("SERVICE_UNIT"),
	    		        		   rs.getInt("SERVICE_STATUS"),
	    		        		   rs.getInt("SERVICE_VALIDITY"),
	    		        		   rs.getString("REQ_DATE"),
	    		        		   rs.getString("PROCESS_DATE"),
	    		        		   rs.getInt("TRANS_TYPE"),
	    		        		   rs.getLong("TRANSACTION_ID"),
	    		        		   rs.getBigDecimal("ICP_REF_ID"),
	    		        		   rs.getInt("PENDING_RETRY"),
	    		        		   rs.getString("RETRY_DATE"),
	    		        		   rs.getInt("RESP_CODE"),
	    		        		   rs.getString("RESP_DESC"),
	    		        		   rs.getString("RESPONSE_DATE")
	    		        		)
	    		    		
	    		    );
	    		    
	    		    if(transactionDetDetails.size()>0)
	    		    {	
	    		    	for(TransactionDet transactionDet:transactionDetDetails)
	    		    	{	
	    		    		log.info("[copyToMisTables(Scheduler Process)]::::::::::Transaction Det details:::::::::"+transactionDet);
	    		    		
	    		    		query="INSERT INTO REQUEST_MIS_"+transaction.getRequestDateMonthWise()+" (SEQ_ID,REQ_DATE,SUBSCRIBER_MSISDN,STATUS,BATCH_NUMBER,REQ_MODE,"
	    		    			+ " VOUCHER_NUMBER,TRANSACTION_ID,DENOM_TRANS_ID,BONUS_TRANS_ID,DENOM_ICP_REF_ID,BONUS_ICP_REF_ID,"
	    		    			+ " DENOM_DETAILS,BONUS_DETAILS,DENOM_STATUS,BONUS_STATUS,DENOM_RESPCODE,DENOM_RESPDESC,BONUS_RESPCODE,BONUS_RESPDESC,"
	    		    			+ " MAIN_RESPCODE,SERIAL_NUMBER,REQ_TYPE,VOUCHER_AMOUNT,BONUS_ALLOWED) VALUES (:seqId,to_date('"+transaction.getRequestDate()+"','dd-mm-yyyy hh24:mi:ss'),:msisdn,"
	    		    			+ " :status,:batchNumber,:reqMode,:voucherNumber,:transactionId,:denomTransId,:bonusTransId,:denomRefId,:bonusRefId,:denomDetails,"
	    		    			+ " :bonusDetails,:denomStatus,:bonusStatus,:denomRespCode,:denomRespDesc,:bonusRespCode,:bonusRespDesc,:mainRespCode,"
	    		    			+ " :serialNumber,:reqType,:voucherAmount,:bonusFlag)";
	    		    		
	    		    		
	    		    		params.put("seqId",String.valueOf(transaction.getTransactionSequenceId()));
	    		    		params.put("msisdn",String.valueOf(transaction.getMsisdn()));
	    		    		params.put("status",String.valueOf(transaction.getStatus()));
	    		    		params.put("batchNumber",String.valueOf(transaction.getBatchNumber()));
	    		    		params.put("reqMode",String.valueOf(transaction.getRequestMode()));
	    		    		params.put("voucherNumber",String.valueOf(transaction.getVoucherNumber()));
	    		    		params.put("transactionId",String.valueOf(transaction.getTransactionId()));
	    		    		params.put("denomTransId", transactionDet.getTransactionType() == 1 ? String.valueOf(transactionDet.getTransactionId()):null);
	    		    		params.put("bonusTransId", transactionDet.getTransactionType() == 0 ? String.valueOf(transactionDet.getTransactionId()):null);
	    		    		params.put("denomRefId", transactionDet.getTransactionType() == 1 ? String.valueOf(transactionDet.getIcpReferenceId()):null);
	    		    		params.put("bonusRefId", transactionDet.getTransactionType() == 0 ? String.valueOf(transactionDet.getIcpReferenceId()):null);
	    		    		params.put("denomDetails",transactionDet.getTransactionType() == 1 ? transactionDet.getServiceType()+","+transactionDet.getServiceUnit()+","+transactionDet.getServiceValidity()+"|":null);
	    		    		params.put("bonusDetails", transactionDet.getTransactionType() == 0 ? transactionDet.getServiceType()+","+transactionDet.getServiceUnit()+","+transactionDet.getServiceValidity()+"|":null);
	    		    		params.put("denomStatus", transactionDet.getTransactionType() == 1 ? String.valueOf(transactionDet.getServiceStatus()):null);
	    		    		params.put("bonusStatus", transactionDet.getTransactionType() == 0 ? String.valueOf(transactionDet.getServiceStatus()):null);
	    		    		params.put("denomRespCode", transactionDet.getTransactionType() == 1 ? String.valueOf(transactionDet.getResponseCode()):null);
	    		    		params.put("denomRespDesc", transactionDet.getTransactionType() == 1 ? transactionDet.getResponseDescription():null);
	    		    		params.put("bonusRespCode", transactionDet.getTransactionType() == 0 ? String.valueOf(transactionDet.getResponseCode()):null);
	    		    		params.put("bonusRespDesc", transactionDet.getTransactionType() == 1 ? transactionDet.getResponseDescription():null);
	    		    		params.put("mainRespCode", String.valueOf(transaction.getResponseCode()));
	    		    		params.put("serialNumber", String.valueOf(transaction.getSerialNumber()));
	    		    		params.put("reqType", String.valueOf(transaction.getRequestType()));
	    		    		params.put("voucherAmount",String.valueOf(transaction.getVoucherAmount()));
	    		    		params.put("bonusFlag", transactionDet.getTransactionType() == 0 ? "1":"0");
	    		    		
	    		    		

	    		    		
	    		    		log.info("[copyToMisTables(Scheduler Process)]:::::::query to insert into REQUEST_MIS_"+transaction.getRequestDateMonthWise()+" table:::::::::"+query);
	    		    		
	    		    		try
	    		    		{
	    		    			isRecordInserted = namedDbJdbcTemplate.update(query, params)>0;
	    		    			log.info("[copyToMisTables(Scheduler Process)]:::::::isRecordInserted into REQUEST_MIS_"+transaction.getRequestDateMonthWise()+" table:::::::::"+isRecordInserted);
	    		    		}
	    		    		catch(Exception e)
	    		    		{
	    		    			isRecordInserted=false;
	    		    			log.error("[copyToMisTables(Scheduler Process)]:::::::Exception in inserting records into REQUEST_MIS_"+transaction.getRequestDateMonthWise()+" table::::::::"+e.getMessage());
	    		    	
	    		    		}
	    		    			    		
	    		    	}//end of for each in transactionDet
	    		    	
	    		    }//end of if transactionDet.size()>0
	    		    	
	    		}//end of record updated in TRASACTION_MAST
	    		
	    		
	    		
	    		 //to update TRANSCATION_MAST table with BACKUP_FLAG= 
	    		query="UPDATE TRANSACTION_MAST SET BACKUP_FLAG=:backupFlag WHERE SEQ_ID=:seqId";
	    		
	    		params.put("backupFlag","2");
	    		params.put("seqId",String.valueOf(transaction.getTransactionSequenceId()));
	    		
	    		log.info("[copyToMisTables(Scheduler Process)]:::::::query to update TRANSACTION_MAST table:::::::::"+query);
	    		
	    		try
	    		{
	    			isRecordsUpdated = namedDbJdbcTemplate.update(query, params)>0;
	    			log.info("[copyToMisTables(Scheduler Process)]:::::::isRecordsUpdated into TRANSACTION_MAST table:::::::::"+isRecordsUpdated);
	    		}
	    		catch(Exception e)
	    		{
	    			isRecordsUpdated=false;
	    			log.error("[copyToMisTables(Scheduler Process)]:::::::Exception in updating records in TRANSACTION_MAST table:::::::::"+e.getMessage());
	    			
	    		}
	    		
	    		
	    	}//end of for each in transactionDetails
	    	
	    }//end of transactionDetails.size()
	    try {
	        Thread.sleep(1000); // Pause execution for 1 second
	    } catch (InterruptedException e) {
	        e.printStackTrace();
	    }

	}
	
}
