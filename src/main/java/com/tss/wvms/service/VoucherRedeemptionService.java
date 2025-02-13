package com.tss.wvms.service;

import java.util.AbstractMap;



import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.net.http.HttpHeaders;
import java.text.DateFormat;
import java.text.SimpleDateFormat;


import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;


import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.tss.wvms.generic.Generic;
import com.tss.wvms.model.BatchMast;
import com.tss.wvms.model.MessageMast;
import com.tss.wvms.model.ReasonMast;
import com.tss.wvms.model.RechargeMast;
import com.tss.wvms.model.TransactionMast;
import com.tss.wvms.model.VoucherDet;
import com.tss.wvms.model.DenominationMast;
import com.tss.wvms.model.FreeBeeDet;
import com.tss.wvms.requestResponse.VoucherRedeemRequest;
import com.tss.wvms.requestResponse.VoucherRedeemResponse;
import com.tss.wvms.requestResponse.VoucherRedeemResponse.BalanceInfo;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class VoucherRedeemptionService {
	
	@Value("${WVMS_MOBILE_NUMBER_REGEX}")
	private String msisdnRegEx;
	
	@Value("${WVMS_SERIAL_NUMBER_REGEX}")
	private String serialNoRegEx;
	
	@Value("${WVMS_VOUCHER_NUMBER_REGEX}")
	private String voucherNoRegEx;
	
	@Value("${WVMS_CLIENT_FLAG}")
	private String clientFlag;
	
	@Value("${WVMS_FRAUD_CALL_PARAMS}")
	private String fraudCallParams;
	
	@Value("${WVMS_MAX_FAIL_COUNT}")
    private int maxFailCount;
	
	@Value("${WVMS_MULTI_BLOCK_CALL}")
	private int multiBlockCallFlag;
	
	@Value("${WVMS_ICP_EAPI_USERNAME}")
	private String userName;
	
	@Value("${WVMS_ICP_EAPI_PASSWORD}")
	private String password;
	
	@Value("${WVMS_ICP_BLOCK_SUBSCRIBER_URL}")
	private String blockSubscriberUrl;
	
	@Value("${WVMS_FRAUD_BLOCK_MESSAGE_FLAG}")
	private String fraudBlockMessageFlag;
	
	@Value("${WVMS_SENDSMS_PORT}")
	private String smsPort;
	
	@Value("${WVMS_BLOCK_THRESHOLD}")
	private String blockThreshold;
	
	@Value("${WVMS_MIGRATION_FLAG}")
	private String migrationFlag;
	
	@Value("${WVMS_ACCESS_TYPE_ALLOWED_FOR_REDEEM}")
	private String allowedAccessTypeForRedeem;
	
	@Value("${WVMS_SERVICE_BUCKET_ID}")
	private String seviceBucketId;
	
	@Value("${WVMS_TIME_MULTIPLIER}")
	private String timeMultiplier;
	
	@Value("${WVMS_TOTAL_PROCESS_INSTANCES}")
	private int noOfProcessInstances;
	
	@Value("${WVMS_SERVER_UP}")
	private int serverUp;
	
	@Value("${WVMS_EAPI_GET_VOUCHER_DET_FLAG}")
	private String getVoucherDetFlag;
	
	@Value("${WVMS_MAX_CHECK_STATUS_TRIES}")
	private int maxCheckStatusTries;
	
	@Value("${WVMS_EACH_TRY_SLEEP}")
	private int eachTrysleep;
	
	@Value("${WVMS_ICP_API_TIMEOUT}")
	private int TIMEOUT;
	
	@Autowired
	private NamedParameterJdbcTemplate namedDbJdbcTemplate;
	
	@Autowired
	private Generic generic;
	
	@Autowired
    private GenericFunctions genericFunction;
	
	private String logFileName ="WVMS_VoucherRedemption.log";
	
	private int SLAB_COSVAL = 0, COS_COSVAL = 0, FREEBIE_COSVAL = 0, voucherStatusChecker = 1,processId=0;
	
	private Date expiryDate = null;
	
    Map<String,String> denomHash = new HashMap<String,String>();  
    Map<String,String> bonusHash = new HashMap<String,String>(); 
    
    private VoucherRedeemResponse voucherRedeemResponse = new VoucherRedeemResponse();
	private ArrayList<String> redeemVoucherList = new ArrayList<String>();
	
    private DenominationMast denominationDetails = new DenominationMast();
    private VoucherDet voucherDetails = new VoucherDet();
    private TransactionMast transactionDetails = new TransactionMast();
    private List<ReasonMast> reasonList;
	
	public VoucherRedeemResponse redeemVoucher(VoucherRedeemRequest voucherRedeemRequest) throws Exception
	{
		genericFunction.logFunction(logFileName,"::::::::::::::::::::::::::[redeemVoucher]:::::::::::::::::::::::::");
		String transactionId = generic.getTransactionId();
		String responseDescription ="" ,responseCode = "0000",query="";
		boolean responseData = false;
		try 
		{
			if(transactionId.equals(""))
			{
				responseDescription = "Unable to fetch transaction id";
				
				voucherRedeemResponse.setRespCode("1014");
				voucherRedeemResponse.setRespDesc(responseDescription);
				//throw new Exception("1014");
	            return voucherRedeemResponse;
			}
		
			//parse the request to check the request fields
			parseRequest(voucherRedeemRequest,transactionId);
			
			if(redeemVoucherList.contains(voucherRedeemRequest.toString()))
	        {
					responseDescription = "Multiple Duplicate Request";
					log.error(responseDescription);
				     
	                voucherRedeemResponse.setRespCode("1014");
	                voucherRedeemResponse.setRespDesc(responseDescription);
	                //throw new Exception("1014");
	                return voucherRedeemResponse;              
	        }
	        else
	        {
	                redeemVoucherList.add(voucherRedeemRequest.toString());
	        }
		
			//to validate msisdn, voucher number and serial number format against regular expression.
			validateData(voucherRedeemRequest,transactionId);
			
			if(migrationFlag.equals("1"))
	        {
					genericFunction.logFunction(logFileName,"[redeemVoucher]::::::::::migrationFlag::::::::::"+migrationFlag);
	                switch(String.valueOf(voucherRedeemRequest.getVoucherFlag()))
	                {
	                        case "2" :
	                        			genericFunction.logFunction(logFileName,"[redeemVoucher]:::::::::::(VOUCHER):::::::::::::");
	                        	        
	                        			query = "SELECT BATCH_ID,voucherDecrypt(VOUCHER_NUMBER),SERIAL_NUMBER,STATUS FROM VOUCHER_DET WHERE VOUCHER_NUMBER = voucherEncrypt(:voucherNumber)";
		                                responseData = getData(query,voucherRedeemRequest.getVoucherFlag(),voucherRedeemRequest,transactionId);
		                                
		                                genericFunction.logFunction(logFileName,"[redeemVoucher]:::::::::::responseData(VOUCHER):::::::::::::"+responseData);
		                                
		                                if(responseData == false)
		                                {
		                                		responseCode = manageCustomerMast(voucherRedeemRequest,transactionId);
		                                		genericFunction.logFunction(logFileName,"[redeemVoucher]:::::::::::responseCode(VOUCHER):::::::::::::"+responseCode);
		                                        throw new Exception(responseCode);
		                                }
		                                break;
	                        case "1" :
	                        	        query = "SELECT BATCH_ID,voucherDecrypt(VOUCHER_NUMBER),SERIAL_NUMBER,STATUS FROM VOUCHER_DET WHERE SERIAL_NUMBER =:serialNumber AND BATCH_ID =:batchId";
		                        		responseData = getData(query, voucherRedeemRequest.getVoucherFlag(),voucherRedeemRequest,transactionId);
		                        		genericFunction.logFunction(logFileName,"[redeemVoucher]:::::::::::responseData(SERIAL NUMBER):::::::::::::"+responseData);
		                        		
		                        		if(responseData == false)
		                                {
		                        				genericFunction.logFunction(logFileName,"[redeemVoucher]:::::::::::responseCode(SERIAL NUMBER):::::::::::::"+responseCode);
		                                        throw new Exception("1003");
		                                }
		                                break;
	                        default :
	                                	throw new Exception("1005");
	                }
	        }
			genericFunction.logFunction(logFileName,"::::;before insert into insertIntoTransactionMast:::::::");
			responseCode = insertIntoTransactionMast(voucherRedeemRequest,transactionId);
		}
		catch (Exception e)
		{
			 responseCode = e.getMessage();
             log.error("Exception in redeemVoucher"+e.getMessage());
             
             if(voucherStatusChecker == 2)
             {
                     try{
                             updateVoucherStatus(String.valueOf(voucherDetails.getSerialNumber()),String.valueOf(voucherDetails.getBatchId()), 1);

                     }catch(Exception ex)
                     {
                    	 responseCode = ex.getMessage();
                     }
             }

		}
		finally
        {
                redeemVoucherList.remove(voucherRedeemRequest.toString());
                voucherRedeemResponse = constructResponseJson(responseCode,null);
                genericFunction.logFunction(logFileName,"[processRequest] :: RESPONSE : "+voucherRedeemResponse);
                genericFunction.logFunction(logFileName,"****************Voucher Redemption completed****************");
        }



	
		return voucherRedeemResponse; 
	}
	
	//parse the request to check the request fields
	
	private VoucherRedeemResponse parseRequest(VoucherRedeemRequest voucherRedeemRequest,String transactionId)
	{
		genericFunction.logFunction(logFileName,":::::::::::parseRequest:::::::::::::::::::");
		if(voucherRedeemRequest.toString().isEmpty())
		{	
			genericFunction.logFunction(logFileName,"Request content is null");
			//throw new Exception("1005");
			voucherRedeemResponse.setRespCode("1005");
			voucherRedeemResponse.setRespDesc("Request content is null for transactionId:"+transactionId);
           
		}
		else if(voucherRedeemRequest.getVoucherFlag().isEmpty() || voucherRedeemRequest.getVoucherNo().isEmpty() || voucherRedeemRequest.getMsisdn().isEmpty())
		{	
			genericFunction.logFunction(logFileName,"Invalid Request for transactionId:"+transactionId);
			//throw new Exception("1005");
			voucherRedeemResponse.setRespCode("1005");
			voucherRedeemResponse.setRespDesc("Invalid Request for transactionId:"+transactionId);
            
		}
		return voucherRedeemResponse;	
		
		
	}

	
	//to validate msisdn, voucher number and serial number format against regular expression.
	
    private VoucherRedeemResponse validateData(VoucherRedeemRequest voucherRedeemRequest,String transactionId) throws Exception
    {
    		genericFunction.logFunction(logFileName,":::::::::::validateData:::::::::::::::::::");
    		String responseDescription="";
            try{
                    if(!voucherRedeemRequest.getMsisdn().matches(msisdnRegEx))
                    {
                    	    responseDescription="Msisdn Not Matching With RegEx for transactionId:"+transactionId;
                    	    genericFunction.logFunction(logFileName,responseDescription);
                            
                    	    //throw new Exception("1008");
                			voucherRedeemResponse.setRespCode("1008");
                			voucherRedeemResponse.setRespDesc(responseDescription);
                    }

                    switch(Integer.parseInt(voucherRedeemRequest.getVoucherFlag()))
                    {
                            case 1 :// serial number
                                    if(!voucherRedeemRequest.getVoucherNo().trim().matches(serialNoRegEx))
                                    {
                                    		responseDescription="Serial Number Not Matching With RegEx for transactionId:"+transactionId;
                                    		genericFunction.logFunction(logFileName,responseDescription);
                                    	    
                                            //throw new Exception("1003");
                                			voucherRedeemResponse.setRespCode("1003");
                                			voucherRedeemResponse.setRespDesc(responseDescription);
                                             
                                    }
                                    break;
                            case 2 :// Voucher number
                                    if(!voucherRedeemRequest.getVoucherNo().trim().matches(voucherNoRegEx))
                                    {
	                                    	responseDescription="Voucher number Not Matching With RegEx for transactionId:"+transactionId;
	                                    	genericFunction.logFunction(logFileName,responseDescription);
                                            
                                            String respCode = manageCustomerMast(voucherRedeemRequest,transactionId);
                                            //throw new Exception("respCode");
                                			voucherRedeemResponse.setRespCode(respCode);
                                			voucherRedeemResponse.setRespDesc(responseDescription);
                                  
                                    }
                                    break;
                    }


            }catch(NullPointerException e)
            {
	            	responseDescription="Request Data Not Available for transactionId:"+transactionId;
	            	genericFunction.logFunction(logFileName,responseDescription);
                
                    //throw new Exception("1004");
                    voucherRedeemResponse.setRespCode("1004");
        			voucherRedeemResponse.setRespDesc(responseDescription);
          
            }
            
            return voucherRedeemResponse;
    }
    
    //to check the blockSubscriber
    
	public String manageCustomerMast(VoucherRedeemRequest voucherRedeemRequest,String transactionId) throws Exception 
    {
		genericFunction.logFunction(logFileName,":::::::::::manageCustomerMast:::::::::::::::::::");

    	boolean isRecordsUpdated = false;
    	
		String query ="",str_fail_count = "0",responseCode = "1007";
        String blockResponse="";
        
        Map<String,String> params= new  HashMap<String,String>();
        JSONObject fraudJson = new JSONObject();
        
        Map<String,String> fraudReqhash = new HashMap<String,String>();
        fraudReqhash=Generic.stringToHash(fraudCallParams,",");
        
        Map<String,String> fraudBlockHash = new HashMap<String,String>();
        fraudBlockHash = Generic.stringToHash(fraudBlockMessageFlag,",");
        
        Map<String,String> messageHash = (Map<String, String>) getMessageHash();
        
        Date fraudDate = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
        
        try{
            if(!clientFlag.equals("1"))
            {
                    return responseCode;
            }
            query = "SELECT FAIL_COUNT FROM CUSTOMER_MAST WHERE SUBSCRIBER_MSISDN =:msisdn";
            params.put("msisdn",voucherRedeemRequest.getMsisdn());
            
            genericFunction.logFunction(logFileName,"[manageCustomerMast]::::::fecthing fail count from CUSTOMER_MAST:::::::::"+query);
            
            List<String> failCounts = namedDbJdbcTemplate.query(query, params, 
            	    (rs, rowNum) -> rs.getString("FAIL_COUNT")
            	);

        	if (failCounts.isEmpty()) {
        		genericFunction.logFunction(logFileName,"No fail count found for MSISDN: " + voucherRedeemRequest.getMsisdn());
        	    str_fail_count = "0"; // Handle this case as needed (e.g., default value or exception)
        	} else {
        	    str_fail_count = failCounts.get(0);
        	}
            
        	genericFunction.logFunction(logFileName,"[manageCustomerMast]::::::fail count from CUSTOMER_MAST for msisdn:::::::::"+voucherRedeemRequest.getMsisdn()+"::::::::str_fail_count::::"+str_fail_count);
        	genericFunction.logFunction(logFileName,"::::str_fail_count:::::"+str_fail_count+":::::::maxFailCount:::::"+maxFailCount+"::::::multiBlockCallFlag:::::::"+multiBlockCallFlag);
            
            
        	if(!failCounts.isEmpty())
            {	
            	fraudJson.put("accessNo",voucherRedeemRequest.getMsisdn());
            	fraudJson.put("appTxnRefId",transactionId);
            	fraudJson.put("ocsTxnRefId",transactionId);
            	fraudJson.put("genTime",dateFormat.format(fraudDate));
            	fraudJson.put("evrEvent",fraudReqhash.get("evrEvent"));
            	fraudJson.put("evrCause",fraudReqhash.get("evrCause"));
            	fraudJson.put("evrReason",fraudReqhash.get("evrReason"));
            	fraudJson.put("evrCategory",fraudReqhash.get("evrCategory"));
            	fraudJson.put("evtType",fraudReqhash.get("evtType"));
            	fraudJson.put("comment","Fraud Call");
            	fraudJson.put("nodeId","1");
            	fraudJson.put("moduleId","0");
            	fraudJson.put("subModuleId","0");
            	fraudJson.put("rtt","0");
            	
            	
            	if(Integer.parseInt(str_fail_count)>=maxFailCount)
            	{	
            		responseCode="1011";
            		if(multiBlockCallFlag == 2)
                    {
            			 genericFunction.logFunction(logFileName,"[manageCustomerMast]::::::multiple time block call initiation:::::::");
                         blockResponse = blockSubscriber(voucherRedeemRequest.getMsisdn(),blockSubscriberUrl,fraudJson,transactionId);
                         
                         genericFunction.logFunction(logFileName,":: ICP Fraud Response : "+blockResponse);
                         if(blockResponse.length()!= 0)
                         {
                        	 	 genericFunction.logFunction(logFileName," :: User with msisdn : "+voucherRedeemRequest.getMsisdn()+" blocked for fraud");
                                 JSONObject blockSubResponse=new JSONObject(blockResponse);                                    

                                 if(blockSubResponse.getInt("statusCode") == 0)
                                 {
                                         if(fraudBlockHash.get("2").equals("true"))
                                         {
                                                 sendSms(messageHash.get("1005").replace("__MSISDN__",voucherRedeemRequest.getMsisdn()).replace("__TXID__",transactionId), transactionId, smsPort, transactionId);
                                         }
                                         else
                                         {
                                        	 	 genericFunction.logFunction(logFileName,"[manageCustomerMast]::::Fraud SMS is disabled:::");
                                         }
                                 }
                                 else
                                 {
                                	 	 genericFunction.logFunction(logFileName,"[manageCustomerMast]:: failed to block in ICP");
                                         throw new Exception(""+blockSubResponse.getJSONObject("response").getInt("errorCode"))
;
                                 }
                         }
                         else
                         {
                        	 	 genericFunction.logFunction(logFileName,"[manageCustomerMast]:: Empty response from ICP");
                                 throw new Exception("1014");
                         }
                    }
            		else if(multiBlockCallFlag ==1  && Integer.parseInt(str_fail_count) == maxFailCount)
                    {
            				genericFunction.logFunction(logFileName,"one time block call initiation");
                            blockResponse = blockSubscriber(voucherRedeemRequest.getMsisdn(),blockSubscriberUrl, fraudJson,transactionId);
                            genericFunction.logFunction(logFileName,":: ICP Fraud Response : "+blockResponse);
                            
                            if(blockResponse.length()!= 0)
                            {
                            	    genericFunction.logFunction(logFileName,":: User with msisdn : "+voucherRedeemRequest.getMsisdn()+" blocked for fraud");
                                    JSONObject blockSubResponse=new JSONObject(blockResponse);
                                    if(blockSubResponse.getInt("statusCode") == 0)
                                    {
                                            if(fraudBlockHash.get("2").equals("true"))
                                            {
                                                    sendSms(messageHash.get("1005").replace("__MSISDN__",voucherRedeemRequest.getMsisdn()).replace("__TXID__",transactionId), voucherRedeemRequest.getMsisdn(), smsPort, transactionId);
                                            }
                                            else
                                            {
                                            	    genericFunction.logFunction(logFileName,"[manageCustomerMast]:: Fraud SMS is disabled");
                                            }
                                    }
                                    else
                                    {
                                    		genericFunction.logFunction(logFileName,"[manageCustomerMast]::failed to block in ICP");
                                            throw new Exception(""+blockSubResponse.getJSONObject("response").getInt("errorCode"));
                                    }
                            }
                            else
                            {
                            		genericFunction.logFunction(logFileName,"[manageCustomerMast]::Empty response from ICP");
                                    throw new Exception("1014");
                            }
                    }

            	}
            	else if(Integer.parseInt(blockThreshold) == Integer.parseInt(str_fail_count))
                {
            			genericFunction.logFunction(logFileName,"[manageCustomerMast]:::User Reached to maximum fraud Attempt");
                        if(fraudBlockHash.get("1").equals("true"))
                        {
                                sendSms(messageHash.get("1004"), voucherRedeemRequest.getMsisdn(), smsPort, transactionId);
                        }
                        else
                        {
                        		genericFunction.logFunction(logFileName,"[manageCustomerMast]:::: Pre Fraud SMS is disabled");
                        }
                }
            	
            	
            	query = "UPDATE CUSTOMER_MAST SET FAIL_COUNT =:failCount, LAST_MOD_DATE = SYSDATE WHERE SUBSCRIBER_MSISDN =:msisdn";
            	params.put("failCount",String.valueOf(Integer.parseInt(str_fail_count)+1));
            	params.put("msisdn", voucherRedeemRequest.getMsisdn());
            	
            	genericFunction.logFunction(logFileName,"[manageCustomerMast]::::::updating CUSTOMER_MAST::::::::"+query);
            	
            	try {
            		isRecordsUpdated = namedDbJdbcTemplate.update(query, params) > 0;
            		genericFunction.logFunction(logFileName,"[manageCustomerMast]:::::isRecordsUpdated to CUSTOMER_MAST:::::::"+isRecordsUpdated);
            	}
            	catch (Exception e)
            	{
            		genericFunction.logFunction(logFileName,"[manageCustomerMast]:::::Error in updating records to CUSTOMER_MAST:::::::"+e.getMessage());
            	}
            	
            }
            else
            {
            	genericFunction.logFunction(logFileName,"[manageCustomerMast]::::::::::inside else::::::::::::::::::::::");
            	query = "INSERT INTO CUSTOMER_MAST(SUBSCRIBER_MSISDN,FAIL_COUNT,LAST_MOD_DATE) VALUES(:msisdn,:str_fail_count,SYSDATE)";
            	
            	params.put("msisdn", voucherRedeemRequest.getMsisdn());
            	params.put("str_fail_count","1");
            	
            	genericFunction.logFunction(logFileName,"[manageCustomerMast]::::::Inserting CUSTOMER_MAST::::::::"+query);
            	
            	try {
            		isRecordsUpdated = namedDbJdbcTemplate.update(query, params) > 0;
            		genericFunction.logFunction(logFileName,"[manageCustomerMast]:::::isRecordsUpdated to CUSTOMER_MAST:::::::"+isRecordsUpdated);
            	}
            	catch (Exception e)
            	{
            		genericFunction.logFunction(logFileName,"[manageCustomerMast]:::::Error in Inserting records to CUSTOMER_MAST:::::::"+e.getMessage());
            	}
            	
            }
            
        }    
        catch(Exception e)
        {
        	genericFunction.logFunction(logFileName,"[manageCustomerMast]:::::Exception in manageCustomerMast:::::"+e.getMessage());
        }


		
    	return responseCode;
    	
    }
   

    //to call External Block subscriber API
	
    public String blockSubscriber(String msisdn,String url, JSONObject data, String transactionId) throws Exception {
        String output = "";

        genericFunction.logFunction(logFileName,"::::::::::::blockSubscriber:::::::");
        genericFunction.logFunction(logFileName,"[blockSubscriber]:: URL : " + url + " | data : " + data +" | userName : "+userName+"|password :"+password);

        try {
            // Create client and configure timeout properties
            ClientConfig clientConfig = new ClientConfig();
            HttpAuthenticationFeature auth = HttpAuthenticationFeature.basic(userName, password);
            clientConfig.register(auth);

            Client client = ClientBuilder.newClient(clientConfig);
            client.property(ClientProperties.CONNECT_TIMEOUT, 10000);
            client.property(ClientProperties.READ_TIMEOUT, TIMEOUT);

       
            url = url.replaceAll("__MSISDN__",msisdn);
            WebTarget webTarget = client.target(url);

            // Correct request usage with String "application/json"
            Invocation.Builder invocationBuilder = webTarget.request("application/json");

            // Correct use of Entity for sending a plain string
            Response response = invocationBuilder.post(Entity.text(data.toString()));

            genericFunction.logFunction(logFileName,"[blockSubscriber]:: Response Status ::: for transactionId ::: " + transactionId + " ::: is :: " + response.getStatus());

            if (response.getStatus() != 200) {
                log.warn(":: Block Request Rejected ");
                throw new Exception("1014");
            }

            // Process the response
            output = response.readEntity(String.class);
            output = output.replace("\n", "");

            genericFunction.logFunction(logFileName,"[blockSubscriber]::Response for transactionId ::: " + transactionId + " ::: is ::: " + output);
        } catch (Exception st) {
        	genericFunction.logFunction(logFileName,"[blockSubscriber]::Exception in blockSubscriber: " + st.getMessage());
            throw new Exception("1014");
        }

        return output;
    }


    
    //to insert the records into OUT_SMS_Q table
    
    private int sendSms(String message, String msisdn, String smsPort, String transactionId)throws Exception
    {
    	
    	    String query ="";
    	    int recordsInsertedCount = 0;
    	    HashMap<String,String> params = new  HashMap<String,String>();
    	    
    	    query = "INSERT INTO OUT_SMS_Q(MSG_ID,DEST_MSISDN,FROM_MSISDN,MESSAGE,DATE_TIME,MSG_STAT,TRANSACTION_ID) values(OUT_SMS_Q_SEQ.nextval,:msisdn,:smsPort,:message,sysdate,'N',:transactionId)";
            params.put("msisdn",msisdn);
            params.put("smsPort",smsPort);
            params.put("message",message);
            params.put("transactionId",transactionId);
            
            genericFunction.logFunction(logFileName,"[sendSms]:::INSERT query:::::::"+query);
            
            try {  	
            	recordsInsertedCount = namedDbJdbcTemplate.update(query, params);
            	genericFunction.logFunction(logFileName,"[sendSms]:::::recordsInsertedCount:::::::"+recordsInsertedCount);
            }
            catch (Exception e)
            {
            	recordsInsertedCount= 0 ;
            	genericFunction.logFunction(logFileName,"[sendSms]:::Error in Insertion to OUT_SMS_Q:::::"+e.getMessage());
            	
            }
            return recordsInsertedCount;
            
    }

    //to fetch the message from WVMS_MESSAGE_MAST_1 table
    
    private Map<String, String> getMessageHash() {
    	genericFunction.logFunction(logFileName,"::::::getMessageHash:::::::::");
        String query = "";
        Map<String, String> messageHash = new HashMap<>();
        try {
            query = "SELECT MESSAGE_ID, MESSAGE FROM WVMS_MESSAGE_MAST_1 WHERE MESSAGE_ID IN (1004, 1005)";
            
            // Query and map each row to a key-value pair in the map
            messageHash = namedDbJdbcTemplate.query(query, new HashMap<>(), 
                (rs, rowNum) -> {
                    // Create a map entry with MESSAGE_ID as the key and MESSAGE as the value
                    return new AbstractMap.SimpleEntry<>(rs.getString("MESSAGE_ID"), rs.getString("MESSAGE"));
                }).stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)); // Collect into a Map
            
            genericFunction.logFunction(logFileName,"[getMessageHash]::::::messageHash:::::::::" + messageHash);

        } catch (Exception e) {
        	genericFunction.logFunction(logFileName,"[getMessageHash]::::::::::Exception in getMessageHash: " + e.getMessage());
        }

        return messageHash;
    }


    //to fetch voucher data from database and load it into hash (denomHash and bonusHash)
    
    private boolean getData(String query, String voucherFlag,VoucherRedeemRequest voucherRedeemRequest,String transactionId) throws Exception
    {

    	genericFunction.logFunction(logFileName,"::::::::::::::getData::::::::::::::::");
    	genericFunction.logFunction(logFileName,"[getData]:::::::::::::query to fetch details based on voucher flag:::::::::::::::::::"+query);
    	
    	String batchId = "",denominationId = "", freebieId = "", slabId = "", cosId = "", bonusId = "", batchStatus = "",serialNumber="";
        int voucherStatus = 1;
        long voucherNumber=0;
        Date enableDate = null;
        
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
        
        BatchMast batchDetails = new BatchMast();
        HashMap<String,String> params = new HashMap<String,String>();
        
        
        try {
        	
        	switch(voucherFlag)
            {
                    case "1" :
                    	    //query = "SELECT BATCH_ID,voucherDecrypt(VOUCHER_NUMBER),SERIAL_NUMBER,STATUS FROM VOUCHER_DET WHERE SERIAL_NUMBER = :serialNumber AND BATCH_ID = :batchId";
                            batchId = voucherRedeemRequest.getVoucherNo().substring(0,4);
                            serialNumber = voucherRedeemRequest.getVoucherNo().substring(4);
                    
                            params.put("serialNumber",Integer.toString((Integer.parseInt(serialNumber))));
                            params.put("batchId",Integer.toString((Integer.parseInt(batchId))));
                            
                            break;
                    case "2" :
                    	    //query = "SELECT BATCH_ID,voucherDecrypt(VOUCHER_NUMBER),SERIAL_NUMBER,STATUS FROM VOUCHER_DET WHERE VOUCHER_NUMBER = voucherEncrypt(:voucherNumber)";
                    	    params.put("voucherNumber", voucherRedeemRequest.getVoucherNo());
                            break;
            }
            
        	voucherDetails = namedDbJdbcTemplate.query(query,params,
        			
        			 (rs,rowNum) -> new VoucherDet(
        					       rs.getInt("BATCH_ID"),
        					       rs.getLong("voucherDecrypt(VOUCHER_NUMBER)"),
        					       rs.getInt("SERIAL_NUMBER"),
        					       rs.getInt("STATUS")
        					 )
        			).get(0);
        	
        	if(!String.valueOf(voucherDetails.getBatchId()).isEmpty())
        	{	
        		batchId = String.valueOf(voucherDetails.getBatchId());
        		voucherNumber = voucherDetails.getVoucherNumber();
        		serialNumber = String.valueOf(voucherDetails.getSerialNumber());
                voucherStatus = voucherDetails.getStatus();
                
                genericFunction.logFunction(logFileName,"[getData]:::batchId::::"+batchId+"::::::voucherNumber::::::"+voucherNumber+":::::::serialNumber:::::::"+serialNumber+":::::::voucherStatus::::::"+voucherStatus);
            	
        	}
        	else
        	{
        		genericFunction.logFunction(logFileName,"[getData]:::::Voucher details not found for transactionId:::::"+transactionId);
                return false; // voucher details not found

        	}
        }
        catch(Exception e)
        {
        	genericFunction.logFunction(logFileName,"[getData]:::::::::Exception in fetching details based on voucherFlag::::::::::"+e.getMessage());
        	 throw new Exception("1014");
        }
        
        
        switch(voucherStatus)
        {
        
                
                case 0 :
                	    genericFunction.logFunction(logFileName,"[getData]::::Voucher is genrated but not active");
                        return false;
                        

                case 2 : genericFunction.logFunction(logFileName,"[getData]::::Voucher is processing"); //getting processed
                         break;
                case 4 : // Timed-out
                        String tmpMsisdn = getCustomerDet(voucherNumber,batchId);
                        if(voucherRedeemRequest.getMsisdn().equals(tmpMsisdn))
                        {
                        	    genericFunction.logFunction(logFileName,"[getData]::::Voucher is in timeout state");
                                throw new Exception("1009"); // Voucher is being processed
                        }
                        else
                        {
                        	    genericFunction.logFunction(logFileName,"[getData]::::Voucher is redeemed for other user");
                                throw new Exception("1010"); // Voucher is redeemed for other user
                        }
                       
                case 3 :
                		genericFunction.logFunction(logFileName,"[getData]::::Voucher is redeemed");
                        throw new Exception("1010"); // Voucher is redeemed
                case 5 :
                		genericFunction.logFunction(logFileName,"[getData]::::Voucher is expired");
                        return false; // Voucher is Expired
        }
        genericFunction.logFunction(logFileName,"[getData]::::::::::::::::::::after vouchflag switch::::::::::::::::::::::::");
        updateVoucherStatus(serialNumber, batchId, 2);
        
        if(!batchId.equals(""))
        {	
        	
        	query ="SELECT BATCH_ID,STATUS,DENOMINATION_ID,BONUS_ID,ENABLE_DATE FROM BATCH_MAST WHERE BATCH_ID =:batchId";
        	params.put("batchId", batchId);
        	
        	genericFunction.logFunction(logFileName,"[getData]::::::::::: query to fetch batch details:::::::::::::::::"+query);
        	
        	try{
        		 batchDetails = namedDbJdbcTemplate.query(query, params,
        	
        			
        			   (rs,rowNum)-> new BatchMast(
        					   rs.getInt("BATCH_ID"),
        					   rs.getInt("STATUS"),
        					   rs.getInt("DENOMINATION_ID"),
        					   rs.getInt("BONUS_ID"),
        					   rs.getString("ENABLE_DATE")  
        					   )
        			   
	        	).get(0);
        		denominationId = String.valueOf(batchDetails.getDenomainationId());
        		genericFunction.logFunction(logFileName,"[getData]:::::::::::batch details:::::::::::::::::"+batchDetails);
        	}
        	catch(Exception e)
        	{
        		genericFunction.logFunction(logFileName,"[getData]:::::::::::Exception while fetching batch details:::::::::::::::::"+e.getMessage());	
        		throw new Exception("1014");
        	}
        	
        	if(!denominationId.equals(null))
        	{	
        		genericFunction.logFunction(logFileName,"[getData]:::::::::::bonusId::::::::::::::"+batchDetails.getBonusId());
        		if(!String.valueOf(batchDetails.getBonusId()).equals(null) && !String.valueOf(batchDetails.getBonusId()).equals(""))
                {
                    bonusHash = getDenomDetails(String.valueOf(batchDetails.getBonusId()));
                    genericFunction.logFunction(logFileName,"[getData]:::::::::::bonusHash hash:::::::::::::::::"+bonusHash);
                      
                }
        		if(!denominationId.equals(""))
        		{	
        			denomHash = getDenomDetails(denominationId);
        			genericFunction.logFunction(logFileName,"[getData]:::::::::::denomination hash:::::::::::::::::"+denomHash);	
        		}
        		if(batchDetails.getEnableDate()!=null && batchDetails.getEnableDate().isEmpty())
        		{
        			if(checkExpiry(dateFormat.parse(batchDetails.getEnableDate())) == false || denomHash.isEmpty())
        			{	     
    	                     return false;
    	            }
        		}
	    		
	    		
        		
        	}
        	
        }
        else
        {	
        	genericFunction.logFunction(logFileName,"[getData]::::Batch details are not present");
            return false; 
        }

    	return true;
    }	
    
    
    private String getCustomerDet(long voucherNumber,String batchId) throws Exception
    {
    	    genericFunction.logFunction(logFileName,"::::::::::::::::getCustomerDet::::::::::::::::");
            String tmpMsisdn = "",query="";
            HashMap<String,String> params = new HashMap<String,String>();
            try{
                    query = "SELECT SUBSCRIBER_MSISDN FROM TRANSACTION_MAST WHERE VOUCHER_NUMBER =:voucherNumber AND BATCH_NUMBER = :batchId";
                    params.put("voucherNumber",String.valueOf(voucherNumber));
                    params.put("batchId", batchId);
                    
                    genericFunction.logFunction(logFileName,"[getCustomerDet]::::::query to fetch details from TRANSACTION_MAST:::::::::"+query);
                    tmpMsisdn = namedDbJdbcTemplate.queryForObject(query, params, String.class);
                  
            }catch(Exception e)
            {
            		genericFunction.logFunction(logFileName,"[getCustomerDet]::::::Error while fetching details from TRANSACTION_MAST:::::: "+e.getMessage());
                    throw new Exception("1014");
            }
            return tmpMsisdn;
    }

    

    private void updateVoucherStatus(String voucherNo, String batchId, int status)throws Exception
    {
    		genericFunction.logFunction(logFileName,":::::::::::::::::::::updateVoucherStatus::::::::::::::::");
            voucherStatusChecker = status;
            String query="";
            boolean isRecorsUpdated =false;
            HashMap<String,String> params = new HashMap<String,String>();
            try{
                    if(migrationFlag.equals("1"))
                    {
                    	query = "UPDATE VOUCHER_DET SET STATUS =:status WHERE SERIAL_NUMBER =:voucherNo AND BATCH_ID = :batchId";
                        params.put("status",String.valueOf(status));
                        params.put("voucherNo",voucherNo);
                        params.put("batchId",batchId);
                        
                        genericFunction.logFunction(logFileName,"[updateVoucherStatus]:::::::;query to update VOUCHER_DET:::::::::"+query);
                        
                        isRecorsUpdated = namedDbJdbcTemplate.update(query, params)>0;
                        
                    }
            }catch(Exception e)
            {
            		genericFunction.logFunction(logFileName,"[updateVoucherStatus]:::::::::;Exception in updateVoucherStatus::::::"+e.getMessage());
                    throw new Exception("1014");
            }
    }
    
    
    //to fetch denomination details
    private Map<String,String> getDenomDetails(String denominationId) throws Exception
    {
    	genericFunction.logFunction(logFileName,":::::::::::::::::::::getDenomDetails::::::::::::::::");
    	String slabId = "", cosId = "", freebieId = "", accessType = "",query="";
    	int bonusReqMode = 0;
    	
    	Map<String,String> responseHash = new HashMap<String,String>();
    	Map<String,String> params = new HashMap<String,String>();
    	Map<String,Integer> paramsInt = new HashMap<String,Integer>();
        ArrayList<Integer> cosList = new ArrayList<Integer>();
        Map<String,String> allowedAccessTypes = generic.stringToHash(allowedAccessTypeForRedeem,",");
        
        Map<String,String> bucketIdHash = generic.stringToHash(seviceBucketId,",");
        
        
        try 
        {
        	if(!denominationId.equals(""))
        	{
        		query="SELECT SLAB_ID,FREEBEE_ID,COS_ID,MODE_TYPE,DENOMINATION_VALIDITY,VALIDITY_TYPE,NVL(AMOUNT,0) AS AMOUNT,ACCESS_TYPE FROM DENOMINATION_MAST WHERE DENOMINATION_ID =:denominationId";
        	    params.put("denominationId",denominationId);
        	    
        	    genericFunction.logFunction(logFileName,"[getDenomDetails]::::::::::query to fetch denomination details::::::::::::::::"+query);
        	    RowMapper<DenominationMast> rowMapper = (rs, rowNum) -> new DenominationMast(
        	    	    rs.getInt("SLAB_ID"),
        	    	    rs.getInt("FREEBEE_ID"),
        	    	    rs.getInt("COS_ID"),
        	    	    rs.getInt("MODE_TYPE"),
        	    	    rs.getInt("DENOMINATION_VALIDITY"),
        	    	    rs.getInt("VALIDITY_TYPE"),
        	    	    rs.getInt("AMOUNT"),
        	    	    rs.getInt("ACCESS_TYPE")
        	    	);

        	    denominationDetails = namedDbJdbcTemplate.query(query, params, rowMapper).get(0);
        	    		
        	    genericFunction.logFunction(logFileName,"[getDenomDetails]::::::::::denomination details::::::::::::::::"+denominationDetails);
        	  	
        	}
        	
        	if(!String.valueOf(denominationDetails.getAccessType()).equals("") && !String.valueOf(denominationDetails.getAccessType()).equals(null))
        	{
        		genericFunction.logFunction(logFileName,"[getDenomDetails] :: Card Access type : "+denominationDetails.getAccessType());
        		genericFunction.logFunction(logFileName,"[getDenomDetails]:::::::::allowedAccessTypes:::::::::::"+allowedAccessTypes+"::::::accessType::::"+denominationDetails.getAccessType());
        		if (accessType != null && !accessType.isEmpty() 
        		        && allowedAccessTypes.containsKey(accessType)) {
        		    String accessTypeValue = allowedAccessTypes.get(accessType);
        		    
        		    if (accessTypeValue != null && accessTypeValue.equals("0")) {
        		    	genericFunction.logFunction(logFileName,"[getDenomDetails] :: Card Access Type Not Allowed:::::");
        		      
        		    	return responseHash;
        		    }
        		}

        	}
        	
        	 if(denominationDetails.getModeType()!= 0)
             {
        		 	 genericFunction.logFunction(logFileName,"[getDenomDetails]:::::::::getModeType:::::::::::"+denominationDetails.getModeType());
        		     
                     cosList = Generic.getCosValues(denominationDetails.getModeType());
                     genericFunction.logFunction(logFileName,"[getDenomDetails] :: Denomination Id : "+denominationId+"|Cos list : "+cosList+"| Username : "+userName+"| bonusReqMode : "+denominationDetails.getModeType());
                     if(!cosList.contains(Integer.parseInt(userName)))
                     {
                    	 	 genericFunction.logFunction(logFileName,"[getDenomDetails] :: Request Mode Not Applicable For Bonus");
                             return responseHash;
                     }
             }

        	 genericFunction.logFunction(logFileName,"[getDenomDetails]:::denominationDetails.getSlabId():::::::::"+denominationDetails.getSlabId());
        	 if(!String.valueOf(denominationDetails.getSlabId()).equals("-1") && !String.valueOf(denominationDetails.getSlabId()).equals(null) && !String.valueOf(denominationDetails.getSlabId()).equals(""))
             {
        		       
        		 	   genericFunction.logFunction(logFileName,"[getDenomDetails]::::to fetch the Talktime details for id:::::::::"+denominationDetails.getSlabId());
                      
        		       query="SELECT SLAB_AMOUNT,SLAB_VALIDITY FROM RECHARGE_MAST WHERE SLAB_ID =:slabId";
        		       params.put("slabId",String.valueOf(denominationDetails.getSlabId()));
        		       
        		       genericFunction.logFunction(logFileName,"[getDenomDetails]:::: query to fetch the Talktime details::::::;"+query);
                       
        		       RechargeMast slabDetails = namedDbJdbcTemplate.query(query, params,
        		    	    
        		    		   (rs,rowNum) -> new RechargeMast(	         
        		    				        rs.getInt("SLAB_AMOUNT"),
        		    				        rs.getInt("SLAB_VALIDITY")
        		    				  )
        		       ).get(0);
        		       
        		       genericFunction.logFunction(logFileName,"[getDenomDetails]::::Talktime details:::::::::"+slabDetails);
                       
        		       if(!String.valueOf(slabDetails.getSlabAmount()).isEmpty() && !String.valueOf(slabDetails.getSlabValidity()).isEmpty())
        		       {
        		    	   responseHash.put("TALKTIME",bucketIdHash.get("TALKTIME")+"|"+slabDetails.getSlabAmount()+"|"+slabDetails.getSlabValidity());
        		       }
                  
             }
        	 
        	  if(!String.valueOf(denominationDetails.getFreeBeeId()).equals(null) && !String.valueOf(denominationDetails.getFreeBeeId()).equals(""))
              {
        		  
        		  	  genericFunction.logFunction(logFileName,"[getDenomDetails]:::::::::Credit details for id:::::::"+denominationDetails.getFreeBeeId());
                      
        		      query = "SELECT FREEBEE_TYPE,FREEBEE_VALUE,FREEBEE_VALIDITY FROM FREEBEE_DET WHERE FREEBEE_ID =:freebeeId";
        		      paramsInt.put("freebeeId",denominationDetails.getFreeBeeId());
        		      
        		      genericFunction.logFunction(logFileName,"[getDenomDetails]::::::::::query to fetch credit/freebee details::::::::::::"+query);
        		      List<FreeBeeDet> creditDetails = namedDbJdbcTemplate.query(query, paramsInt,
        		    		    
        		    		        (rs,rowNum) -> new FreeBeeDet(
        		    		        		       rs.getString("FREEBEE_TYPE"),
        		    		        		       rs.getInt("FREEBEE_VALUE"),
        		    		        		       rs.getInt("FREEBEE_VALIDITY")
        		    		        		)
        		    		  );
        		      
        		      genericFunction.logFunction(logFileName,"[getDenomDetails]::::::::::credit/freebee details::::::::::::"+creditDetails);
                    
        		      for (FreeBeeDet credit : creditDetails)
        		      {	  
        		    	  responseHash.put(String.valueOf(credit.getFreeBeeType()),bucketIdHash.get(credit.getFreeBeeType())+"|"+credit.getFreeBeeValue()+"|"+credit.getFreebeeValidity());
        		      }
                      
              }


        }
        catch(Exception e)
        {
        	genericFunction.logFunction(logFileName,"[getDenomDetails]::::::Exception in fetching denomaination details:::::::::::"+e.getMessage());
        }
        
        return responseHash;
    	
    }


    private boolean checkExpiry(Date enableDate) throws Exception
    {
    		genericFunction.logFunction(logFileName,"::::::::::checkExpiry::::::::::::::::::::");
    		genericFunction.logFunction(logFileName,"::::::::enableDate:::::::::::::"+enableDate);
    	    Map<String,String> timeMultiplierHash = Generic.stringToHash(timeMultiplier,",");
            boolean result = true;
            try{
                    Date today = new Date();
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(enableDate);
                    int multiplier = Integer.parseInt(timeMultiplierHash.get(denominationDetails.getValidationType()));
                    multiplier = denominationDetails.getDenominationValidity() * multiplier;
                    cal.add(Calendar.DATE,multiplier);
                   

                    if (expiryDate != null) {
                        expiryDate = cal.getTime();

                        if (expiryDate != null && !expiryDate.after(today)) {
                        	genericFunction.logFunction(logFileName,"[checkExpiry] :: Voucher is expired");
                        	genericFunction.logFunction(logFileName,"[checkExpiry] :: Expiry Date : " + expiryDate + " | Current Date : " + today);
                            result = false;
                        }
                    } else {
                    	genericFunction.logFunction(logFileName,"[checkExpiry] :: Expiry Date is null, cannot proceed with expiry check");
                        result = false;
                    }
                    

            }catch(Exception e)
            {
            		genericFunction.logFunction(logFileName,"[checkExpiry] :: "+e.getMessage());
                    throw new Exception("1014");
            }
            return result;

    }

    //to insert records into TRANSACTION_MAST table
    private String insertIntoTransactionMast(VoucherRedeemRequest voucherRedeemRequest,String transactionId)throws Exception
    {
    	genericFunction.logFunction(logFileName,"::::::::::::insertIntoTransactionMast::::::");
    	
    	int res = 0, rtbsProcessId = 0;
    	String query="";
    	boolean isRecordInserted = false,isRecordUpdated=false;
    	HashMap<String,String> params= new HashMap<String,String>();
        Map<String,String> eapiUserFlag = Generic.stringToHash(getVoucherDetFlag,",");
    	
        if(clientFlag.equals("1"))
        {
        	if(!denomHash.isEmpty())
            {
        			genericFunction.logFunction(logFileName,"[insertIntoTransactionMast]::::::::::::denomHash::::::"+denomHash);
                    for(String key : denomHash.keySet())
                    {
                    	    int isRecordInsertedIntoTransactionDet =  insertIntoTransactionDet(denomHash.get(key).split("\\|")[0],denomHash.get(key).split("\\|")[1],denomHash.get(key).split("\\|")[2],"1",transactionId+"1",key,transactionId);
                            res = res + isRecordInsertedIntoTransactionDet;
                            
                            genericFunction.logFunction(logFileName,"[insertIntoTransactionMast]:::::RecordInsertedIntoTransactionDet(Denomination):::::::"+ isRecordInsertedIntoTransactionDet);
                            
                            if(key.equals("TALKTIME"))
                            {
                                    SLAB_COSVAL = 1;
                            }
                            else if(key.equals("COS"))
                            {
                                    COS_COSVAL = 2;
                            }
                            else
                            {
                                    FREEBIE_COSVAL = 4;
                            }
                    }
            }
        	
            if(!bonusHash.isEmpty())
            {
            		genericFunction.logFunction(logFileName,"[insertIntoTransactionMast]::::::::::::bonusHash::::::"+bonusHash);
                    int counter = 2;
            		for(String key : bonusHash.keySet())
                    {
            			    //int isRecordInsertedIntoTransactionDetBonus = insertIntoTransactionDet(bonusHash.get(key).split("\\|")[0],bonusHash.get(key).split("\\|")[1],bonusHash.get(key).split("\\|")[2],"0",transactionId+String.valueOf(counter),key,transactionId);
                    	    int isRecordInsertedIntoTransactionDetBonus = insertIntoTransactionDet(bonusHash.get(key).split("\\|")[0],bonusHash.get(key).split("\\|")[1],bonusHash.get(key).split("\\|")[2],"0",transactionId+counter,key,transactionId);
                            res = res + isRecordInsertedIntoTransactionDetBonus;
                            
                            genericFunction.logFunction(logFileName,"[insertIntoTransactionMast]::::::RecordInsertedIntoTransactionDet(Bonus)::::::::"+isRecordInsertedIntoTransactionDetBonus);
                            if(key.equals("TALKTIME"))
                            {
                                    SLAB_COSVAL = 1;
                            }
                            else if(key.equals("COS"))
                            {
                                    COS_COSVAL = 2;
                            }
                            else
                            {
                                    FREEBIE_COSVAL = 4;
                            }
                            counter = counter+1;
                    }
            }

        }
        else
        {
        		genericFunction.logFunction(logFileName,"insertIntoTransactionMast :: RTBS is Active host");
                res = 1;
                rtbsProcessId = 10;
        }


        if(res != 0)
        {
                processId = generic.getProcesId(noOfProcessInstances, serverUp) + rtbsProcessId;
                if(migrationFlag.equals("1"))
                {
                		genericFunction.logFunction(logFileName,"[insertIntoTransactionMast]:::::::::MSISDN:::::::::::::"+voucherRedeemRequest.getMsisdn());
                	    query="INSERT INTO TRANSACTION_MAST(SEQ_ID,TRANSACTION_ID,REQ_DATE,SUBSCRIBER_MSISDN,STATUS,BATCH_NUMBER,REQ_MODE,VOUCHER_NUMBER,PROCESS_ID,APPLICABLE_COS,SERIAL_NUMBER,REQ_TYPE,VOUCHER_AMOUNT,EXPIRY_DATE) VALUES(TRANSACTION_MAST_SEQ.NEXTVAL,:transactionId,SYSDATE,:msisdn,0,:batchNo,:username,:voucherNo,:processId,:cosVal,:serialNo,:voucherFlag,:voucherAmount,:expiryDate)";
                	    
                	    params.put("transactionId",transactionId);
                	    params.put("msisdn", voucherRedeemRequest.getMsisdn());
                	    params.put("batchNo",String.valueOf(voucherDetails.getBatchId()));
                	    params.put("username",eapiUserFlag.get("WVMS"));
                	    params.put("voucherNo",String.valueOf(voucherDetails.getVoucherNumber()));
                	    params.put("processId",String.valueOf(processId));
                	    params.put("cosVal", Integer.toString(SLAB_COSVAL + COS_COSVAL + FREEBIE_COSVAL));
                	    params.put("serialNo",Integer.toString(voucherDetails.getSerialNumber()));
                	    params.put("voucherFlag",voucherRedeemRequest.getVoucherFlag());
                	    params.put("voucherAmount",String.valueOf(denominationDetails.getAmount()));
                	    if(expiryDate!=null	)
                	    {
                	    	params.put("expiryDate",String.valueOf(new java.sql.Date(expiryDate.getTime())));
                	    }
                	    else                	    
                	    {
                	    	params.put("expiryDate",String.valueOf(""));
                	    }
                	    	
                	    
	    
                }
                else
                {
                		genericFunction.logFunction(logFileName,"[insertIntoTransactionMast]:::::::::MSISDN:::::::::::::"+voucherRedeemRequest.getMsisdn());
            	        query = "INSERT INTO TRANSACTION_MAST(SEQ_ID,TRANSACTION_ID,REQ_DATE,SUBSCRIBER_MSISDN,STATUS,REQ_MODE,PROCESS_ID,REQ_TYPE,VOUCHER_NUMBER) VALUES(TRANSACTION_MAST_SEQ.NEXTVAL,:transactionId,SYSDATE,:msisdn,0,:username,:processId,:voucherFlag,:voucherNo)";
                      
            	        params.put("transactionId",transactionId);
                	    params.put("msisdn", voucherRedeemRequest.getMsisdn());
                	    params.put("username",eapiUserFlag.get("WVMS"));
                	    params.put("processId",String.valueOf(processId));
                	    params.put("voucherFlag",voucherRedeemRequest.getVoucherFlag());
                	    params.put("voucherNo",String.valueOf(voucherDetails.getVoucherNumber()));
                	    
            	       
                }
                genericFunction.logFunction(logFileName,"[insertIntoTransactionMast]::::::query to insert into TRANSACTION_MAST table::::::::"+query);                       
                
                try {
                	isRecordInserted = namedDbJdbcTemplate.update(query,params)>0;
                	
                	genericFunction.logFunction(logFileName,"[insertIntoTransactionMast]::::::isRecordInserted in TRANSACTION_MAST table:::::::"+isRecordInserted);
                }
                catch(Exception e)
                {
                	genericFunction.logFunction(logFileName,"[insertIntoTransactionMast] :: Record not inserted in TRANSACTION_MAST table:::::::"+e.getMessage());
                    throw new Exception("1014");
                }
            

                if(isRecordInserted)
                {
                	      int customerMastRecordCount = 0;
                	       
                		  genericFunction.logFunction(logFileName,"[insertIntoTransactionMast] :: Record inserted inserted into transaction table");
                		  		  
                		  query = "SELECT COUNT(*) FROM CUSTOMER_MAST WHERE SUBSCRIBER_MSISDN=:msisdn";
                		  params.put("msisdn",voucherRedeemRequest.getMsisdn());
                		  
                		  genericFunction.logFunction(logFileName,"[insertIntoTransactionMast] :: query to check in msisdn exist in CUSTOMER_MAST table:::::::"+query);
                		  
                		  customerMastRecordCount = namedDbJdbcTemplate.queryForObject(query,params,Integer.class);
                		  
                		  genericFunction.logFunction(logFileName,"[insertIntoTransactionMast] :::::::customerMastRecordCount:::::::"+customerMastRecordCount);
                		  
                		  genericFunction.logFunction(logFileName,"[insertIntoTransactionMast] :: query to update CUSTOMER_MAST table:::::::"+query);
                		  if(customerMastRecordCount>0)
                		  {	  
                			  query = "UPDATE CUSTOMER_MAST SET FAIL_COUNT =:failCount ,LAST_MOD_DATE = SYSDATE WHERE SUBSCRIBER_MSISDN =:msisdn";
                		  }
                		  else
                		  {	  
                			  query = "INSERT INTO CUSTOMER_MAST(SUBSCRIBER_MSISDN,FAIL_COUNT,LAST_MOD_DATE) VALUES(:msisdn,:failCount,SYSDATE)";
                		  }
                          
 
                          params.put("failCount","0");
                          params.put("msisdn",voucherRedeemRequest.getMsisdn());
                          
                          genericFunction.logFunction(logFileName,"[insertIntoTransactionMast] :: query to update CUSTOMER_MAST table:::::::"+query);
                         
                          try 
                          {
                        	  isRecordUpdated=namedDbJdbcTemplate.update(query,params)>0;
                        	  genericFunction.logFunction(logFileName,"[insertIntoTransactionMast]::::::isRecordUpdated in CUSTOMER_MAST table:::::::"+isRecordUpdated);
                          }
                          catch(Exception e)
                          {
                        	  genericFunction.logFunction(logFileName,"[insertIntoTransactionMast] :: Record not updated in CUSTOMER_MAST table::::::::::"+e.getMessage());
                        	  throw new Exception("1014");
                          }
                          return checkStatus(transactionId);
                }
        }
        else
        {
        		genericFunction.logFunction(logFileName,"[insertIntoTransactionMast] :: Record not inserted in Transaction det");
                throw new Exception("1014");
        }

    	return "0000";
    }
    
    
    //to insert records into TRANSACTION_DET table
    private int insertIntoTransactionDet(String bucketId, String value, String validity, String transType, String subTransId, String bucketName,String transactionId) throws Exception
    {
    	genericFunction.logFunction(logFileName,":::::::::::insertIntoTransactionDet::::::");
    	genericFunction.logFunction(logFileName,"[insertIntoTransactionDet] :: bucketId :"+bucketId+"|value :"+value+"|validity :"+validity+"|transType :"+transType+"|subTransId :"+subTransId+"|bucketName :"+bucketName);
           
        String query="";
        int isRecordInserted = 0;
        HashMap<String,String> params = new HashMap<String,String>();
        
        query = "INSERT INTO TRANSACTION_DET(SERVICE_TYPE,SERVICE_UNIT,SERVICE_STATUS,SERVICE_VALIDITY,REQUEST_DATE,TRANS_TYPE,MAIN_TRANS_ID,TRANSACTION_ID,SERVICE_NAME) VALUES(:bucketId,:value,0,:validity,SYSDATE,:transType,:transactionId,:subTransId,:bucketName)";
        
        params.put("bucketId",bucketId);
        params.put("value",value);
        params.put("validity", validity);
        params.put("transType",transType);
        params.put("transactionId",transactionId);
        params.put("subTransId",subTransId);
        params.put("bucketName",bucketName);
        
        genericFunction.logFunction(logFileName,"[insertIntoTransactionDet]:::::::::::query to insert record into TRANSACTION_DET table::::::::::::"+query);
        try
        {
        		isRecordInserted = namedDbJdbcTemplate.update(query, params);

        }catch(Exception e)
        {
        		genericFunction.logFunction(logFileName,"[insertIntoTransactionDet] ::"+e.getMessage());
        		genericFunction.logFunction(logFileName,"[insertIntoTransactionDet] :: bucketId :"+bucketId+"|value :"+value+"|validity :"+validity+"|transType :"+transType+"|subTransId :"+subTransId+"|bucketName :"+bucketName);
                return 0;
        }
        return isRecordInserted;
    }
    
    
    private String checkStatus(String transactionId) throws Exception {
    	genericFunction.logFunction(logFileName,":::::::::::checkStatus::::::::::");
        
        int status = 0;
        String responseCode = "", query = "";    
        boolean isRecordsUpdated = false;
        HashMap<String, String> params = new HashMap<>();
        List<TransactionMast> transactionList = null;
        
        try {
            TimeUnit.SECONDS.sleep(1);

            query = "SELECT STATUS, RESP_CODE, RESP_DESC, IVR_BALANCE_INFO FROM TRANSACTION_MAST " +
                    "WHERE TRANSACTION_ID = :transactionId AND STATUS != 0";                    
            params.put("transactionId", transactionId);

            genericFunction.logFunction(logFileName,"[checkStatus] Query to fetch status, responseCode, balanceInfo from TRANSACTION_MAST:"+ query);

            for (int i = 1; i <= maxCheckStatusTries; i++) {
            	genericFunction.logFunction(logFileName,"[checkStatus] Attempt:"+ i);

                 transactionList = namedDbJdbcTemplate.query(query, params,
                    (rs, rowNum) -> new TransactionMast(
                        rs.getInt("STATUS"),
                        rs.getInt("RESP_CODE"),
                        rs.getString("RESP_DESC"),
                        rs.getString("IVR_BALANCE_INFO")
                    )
                );

                TimeUnit.SECONDS.sleep(eachTrysleep);
            }
            if (transactionList.size()>0) {
                
                 transactionDetails = transactionList.get(0);
                 genericFunction.logFunction(logFileName,"[checkStatus] Retrieved transaction details:"+transactionDetails);

                if (transactionDetails.getStatus() != 0 && 
                    transactionDetails.getStatus() != 1 && 
                    transactionDetails.getStatus() != 5) {

                	genericFunction.logFunction(logFileName,"[checkStatus] Transaction details match condition, returning responseCode:"+ 
                             transactionDetails.getResponseCode());
                    return String.valueOf(transactionDetails.getResponseCode());
                }

            }
            genericFunction.logFunction(logFileName,"[checkStatus] TRANSACTION_MAST final status="+status);
            if (status == 0) {
                query = "UPDATE TRANSACTION_MAST SET STATUS = 10 WHERE TRANSACTION_ID = :transactionId";
                genericFunction.logFunction(logFileName,"[checkStatus] Query to update TRANSACTION_MAST:"+ query);
                
                try {
                    isRecordsUpdated = namedDbJdbcTemplate.update(query, params) > 0;
                    genericFunction.logFunction(logFileName,"[checkStatus] isRecordsUpdated in TRANSACTION_MAST:"+isRecordsUpdated);
                } catch (Exception e) {
                	genericFunction.logFunction(logFileName,"[checkStatus] Exception in updating TRANSACTION_MAST:"+e.getMessage());
                    return "1014";
                }

                updateVoucherStatus(String.valueOf(voucherDetails.getSerialNumber()), 
                                    String.valueOf(voucherDetails.getBatchId()), 1);
                return "1014";
            } else {
                return "1009";
            }
        } catch (Exception e) {
        	genericFunction.logFunction(logFileName,"[checkStatus] Exception while fetching records from TRANSACTION_MAST:"+ e.getMessage());
            return "1014";
        }
    }

    
    
    
    private VoucherRedeemResponse constructResponseJson( String responseCode, JSONObject responseData) throws JSONException
    {
    		genericFunction.logFunction(logFileName,":::::::::::::::::constructResponseJson::::::::::::::::::::");
    	    reasonList = getReasonList();
    	    String responseDesc ="";
    	    genericFunction.logFunction(logFileName,"[constructResponseJson]:::::::::responseCode::::::::"+responseCode);
            try{

                    if(responseCode.equals("1000"))
                    {
                    		responseCode = "1014";
                    		responseDesc = getResponseDescription(responseCode);
                            transactionDetails.setResponseDescription(responseDesc);
                            
                            voucherRedeemResponse.setRespCode(responseCode);
	                    	voucherRedeemResponse.setRespDesc(responseDesc);
                    }
                    else if(!responseCode.equals("0000"))
                    {
                    		responseDesc = getResponseDescription(responseCode);
                    		transactionDetails.setResponseDescription(responseDesc);
                    		voucherRedeemResponse.setRespCode(responseCode);
	                    	voucherRedeemResponse.setRespDesc(responseDesc);
                    }

                    if(responseData != null)
                    {
	                    	voucherRedeemResponse.setRespCode(responseCode);
	                    	voucherRedeemResponse.setRespDesc(transactionDetails.getResponseDescription());
                    
                    }
                    else
                    {

                            if(responseCode.equals("0000") || responseCode.equals("0"))
                            {
                                 
                                    List<BalanceInfo> ivrBalInfoList = new ArrayList<>();
                                    genericFunction.logFunction(logFileName,"[constructResponseJson]:::::transactionDetails:::::"+transactionDetails);
                                    genericFunction.logFunction(logFileName,"[constructResponseJson]:::::ivrBalanceDetails:::::"+transactionDetails.getIvrBalanceInfo());
                                    if(transactionDetails.getIvrBalanceInfo() != null && !transactionDetails.getIvrBalanceInfo().equals(""))
                                    {
                                    		//<_>Prepaid Core||0.00||20250412 13:16:00
//                                          
                                            String[] ivrBalanceInfoArr = transactionDetails.getIvrBalanceInfo().split("<_>");
                                            for (String item : ivrBalanceInfoArr) {
                                                if (item != null && !item.trim().isEmpty()) { // Check for null and empty string
                                                    String[] balanceDetailsArr = item.split("\\|\\|");
                                                    if (balanceDetailsArr.length >= 3) { // Ensure we have at least 3 parts before accessing indices
                                                        BalanceInfo balanceDetails = new BalanceInfo();
                                                        balanceDetails.setName(balanceDetailsArr[0]);
                                                        balanceDetails.setNewBalance(balanceDetailsArr[1]);
                                                        balanceDetails.setNewExpiry(balanceDetailsArr[2]);
                                                        ivrBalInfoList.add(balanceDetails);
                                                    } else {
                                                        // Log error or handle cases where data is incomplete
                                                        genericFunction.logFunction(logFileName, "[constructResponseJson] Invalid balance info format: " + item);
                                                    }
                                                }
                                            }
                                    }
                                    voucherRedeemResponse.setBalInfo(ivrBalInfoList);
                                    voucherRedeemResponse.setDenomAmount(String.valueOf(denominationDetails.getAmount()));
                                  

                            }
                    }

            }catch(Exception e)
            {
            		genericFunction.logFunction(logFileName,"[constructResponseJson] Execption in constructResponseJson:: "+e.getMessage());
                    voucherRedeemResponse.setRespCode("1000");
                    voucherRedeemResponse.setRespDesc("Something went wrong, Please retry after sometime or contact customer care for more Information.");
                  
            }
            genericFunction.logFunction(logFileName,"[constructResponseJson]:::::::::voucherRedeemResponse::::::::::::"+voucherRedeemResponse);
            return voucherRedeemResponse;
    }


    public List<ReasonMast> getReasonList()
    {
    	genericFunction.logFunction(logFileName,"::::::::::::::::getReasonList:::::::::::::");
    	String query ="";
    	HashMap<String,String> params = new HashMap<String,String>();
    
    	query = "SELECT RESP_CODE,RESP_MSG FROM REASON_MAST";
    	genericFunction.logFunction(logFileName,"[getReasonList]:::::::query to fetcg details from REASON_MAST::::::"+query);
    	reasonList = namedDbJdbcTemplate.query(query, params,
    			   
    			   (rs,rowNum) -> new ReasonMast(
    
    					   rs.getInt("RESP_CODE"),
    					   rs.getString("RESP_MSG")
    			   )
    	);
    			
    	return reasonList;		
    	
    }

    
    public String getResponseDescription(String responseCode)
    {
    	String responseDescription = "";
    	for(ReasonMast r:reasonList)
    	{
    	    if(r.getResponseCode() == Integer.parseInt(responseCode))	
    	    {	
    	    	responseDescription = r.getResponseMessage();
    	    	genericFunction.logFunction(logFileName,"[getResponseDescription]::::::::responseDescription:::::::::"+responseDescription);
    	    }
    	}
    	
    	return responseDescription;
    }
}	


