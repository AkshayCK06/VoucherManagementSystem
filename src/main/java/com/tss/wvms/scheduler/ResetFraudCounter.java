package com.tss.wvms.scheduler;

import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.tss.wvms.model.CustomerMast;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ResetFraudCounter {
	
	@Value("${WVMS_RESET_DAY_LIMIT}")
	private int resetDayLimit;
	
	
	@Autowired
	private NamedParameterJdbcTemplate namedDbJdbcTemplate;

	  @Scheduled(cron = "0 50 3 * * *") // 3:50am every day
	  public void resetFraudCounter() throws Exception {
		    
	      log.info(":::::::::::::resetFraudCounter::::::::::::");
		  String query = "";
		  boolean isRecordDeleted= false;
		  HashMap<String,Long> params = new HashMap<String,Long>();
		  
		  //to fetch transaction details from CUSTOMER_MAST
		  query="SELECT SUBSCRIBER_MSISDN,FAIL_COUNT,TO_CHAR(LAST_MOD_DATE,'DD-MM-YYYY HH24:MI:SS') AS LAST_MOD_DATE  FROM CUSTOMER_MAST WHERE LAST_MOD_DATE >  SYSDATE - 10 / (24*60) AND FAIL_COUNT<6";
	  
		  log.info("[resetFraudCounter(Scheduler Process)]::::::::query to fetch transaction details from CUSTOMER_MAST and FAIL_COUNT<6::::::::::"+query);
		  List<CustomerMast> customerMastDetails = namedDbJdbcTemplate.query(query, new HashMap<String,String>() ,
				       (rs,rowNum) -> new CustomerMast( 
				    		           rs.getLong("SUBSCRIBER_MSISDN"),
				    		           rs.getInt("FAIL_COUNT"),
				    		           rs.getString("LAST_MOD_DATE")  
				    		           
				    		   )
		  );
		  
		  if(customerMastDetails.size()>0)
		  {	  
			  
			  for(CustomerMast c:customerMastDetails)
			  {	  
				  log.info("[resetFraudCounter(Scheduler Process)]::::::::customerMastDetail:::::::"+c);
				  
				  query="DELETE FROM CUSTOMER_MAST where SUBSCRIBER_MSISDN=:msisdn";
				  params.put("msisdn",c.getMsisdn());
				  
				  log.info("[resetFraudCounter(Scheduler Process)] :::::query to delet record from CUSTOMER_MAST::::::::::"+query);
				  
				  try {
					  isRecordDeleted = namedDbJdbcTemplate.update(query, params)>0;
					  log.info("[resetFraudCounter(Scheduler Process)]:::::::::isRecordDeleted from CUSTOMER_MAST table:::::::::"+isRecordDeleted);
					  
				  }
				  catch(Exception e)
				  {
					  log.error("[resetFraudCounter(Scheduler Process)]::::::Exception in updating records into CUSTOMER_MAST:::::::"+e.getMessage());
				  }  
				  
			  }
			  
		  }
		  
		   
	  }
	 
}
