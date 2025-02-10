package com.tss.wvms.service;

import java.util.ArrayList;


import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.io.*;
import java.nio.file.*;
import java.util.*;

import org.springframework.stereotype.Service;

import com.tss.wvms.contoller.VoucherController;
import com.tss.wvms.model.BatchMast;
import com.tss.wvms.model.DenominationMast;
import com.tss.wvms.model.RechargeMast;
import com.tss.wvms.model.VoucherDet;
import com.tss.wvms.requestResponse.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class VoucherGenerationService {
	
   
	  @Value("${DATE_HASH}")
	  private String dateHash;
	  
	  @Value("${VMS_HOME}")
	  private String vmsHome;

      @Value("${VMS_CFG_DIR}")
      private String vmsCfgDir;
      
      @Value("${PECLOAD_DATAFILEPATH}")
      private String pecFilePath;
  
	  @Autowired
	  private NamedParameterJdbcTemplate namedDbJdbcTemplate;

	  
	  public Response generateVouchers(int batchId) {
        	
	            int result = 1,batchMastStaus = 4,voucherCountAlreadyExist=0,dateToAdd=0,slabValidityInHours=0,slabAmountI=0,flag=0,serialNumber=0;
	            Long voucherNumber = (long) 0;
	            String query = "",voucherExpiryDate="",voucherLoader="",pecLoader="";
	            boolean isRecordsUpdated = false,isRecordInserted=false,isPECRecordsInserted=false;
	            
	            Random random = new Random();
	            
	            HashMap<String, Object> params = new HashMap<>();
	            HashMap<String, Integer> dateHashMap = new HashMap<String, Integer>();
	            HashMap<Long, Integer> voucherNumberHashMap = new HashMap<Long, Integer>();
	            
	            //To fetch batch details from BATCH_MAST Table where STATUS in (2,5) [2:Generated Request,5:Generation]
	            
	            query = "SELECT BATCH_ID, DENOMINATION_ID, CREATOR_ID, CREATION_DATE,VOUCHER_QUANTITY, BONUS_ID, SERIAL_START, SERIAL_END, BATCH_NAME, RATE_ID FROM batch_mast WHERE BATCH_ID = :batchId AND STATUS IN (2, 5)";
	            log.info(":::::::::::batchDetails query:::::::"+query);
	            params.put("batchId", batchId);
	              

	            List<BatchMast> batchDetails = namedDbJdbcTemplate.query(query, params,

	                  (rs, rowNum) -> new BatchMast(rs.getInt("BATCH_ID"),
		                rs.getInt("DENOMINATION_ID"),
		                rs.getInt("CREATOR_ID"),
		                rs.getString("CREATION_DATE"),
		                rs.getInt("VOUCHER_QUANTITY"),
		                rs.getInt("BONUS_ID"),
		                rs.getString("SERIAL_START"),
		                rs.getString("SERIAL_END"),
		                rs.getString("BATCH_NAME"),
		                rs.getInt("RATE_ID")

	             ));
	            if(batchDetails.size() == 0)
	            {	
	            	  log.info("Voucher generation Failed for batchId={}",batchId);
				      return new Response(1, "Voucher generation Failed for batchId="+batchId + " Batch does not exist");
	            }
	            
	            
	            serialNumber = Integer.parseInt(batchDetails.get(0).getSerialStart());
		        System.out.println("::::batchDetails:::::::::::"+batchDetails);  
		        
		        //=====================================================================================================
		        //To fetch the denomination details by denominationId from DENOMINATION_MAST Table
		        
		        query = "SELECT SLAB_ID,AMOUNT,ACCESS_TYPE,DENO_DESC,CARD_TYPE,DENOMINATION_VALIDITY,VALIDITY_TYPE FROM DENOMINATION_MAST where DENOMINATION_ID=:denominationId";
		        log.info(":::::::::::denominationDetails query:::::::"+query);
		        params.put("denominationId", batchDetails.get(0).getDenomainationId());
		        
		        List<DenominationMast> denominationDetails = namedDbJdbcTemplate.query(query, params,
		        		(rs,rowNum)-> new DenominationMast(rs.getInt("SLAB_ID"),
		        				rs.getInt("AMOUNT"),
		        				rs.getInt("ACCESS_TYPE"),
		        				rs.getString("DENO_DESC"),
		        				rs.getInt("CARD_TYPE"),
		        				rs.getInt("DENOMINATION_VALIDITY"),
		        				rs.getInt("VALIDITY_TYPE")
		        ));
		        denominationDetails.get(0).setDenominationId(batchDetails.get(0).getDenomainationId());
		        log.info(":::::::::::denominationDetails::::::::::"+denominationDetails);
		        
		        
		        //======================================================================================================
		        //To update BATCH_MAST table STATUS=3 to indicate Generated Intermidiate (Request is processing) 
		        
		        query = "UPDATE BATCH_MAST SET STATUS=3 WHERE BATCH_ID=:batchId and SERIAL_START=:serialStart and SERIAL_END=:serialEnd";
		        log.info(":::::::::::update BATCH_MAST query:::::::"+query);
		        
		        params.put("batchId", batchId);
		        params.put("serialStart",batchDetails.get(0).getSerialStart());
		        params.put("serialEnd",batchDetails.get(0).getSerialEnd());
		        
		        isRecordsUpdated = namedDbJdbcTemplate.update(query, params) > 0 ;
		        
		        log.info(":::::::::: isRecordsUpdated in BATCH_MAST table:::::::"+isRecordsUpdated);
		        
		        
		        //=====================================================================================================
		        //Generation of voucher number for Prepaid and Calling cards
		        
		        if(denominationDetails.get(0).getCardType() == 1) //Calling Card
		        {	
		        	
		        	System.out.println(":::::::::::Calling Card Vouchers::::::::::");
		        	
		        	//To check if vouchers already exist for the batchId in VOUCHER_DET Table
		        	
		        	query = "SELECT COUNT(*) FROM VOUCHER_DET WHERE BATCH_ID = :batchId AND STATUS NOT IN (5)";
		        	params.put("batchId",batchId);
		        	
		        	voucherCountAlreadyExist = namedDbJdbcTemplate.queryForObject(query, params, Integer.class);
		        	
		        	//If no voucher exist for the batchId
		        	if(voucherCountAlreadyExist<1)
		        	{	
		        		//To fetch talktime related details from RECHARGE_MAST
		        		
		        		query = "SELECT SLAB_AMOUNT,SLAB_VALIDITY FROM RECHARGE_MAST WHERE SLAB_ID =:slabId";
		        		params.put("slabId", denominationDetails.get(0).getSlabId());
		        		
		        		List<RechargeMast> slabDetails = namedDbJdbcTemplate.query(query, params,
		        				(rs,rowNum)->new RechargeMast(
		        		        rs.getInt("SLAB_AMOUNT"),
		        		        rs.getInt("SLAB_VALIDITY")
		        		));
		        		log.info(":::::::::::slabDetails::::::::::"+slabDetails);
		        		
		        		
		        		//Preparing values to be inserted into PEC(PREPAID) table
		    
		        		String[] temp = dateHash.split(",");
		        		
		        		for (String t : temp) {
		        			dateHashMap.put(t.split(":")[0],Integer.parseInt(t.split(":")[1]));
		                }
		        		
		        		dateToAdd = denominationDetails.get(0).getDenominationValidity() * dateHashMap.get(String.valueOf(denominationDetails.get(0).getValidationType()));
		                
		        		query = "SELECT TO_CHAR(SYSDATE + :dateToAdd, 'dd/mm/yyyy') AS calculatedDate FROM dual";
		        		params.put("dateToAdd",dateToAdd);
		        		
		        		voucherExpiryDate = namedDbJdbcTemplate.queryForObject(query, params, String.class);
		        		
		        		slabValidityInHours = slabDetails.get(0).getSlabValidity() *24;
		        		slabAmountI = slabDetails.get(0).getSlabAmount() * 10000;
		        		
		        		log.info(":::::::::::dateToAdd::::::::::::::::::"+dateToAdd);
		        		log.info(":::::::::::voucherExpiryDate::::::::::"+voucherExpiryDate);
		        		log.info(":::::::::::slabValidityInHours::::::::"+slabValidityInHours);
		        		log.info(":::::::::::slabAmountI::::::::::::::::"+slabAmountI);
		        		
		        		
		        		for(int i = 0; i < batchDetails.get(0).getVoucherQuantity(); i++)
		                {
                                    
                                	  
                                	  while (true) {
                                	        // Query to get a voucher number
                                		  	try {
                                			    query = "SELECT VMS_getVoucherNumber() FROM DUAL";
                                			    voucherNumber = namedDbJdbcTemplate.queryForObject(query, new HashMap<>(), Long.class);
                                			    System.out.println(":::::::voucherNumber:::::::::" + voucherNumber);
                                			} catch (Exception e) {
                                			    System.out.println("Error executing query: " + e.getMessage());
                                			}
                                	        // If voucher number is not already in the map, break the loop
                                	        if (!voucherNumberHashMap.containsKey(voucherNumber)) {
                                	            break;
                                	        }
                                	  }
                                  	  
                                	  //Inserting the voucher number,batchId ,serial number and status =-1(Calling card) into VOUCHER_DET table
                                	  
                                	  voucherLoader +=","+batchId+","+voucherNumber+","+serialNumber+",-1\n";
                                	  voucherNumberHashMap.put(voucherNumber,1);
                          			  serialNumber++;
                                	  
                      }
		        	  isRecordInserted = loadVoucher(batchId,voucherLoader);
            		    
              		  if(isRecordInserted)
              		  {	 
              			  
              			  int randomNumber = random.nextInt(100000000);
              			  pecLoader+= voucherNumber +","+randomNumber+","+batchDetails.get(0).getRateId()+","+
                      	                         denominationDetails.get(0).getAccessType()+","+slabDetails.get(0).getSlabAmount()+","+
                      			                 slabAmountI+","+voucherExpiryDate+",1,2,"+slabValidityInHours+","+batchId+","+serialNumber;
                      	  
                      	  //insert into PREPAID (CARDNO,SERIALNO,RATETABLECODE,SYNCID,CARDVALUE,CARDUNITS,EXPIRNDATE,
                      	  //DISABLED,ACCOUNTTYPE,HOURS_AFTER_FUD_TO_EXPIRE,BATCHNO,SEQUENCE_NUM) 
                      	  //values (?, ?, ?, ?, ?,?,STR_TO_DATE(?,'%d/%m/%Y'),?, ?,?,?,?)
              			  isPECRecordsInserted = loadVoucherWithPECDetails(batchId,pecLoader,String.valueOf(batchDetails.get(0).getVoucherQuantity()),"admin","9999");
                      	    
                      	 
              		  }
              		  
              		  log.info("::::::::voucherNumberHashMap::::::::::"+voucherNumberHashMap.toString());
              		  log.info("::::::::serialNumber::::::::::"+serialNumber+":::::VoucherQuantity:::::"+batchDetails.get(0).getVoucherQuantity());

		        	
		            }
		        	
		        } 
		        else
		        {	
		        	//Prepaid Card
		        	log.info(":::::::::::Prepaid Card Vouchers::::::::::");
		        	for(int i = 0; i < batchDetails.get(0).getVoucherQuantity(); i++)
	                {
                                
                            	  
                            	  while (true) {
                            	        // Query to get a voucher number
                            		  	try {
                            			    query = "SELECT VMS_getVoucherNumber() FROM DUAL";
                            			    voucherNumber = namedDbJdbcTemplate.queryForObject(query, new HashMap<>(), Long.class);
                            			    log.info(":::::::voucherNumber:::::::::" + voucherNumber);
                            			} catch (Exception e) {
                            				log.info("Error executing query: " + e.getMessage());
                            			}
                            	        // If voucher number is not already in the map, break the loop
                            	        if (!voucherNumberHashMap.containsKey(voucherNumber)) {
                            	            break;
                            	        }
                            	  }
                            	  
                            	//Inserting the voucher number,batchId ,serial number and status =0(Prepaid card) into VOUCHER_DET table
                            	  
                              	  
                            	  voucherLoader +=","+batchId+","+voucherNumber+","+serialNumber+",0\n";
                            	  voucherNumberHashMap.put(voucherNumber,1);
        	           			  serialNumber++;
                            	  
                      }
		        	  isRecordInserted = loadVoucher(batchId,voucherLoader);
         		    
	           	
	           		  
	           		  log.info("::::::::voucherNumberHashMap::::::::::"+voucherNumberHashMap.toString());
	           		  log.info("::::::::serialNumber::::::::::"+serialNumber+":::::VoucherQuantity:::::"+batchDetails.get(0).getVoucherQuantity());

		        	
		        }
		        
		        query = "UPDATE BATCH_MAST SET STATUS=:status WHERE BATCH_ID=:batchId";
		        params.put("status", batchMastStaus);
		        params.put("batchId",batchId);
		        
		        isRecordsUpdated = namedDbJdbcTemplate.update(query, params) > 0 ;
		       
		        if(isRecordsUpdated && batchMastStaus == 4 && voucherNumberHashMap.size() == batchDetails.get(0).getVoucherQuantity())
		        {
		        	return new Response(0, "Voucher generation Successfully");
		        }else
			    {
			       log.info("Voucher generation Failed for batchId={}",batchId);
			       return new Response(1, "Voucher generation Failed for batchId="+batchId);
			    }
		     
	        
	   }
	  
	  
	  public boolean loadVoucher(int batchId, String voucherDetloader) {
		    log.info("`:::::::::::::::::loadVoucher:::::::::::::::::::::`");
	        try {
	            // Define file paths based on environment variables
	            String ctlFileContent = vmsHome + "/" + vmsCfgDir + "/controlFileContent.tmpl";
	            StringBuilder contentCtl = new StringBuilder();

	            // Read the template content
	            try (BufferedReader reader = Files.newBufferedReader(Paths.get(ctlFileContent))) {
	                String line;
	                while ((line = reader.readLine()) != null) {
	                    contentCtl.append(line).append(System.lineSeparator());
	                }
	            }

	            // Replace placeholders with actual values
	            String vouchFile = "voucher_" + batchId + ".dat";
	            String vouchFile1 = vmsHome + "/etc/scripts/voucherLdr/" + vouchFile;
	            String updatedContentCtl = contentCtl.toString().replace("__FILE__", vouchFile1);

	            // Write the modified content to the .ctl file
	            String ctlFile = vmsHome + "/etc/scripts/voucherLdr/WVMS_loadVoucher_" + batchId + ".ctl";
	            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(ctlFile))) {
	                writer.write(updatedContentCtl);
	            }

	            // Write voucher details to a file
	            String voucherFilePath = vmsHome + "/etc/scripts/voucherLdr/" + vouchFile;
	            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(voucherFilePath))) {
	                writer.write(voucherDetloader);
	            }

	            // Execute the shell script
	            String shFilePath = vmsHome + "/etc/scripts/WVMS_loadVoucherGenerated.sh";
	            ProcessBuilder pb = new ProcessBuilder("sh", shFilePath, ctlFile);
	            pb.environment().put("VMS_HOME", vmsHome); // Add environment variable if needed

	            // Log command execution (can be enhanced with a logging framework)
	            System.out.println("Executing: " + "source " + vmsHome + "/etc/scripts/VMS_env.env; sh " + shFilePath + " " + ctlFile);
	            log.info("Executing: " + "source " + vmsHome + "/etc/scripts/VMS_env.env; sh " + shFilePath + " " + ctlFile);

	            Process process = pb.start();
	            int exitCode = process.waitFor();

	            // Log the process exit code
	            System.out.println("Command exited with code: " + exitCode);
	            log.info("Command exited with code: " + exitCode);

	            return true;
	        } catch (IOException | InterruptedException e) {
	            e.printStackTrace();
	            return false;
	        }
	    }
	  
	  
	  
	  public boolean loadVoucherWithPECDetails(int batchId, String loaderText, String voucherQuantity, String userName, String creatorID) {
	        try {
	            // Define file name for PEC load
	            String file = "voucherPECLoad_" + batchId + ".dat";
	            String filePath = pecFilePath + "/" + file;

	            // Write loader text to the file
	            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath))) {
	                writer.write(loaderText);
	            }

	            // Log file generation (can be replaced with an actual logging mechanism)
	            System.out.println("AFTER FILE GENERATION");

	            // Prepare the Java command to run
	            String javaCommand = String.format("cd %s/tssgui/WEB-INF/classes; java com.witl.tss.WVMS_PECDetailsLoader %s 1 %s %s %s %s > /dev/null &",
	                    vmsHome, file, batchId, voucherQuantity, userName, creatorID);

	            // Log the command to be executed
	            System.out.println("COMMAND -- " + javaCommand);

	            // Run the Java command via ProcessBuilder
	            ProcessBuilder pb = new ProcessBuilder("bash", "-c", "source " + vmsHome + "/etc/scripts/VMS_env.env; " + javaCommand);
	            pb.environment().put("VMS_HOME", vmsHome); // Add VMS_HOME if needed

	            Process process = pb.start();
	            int exitCode = process.waitFor();

	            // Log the process exit code (can be replaced with an actual logging mechanism)
	            System.out.println("Java command exited with code: " + exitCode);
	            log.info("Java command exited with code: " + exitCode);

	            return true;
	        } catch (IOException | InterruptedException e) {
	            e.printStackTrace();
	            return false;
	        }
	    }

}