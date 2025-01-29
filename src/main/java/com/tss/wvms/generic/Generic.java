package com.tss.wvms.generic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class Generic {
	
	@Autowired
	private NamedParameterJdbcTemplate namedDbJdbcTemplate;
	
	public String getTransactionId()
	{
		log.info(":::::::::::getTransactionId::::::::::");
	    String transactionId= "";
	    try
	    {
	      String query = "SELECT TO_CHAR(TO_NUMBER(TO_CHAR( SYSDATE, 'SS'))+13) || WVMS_TRANSACTION_ID_SEQ.NEXTVAL || TO_CHAR( SYSDATE, 'YYDDDSSSSS') AS TRANSACTIONID FROM DUAL";
	      transactionId = namedDbJdbcTemplate.queryForObject(query, new HashMap<>(), String.class);
	      log.info("[getTransactionId]:::::::::::transactionId::::::::"+transactionId);
	    }
	    catch (Exception localException)
	    {
	      log.error("Exception While fetching transaction id:::::"+localException.getMessage());
	      return "";
	    }
	    return transactionId;
	}

	
	public static Map<String,String> stringToHash(String paramString1, String paramString2) throws Exception
	{
	    log.info(":::::::::stringToHash:::::::");
	    log.info("[getTransactionId]:::::::::paramString1::::::::"+paramString1+"::::::::::paramString2:::::::::"+paramString2);
	    HashMap<String,String> localHashMap = new HashMap();
	    String[] arrayOfString = paramString1.replace("\"", "").trim().split(paramString2);
	    for (int i = 0; i < arrayOfString.length; i += 2) {
	      localHashMap.put(arrayOfString[i], arrayOfString[(i + 1)]);
	    }
	    log.info("[getTransactionId]::::::::::localHashMap::::::::"+localHashMap);
	    return localHashMap;
	}

	
	 public static ArrayList<Integer> getCosValues(int paramInt) throws Exception
	 {
		log.info(":::::::::getCosValues:::::::");
		log.info("[getCosValues]::::paramInt:::::::"+paramInt);
	    ArrayList localArrayList = new ArrayList();
	    if (paramInt == 0) {
	      return localArrayList;
	    }
	    if ((paramInt % 2 != 0) && (paramInt >= 1))
	    {
	      localArrayList.add(Integer.valueOf(1));

	      paramInt -= 1;
	    }
	    int i = 2;
	    while (paramInt >= i)
	    {
	      paramInt -= i;
	      localArrayList.add(Integer.valueOf(i));
	      i *= 2;
	    }
	    log.info("[getCosValues]::::::::::localArrayList::::::::"+localArrayList);
	    return localArrayList;
	  }

	 
	 
	 public int getProcesId(int paramInt1, int paramInt2)
	 {
		log.info(":::::::::::::::::getProcesId:::::::::::::::::");
		log.info("[getProcesId]::::paramInt1:::::::"+paramInt1+":::::paramInt2::::::::"+paramInt2);
		String query="";
		String processSeqId = "";
	    int i = (int)(Math.random() * paramInt1) + 1;
	    try
	    {
	      String str = "SELECT WVMS_PROCESS_SEQ.NEXTVAL FROM DUAL";
	      
	      processSeqId = namedDbJdbcTemplate.queryForObject(query,new HashMap<String,String>(),String.class);
	     
	      if (!processSeqId.equals(null) && !processSeqId.isEmpty())
	      {
	          i = Integer.parseInt(processSeqId);
	          switch (paramInt2)
	          {
	          case 0:
	            i = i % paramInt1 + 1;
	            break;
	          case 1:
	            i = i % (paramInt1 / 2) + 1;
	            break;
	          case 2:
	            i = i % (paramInt1 / 2) + paramInt1 / 2 + 1;
	          }
	        
	      }
	      
	    }
	    catch (Exception localException)
	    {
	      System.out.println("[getProcesId] :: Exception While fetching Process Id");
	      switch (paramInt2)
	      {
	      case 0:
	        i = (int)(Math.random() * paramInt1) + 1;
	        break;
	      case 1:
	        i = (int)(Math.random() * paramInt1 / 2.0D) + 1;
	        break;
	      }
	      return (int)(Math.random() * paramInt1 / 2.0D) + paramInt1 / 2 + 1;
	    }
	    return i;
	  }


	       

}
