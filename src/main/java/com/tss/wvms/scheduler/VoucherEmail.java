package com.tss.wvms.scheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;

import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.tss.wvms.model.DenominationMast;
import com.tss.wvms.model.EmailDet;
import com.tss.wvms.model.VoucherDet;
import com.tss.wvms.service.GenericFunctions;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class VoucherEmail { 
	
	  @Value("${VMS_HOME}")
	  private String vmsHome;

      @Value("${VMS_CFG_DIR}")
      private String vmsCfgDir;
      
      @Value("${WVMS_VOUCHER_DET_FILE_PATH}")
      private String csvFilePath;
	  
      @Value("${WVMS_VOCUHER_FILE_TMPL}")
      private String cvsTemplate;
      
      @Value("${WVMS_VOUCHER_PASSWORD_TMPL}")
      private String passwordTemplate;
      
      @Value("${WVMS_VOUCHER_SUPERIOR_TMPL}")
      private String superiorTemplate;
      
      @Value("${WVMS_VOUCHER_FROM_EMAIL_ID}")
      private String fromMailId;
      
      @Value("${WVMS_FILE_MAIL_SUBJECT}")
      private String csvMailSubject;   
      
      private String fromEmailName;
      
      @Value("${WVMS_PASSWORD_MAIL_SUBJECT}")
      private String passwordEmailSubject;
      
      @Value("${WVMS_SUPERIOR_MAIL_SUBJECT}")
      private String superiorEmailSubject;
      
      @Autowired
      GenericFunctions genericFunctions;
      
      private String logFileName ="WVMS_VoucherEmail.log";
      
	  public int countSeconds = 0;
	  
	  @Autowired
	  private NamedParameterJdbcTemplate namedDbJdbcTemplate;
	  
	  //@Scheduled(fixedDelay = 3000) // Runs every 3 seconds
	  public void generateVoucherPrint() throws Exception {
        	
	    	String query="",fileContent="",fileName="",fileHeader="",fileData="";
	    	
	    	String csvFileMailBody="",passwordMailBody="",superiorMailBody="";
	    	String cvsEmailTemplate="",passwordEmailTemplate="",superiorEmailTemplate="";
	    	
	    	String isCSVEmailSent = "",isPasswordEmailSent="",isSuperiorMailSent="";
	    	
	    	int upperLimit=99999,lowerLimit=10000,filePassword=0;
	    	boolean isEmailDetTableUpdated = false,isZIPFileCreated=false,isCSVFileCreated=false;
	    	Random random = new Random();
	    	HashMap<String,Integer> params = new HashMap<String,Integer>();
	    	
	    	//log.info(":::::::::::::::::[generateVoucherPrint]::::::::::::");
	    	genericFunctions.logFunction(logFileName,"::::::::::::::::::::::::::[generateVoucherPrint]:::::::::::::::::::::::::");
	    	
	    	
	    	//To fetch the batch details from WVMS_EMAIL_DET table where status to send the email(0:New Record)
	    	//=================================================================================================
	    	
	    	query="SELECT SERIAL_FROM,SERIAL_TO,BATCH_ID FROM WVMS_EMAIL_DET where SEND_DATE <= SYSDATE AND STATUS=0 GROUP BY SERIAL_FROM,SERIAL_TO ,BATCH_ID";
	    	
	        List<EmailDet> emailDetails = namedDbJdbcTemplate.query(query, new HashMap<>(),
	        		(rs,rowNum)-> new EmailDet(
	        				rs.getInt("SERIAL_FROM"),
	        				rs.getInt("SERIAL_TO"),
	        				rs.getInt("BATCH_ID")
	        ));
	               
	        for(EmailDet emailRecord : emailDetails)
	        {	
	        	//Update the record with status(1:In queue)
	        	//===========================================================================================
	        	
	        	isEmailDetTableUpdated = updateEmailDet(1,emailRecord.getSerialFrom(),emailRecord.getSerialTo());
	        	
	        	//To fetch EmailId's configured to print the vouchers for a batchId
	            //===========================================================================================
	        	if(isEmailDetTableUpdated)
	        	{	
			        	query = "SELECT ESEQ_ID,BATCH_ID,BATCH_NAME,SERIAL_FROM,SERIAL_TO,CSV_TO_EMAIL,CSV_CC_EMAIL,CSV_BCC_EMAIL,PASS_TO_EMAIL,PASS_CC_EMAIL,PASS_BCC_EMAIL,CREATION_DATE,SEND_DATE,STATUS,SUPERIOR_EMAIL FROM WVMS_EMAIL_DET WHERE SERIAL_FROM=:serialStart AND SERIAL_TO=:serialEnd and BATCH_ID=:batchId";
			        	
			        	params.put("serialStart",emailRecord.getSerialFrom());
			        	params.put("serialEnd", emailRecord.getSerialTo());
			        	params.put("batchId", emailRecord.getBatchId());
			        	
			        	//log.info("[generateVoucherPrint]::::::to fetch email details records from WVMS_EMAIL_DET table::::::"+query);
			        	genericFunctions.logFunction(logFileName,"[generateVoucherPrint]::::::to fetch email details records from WVMS_EMAIL_DET table::::::"+query);
			        	
			        	List<EmailDet> emailDetailsList = namedDbJdbcTemplate.query(query, params,
			        			(rs,rowNum) -> new EmailDet(
			        					    rs.getInt("SERIAL_FROM"),
			        			            rs.getInt("SERIAL_TO"),
			        			            rs.getInt("BATCH_ID"),
			        			            rs.getInt("ESEQ_ID"),
			        			            rs.getString("BATCH_NAME"),
			        			            rs.getString("CSV_TO_EMAIL"),
			        			            rs.getString("CSV_CC_EMAIL"),
			        			            rs.getString("CSV_BCC_EMAIL"),
			        			            rs.getString("PASS_TO_EMAIL"),
			        			            rs.getString("PASS_CC_EMAIL"),
			        			            rs.getString("PASS_BCC_EMAIL"),
			        			            rs.getString("CREATION_DATE"),
			        			            rs.getString("SEND_DATE"),
			        			            rs.getInt("STATUS"),
			        			            rs.getString("SUPERIOR_EMAIL")
			        					
			        					)
			        			);
			        	genericFunctions.logFunction(logFileName,"[generateVoucherPrint]:::::::emailDet::::::::::::"+emailDetailsList.get(0));		
			            //log.info("[generateVoucherPrint]:::::::emailDet::::::::::::"+emailDetailsList.get(0));
			            
			            //formatting voucher details
		                //while(emailDetailsList.size()==1)
			            //{	
			            	EmailDet emailDet=emailDetailsList.get(0);
			            	
			            	//formatting csv file name
			            	fileName = "CWS_"+emailDet.getBatchId()+"_"+emailDet.getSerialFrom()+"-"+emailDet.getSerialTo();
			            	
			            	if(!emailDet.getCsvToEmail().equals(""))
			            	{	
			            		//To fetch the voucher numbers configured for the serial number from VOUCHER_DET table
			            		//================================================================================================
			            		
			            		query="SELECT voucherDecrypt(VOUCHER_NUMBER),SERIAL_NUMBER FROM VOUCHER_DET WHERE SERIAL_NUMBER >=:serialFrom and SERIAL_NUMBER<=:serialEnd and BATCH_ID=:batchId";
			            	    params.put("serialFrom",emailDet.getSerialFrom());
			            	    params.put("serialEnd", emailDet.getSerialTo());
			            	    params.put("batchId",emailDet.getBatchId());
			            	    
			            	    //log.info("[generateVoucherPrint]::::::to fetch voucher details from VOUCHER_DET table::::::"+query);
			            	    genericFunctions.logFunction(logFileName,"[generateVoucherPrint]::::::to fetch voucher details from VOUCHER_DET table::::::"+query);	
			            	    
			            	    List<VoucherDet> voucherDetails = namedDbJdbcTemplate.query(query, params,
			            	    		
			            	    		(rs,rowNum) -> new VoucherDet(
			            	    				rs.getLong("voucherDecrypt(VOUCHER_NUMBER)"),
			            	    				rs.getInt("SERIAL_NUMBER")
			            	    				
			            	    				)
			            	    );
			            	    
			            	    for(VoucherDet voucher:voucherDetails)
			            	    {	
			            	    	//formatting cvs file content
			            	    	//format 0 <7 digit serialNumber> 0000 <12 digit Voucher number>
			            	    	fileContent+="'0"+String.format("%07d",voucher.getSerialNumber())+"0000"+String.format("%012d",voucher.getVoucherNumber())+"'\n";
			            	    }
			            	    
			            	
			            	}
			            	else
			            	{
			            		//log.error("[generateVoucherPrint]::::::::::TO MAIL ID IS MANDATORY:::::::::::::::");
			            		genericFunctions.logFunction(logFileName,"`[generateVoucherPrint]::::::::::TO MAIL ID IS MANDATORY:::::::::::::::`"+query);	
			            	}
			            	
			            //}
			            
			            //formatting csv file header
			            query="SELECT a.AMOUNT, b.VOUCHER_QUANTITY FROM DENOMINATION_MAST a, BATCH_MAST b WHERE a.DENOMINATION_ID=b.DENOMINATION_ID and b.BATCH_ID=:batchId and b.SERIAL_START>=:serialFrom and b.SERIAL_END<=:serialEnd";
			            params.put("batchId", emailDetailsList.get(0).getBatchId());
			            params.put("serialFrom",emailDetailsList.get(0).getSerialFrom());
		        	    params.put("serialEnd", emailDetailsList.get(0).getSerialTo());
		        	    
		        	    //log.info("[generateVoucherPrint]::::::to fetch denomaination and voucher quantity from DENOMINATION_MAST and BATCH_MAST table::::::"+query);
		        	    genericFunctions.logFunction(logFileName,"[generateVoucherPrint]::::::to fetch denomaination and voucher quantity from DENOMINATION_MAST and BATCH_MAST table::::::"+query);	
		        	    
			        	DenominationMast denomination = namedDbJdbcTemplate.query(query, params,
			        			(rs,rowNum)->new DenominationMast(
			        					rs.getInt("AMOUNT"),
			        					rs.getInt("VOUCHER_QUANTITY")
			        					
			        					)
			        			).get(0);
			        	
			        	fileHeader = "CWS"+String.format("%04d",emailDetailsList.get(0).getBatchId())+String.format("%06d",denomination.getVoucherQuantity())+String.format("%04d",denomination.getAmount());
			        
			        	//to format csv file data
			        	fileData = fileHeader+"\n"+fileContent;
			        	
			        	//cvs file password
			        	filePassword=random.nextInt(upperLimit-lowerLimit)+lowerLimit;
			        	
			        	//Update the record with generated password
			        	//===========================================================================================
			        	
			        	query="UPDATE WVMS_EMAIL_DET SET PASSWORD =:filePassword WHERE SERIAL_FROM=:serialFrom and SERIAL_TO=:serialEnd";
			        	params.put("filePassword",filePassword);
			        	params.put("serialFrom", emailRecord.getSerialFrom());
			        	params.put("serialEnd",emailRecord.getSerialTo());
			        	
			        	try {
			        		isEmailDetTableUpdated = namedDbJdbcTemplate.update(query, params) > 0 ;
			        	    
			        		genericFunctions.logFunction(logFileName,"[generateVoucherPrint]:::::isEmailDetTableUpdated record updated in WVMS_EMAIL_DET Table:::::::"+isEmailDetTableUpdated);  
				    	
			        	}
				    	catch(Exception e)
				    	{
				    		genericFunctions.logFunction(logFileName,"[generateVoucherPrint]:::::Error while updation WVMS_EMAIL_DET:::::::"+e.getMessage());
				    		isEmailDetTableUpdated=false;
				    	}
			        	
			        	isCSVFileCreated = createFile(vmsHome+vmsCfgDir+csvFilePath+fileName+".csv", fileData);
			        	isZIPFileCreated = createZipFile(vmsHome+vmsCfgDir+csvFilePath+fileName+".csv",vmsHome+vmsCfgDir+csvFilePath+fileName+".zip",String.valueOf(filePassword));
			        	
			        	
			        	//to fetch cvs,password and superior mail body message from WVMS_MESSAGE_MAST_1 where message_id(1002-cvsMailBody,1001-passwordMailBody,1003-superiorMailBody)
			        	//===========================================================================================
			        	if(isCSVFileCreated && isZIPFileCreated)
			        	{	
			        		
			        	
				        	int[] messageIdArray = {1002,1001,1003};
				        	
				        	for(int messageId : messageIdArray)
				        	{	
				        		query = "SELECT MESSAGE FROM WVMS_MESSAGE_MAST_1 WHERE MESSAGE_ID=:messageId";
				        		params.put("messageId",messageId);
				        		
				        		if(messageId==1002)
				        		{	
				        			csvFileMailBody = namedDbJdbcTemplate.queryForObject(query, params, String.class);
				        		}
				        		else if(messageId==1001)
				        		{	
				        			passwordMailBody = namedDbJdbcTemplate.queryForObject(query, params, String.class);
				        		}
				        		else
				        		{	
				        			superiorMailBody = namedDbJdbcTemplate.queryForObject(query, params, String.class);
				        		}
				        		
				        	}
				        	genericFunctions.logFunction(logFileName,"[generateVoucherPrint]:::::::::::::csvFileMailBody:::::::::::"+csvFileMailBody);
				        	genericFunctions.logFunction(logFileName,"[generateVoucherPrint]:::::::::::::passwordMailBody:::::::::::"+passwordMailBody);
				        	genericFunctions.logFunction(logFileName,"[generateVoucherPrint]:::::::::::::superiorMailBody:::::::::::"+superiorMailBody);
				        	
				        	//reading the emailFileTmeplate into corresponding variables
				        	
				        	cvsEmailTemplate = readFileContent(cvsTemplate,vmsHome,vmsCfgDir);
				        	passwordEmailTemplate = readFileContent(passwordTemplate,vmsHome,vmsCfgDir);
				        	superiorEmailTemplate = readFileContent(superiorTemplate,vmsHome,vmsCfgDir);
				        	
				        	genericFunctions.logFunction(logFileName,"[generateVoucherPrint]:::::::::::::cvsEmailTemplate:::::::::::"+cvsEmailTemplate);
				        	genericFunctions.logFunction(logFileName,"[generateVoucherPrint]:::::::::::::passwordEmailTemplate:::::::::::"+passwordEmailTemplate);
				        	genericFunctions.logFunction(logFileName,"[generateVoucherPrint]:::::::::::::superiorEmailTemplate:::::::::::"+superiorEmailTemplate);
			
			                //replacing the MAIL_BODY in the emailTemplates to the MAILBODY retrieved from WVMS_MESSAGE_MAST_1 table
				        	
				        	
				        	cvsEmailTemplate = cvsEmailTemplate.replaceAll("__MAILBODY__", csvFileMailBody);
				        	passwordEmailTemplate = passwordEmailTemplate.replaceAll("__MAILBODY__", passwordMailBody);
				        	superiorEmailTemplate = superiorEmailTemplate.replaceAll("__MAILBODY__", superiorMailBody);
				        	
				        	//replacing the PLACEHOLDERS present in the MAILBODY retrieved from WVMS_MESSAGE_MAST_1 table
				        	
				        	genericFunctions.logFunction(logFileName,"[generateVoucherPrint]::::::::::emailRecord::::::::::::::"+emailRecord);
			                 
				        	cvsEmailTemplate = cvsEmailTemplate.replaceAll("__BATCHNAME__", emailDet.getBatchName());
				        	cvsEmailTemplate = cvsEmailTemplate.replaceAll("__FILENAME__", fileName+".zip");
				        	cvsEmailTemplate = cvsEmailTemplate.replaceAll("__SERIALEND__",String.valueOf(emailRecord.getSerialTo()));
				        	cvsEmailTemplate = cvsEmailTemplate.replaceAll("__SERIALSTART__", String.valueOf(emailRecord.getSerialFrom()));
				        	
				        	//log.info("[generateVoucherPrint]:::::::::::::cvsEmailTemplate after replaceAll:::::::::::"+cvsEmailTemplate);
				      
				        	genericFunctions.logFunction(logFileName,"[generateVoucherPrint]::::::filePassword:::::"+filePassword+"::::::batchName:::::"+emailRecord.getBatchName()+":::::::fileName::::::"+fileName+".zip"+"::::::serialEnd::::::"+emailRecord.getSerialTo()+"::::::::::serialStart::::::::"+emailRecord.getSerialFrom());
				        	passwordEmailTemplate = passwordEmailTemplate.replaceAll("__PASSWORD__",String.valueOf(filePassword));
				        	passwordEmailTemplate = passwordEmailTemplate.replaceAll("__BATCHNAME__", emailDet.getBatchName());
				        	passwordEmailTemplate = passwordEmailTemplate.replaceAll("__FILENAME__", fileName+".zip");
				        	passwordEmailTemplate = passwordEmailTemplate.replaceAll("__SERIALEND__",String.valueOf(emailRecord.getSerialTo()));
				        	passwordEmailTemplate = passwordEmailTemplate.replaceAll("__SERIALSTART__", String.valueOf(emailRecord.getSerialFrom()));
				        	
				        	//log.info("[generateVoucherPrint]:::::::::::::passwordEmailTemplate after replaceAll:::::::::::"+passwordEmailTemplate);
				        	
				        	superiorEmailTemplate = superiorEmailTemplate.replaceAll("__PASSWORD__", String.valueOf(filePassword));
				        	superiorEmailTemplate = superiorEmailTemplate.replaceAll("__BATCHNAME__", emailDet.getBatchName());
				        	superiorEmailTemplate = superiorEmailTemplate.replaceAll("__FILENAME__",fileName+".zip");
				        	superiorEmailTemplate = superiorEmailTemplate.replaceAll("__SERIALEND__",String.valueOf(emailRecord.getSerialTo()));
				        	superiorEmailTemplate = superiorEmailTemplate.replaceAll("__SERIALSTART__", String.valueOf(emailRecord.getSerialFrom()));
				        	superiorEmailTemplate = superiorEmailTemplate.replaceAll("__FILEMAIL__",emailDet.getCsvToEmail());
				        	superiorEmailTemplate = superiorEmailTemplate.replaceAll("__PASSEMAIL__",emailDet.getPassToEmail());
			
				        	//log.info("[generateVoucherPrint]:::::::::::::superiorEmailTemplate after replaceAll:::::::::::"+superiorEmailTemplate);
				          	
				        	isCSVEmailSent = genericFunctions.sendMail(emailDet.getCsvToEmail(),fromMailId,cvsEmailTemplate,csvMailSubject,vmsHome+vmsCfgDir+csvFilePath+fileName+".zip",emailDet.getCsvCCEmail(),emailDet.getCsvBCCEmail(),fromEmailName);
				        	if(!isCSVEmailSent.equals("1"))
				        	{	
				        		//to update WVMS_EMAIL_DET status(5:Failed To Send)
				        		isEmailDetTableUpdated = updateEmailDet(5,emailRecord.getSerialFrom(),emailRecord.getSerialTo());
				        		genericFunctions.logFunction(logFileName,"[generateVoucherPrint]:::::::::::::Unable to send CSV mail::::::::::::::");
				        	}
				        	
				        	isPasswordEmailSent = genericFunctions.sendMail(emailDet.getPassToEmail(),fromMailId,passwordEmailTemplate,passwordEmailSubject,"",emailDet.getPassCCEmail(),emailDet.getPassBCCEmail(),fromEmailName);
				        	if(!isPasswordEmailSent.equals("1"))
				        	{	
				        		isEmailDetTableUpdated = updateEmailDet(5,emailRecord.getSerialFrom(),emailRecord.getSerialTo());
				        		genericFunctions.logFunction(logFileName,"[generateVoucherPrint]:::::::::::::Unable to send Password mail::::::::::::::");
				        	}	 
				        	
				        	if(!emailDet.getSuperiorEmail().equals(""))
				        	{	
				        		isSuperiorMailSent = genericFunctions.sendMail(emailDet.getSuperiorEmail(),fromMailId,superiorEmailTemplate,superiorEmailSubject,"",null,null,fromEmailName);
				      
				        	}
				        	if(!isSuperiorMailSent.equals("1"))
				        	{	
				        		isEmailDetTableUpdated = updateEmailDet(5,emailRecord.getSerialFrom(),emailRecord.getSerialTo());
				        		genericFunctions.logFunction(logFileName,"[generateVoucherPrint]:::::::::::::Unable to send Superior mail::::::::::::::");
				        	}
				        	
				        	//update WVMS_EMAIL_DET status (6:Email Sent)
				        	isEmailDetTableUpdated = updateEmailDet(6,emailRecord.getSerialFrom(),emailRecord.getSerialTo());
			        	}
	        	
	        	
	        	}
	        	
	        }
	        try {
		        Thread.sleep(1000); // Pause execution for 1 second
		    } catch (InterruptedException e) {
		        e.printStackTrace();
		    }
	        
	   }
	    
	    public boolean createZipFile(String csvFilePath, String zipFilePath, String password) {
	        try {
	        	
	        		genericFunctions.logFunction(logFileName,"[createZipFile]:::::::::::fileName::::::::::::::"+csvFilePath);
	        		genericFunctions.logFunction(logFileName,"[createZipFile]:::::::::::zipFileName::::::::::::::"+zipFilePath);
	        		genericFunctions.logFunction(logFileName,"[createZipFile]:::::::::::password::::::::::::::"+password);
	        	
		            // Set ZIP parameters
		            ZipParameters zipParameters = new ZipParameters();
		            zipParameters.setCompressionMethod(CompressionMethod.DEFLATE); // Use DEFLATE compression
		            zipParameters.setEncryptFiles(true);
		            zipParameters.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD); // Use standard ZIP encryption
	
		            // Create ZIP file with password protection
		            ZipFile zipFile = new ZipFile(zipFilePath, password.toCharArray());
		            zipFile.addFiles(Collections.singletonList(new File(csvFilePath)), zipParameters);
	
		            genericFunctions.logFunction(logFileName,"[createZipFile]:::::::::::Successfully created the zip file::::::::::::::");
		            genericFunctions.logFunction(logFileName,"[createZipFile]:::::::::::ZIP file created successfully:::::::::::::::::::: " + zipFilePath);
		            return true;
	        } catch (Exception e) {
	        	genericFunctions.logFunction(logFileName,"[createZipFile]:::::::::::Exception found while creating the zip. Exception - " + e.getMessage());
	            System.err.println("Error creating ZIP file: " + e.getMessage());
	            e.printStackTrace();
	            return false;
	        }
	    }

	    
	    private boolean createFile(String filePath, String content) throws IOException {
	    	try
	        {
	    		File file = new File(filePath);
		        file.getParentFile().mkdirs();  // Ensure directories are created
		        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
		            writer.write(content);
		        }
		        catch (Exception e)
		    	{
		        	genericFunctions.logFunction(logFileName,"[generateVoucherPrint]:::::::Error in creating csv file:::::::::::::::"+e.getMessage());
		    		return false;
		    	}
		        
	        }
	    	catch (Exception e)
	    	{
	    		genericFunctions.logFunction(logFileName,"[generateVoucherPrint]:::::::::::Error in creating csv file:::::::::::::: "+e.getMessage());
	    		return false;
	    	}
	    	genericFunctions.logFunction(logFileName,"[generateVoucherPrint]::::Successfully created the cvs file::::::::");
    		return true;
	    	
	    }
	

	    
	    public boolean updateEmailDet(int status,int serialStart ,int serialEnd)
	    {
	    	boolean isRecordsUpdated = false;
	    	String query = "";
	    	HashMap<String,Integer> params = new HashMap<String,Integer>();
	    	
	    	query = "UPDATE WVMS_EMAIL_DET SET STATUS=:status where SERIAL_FROM=:serialFrom and SERIAL_TO=:serialEnd";
	    	genericFunctions.logFunction(logFileName,"[generateVoucherPrint]:::::query to update WVMS_EMAIL_DET:::::::"+query);
	    	params.put("status",status);
	    	params.put("serialFrom",serialStart);
	    	params.put("serialEnd",serialEnd);
	    	
	    	try {
	    		  isRecordsUpdated = namedDbJdbcTemplate.update(query, params) > 0 ;
	    		  return isRecordsUpdated;
	    	}
	    	catch(Exception e)
	    	{
	    		genericFunctions.logFunction(logFileName,"[generateVoucherPrint]:::::Error while updation WVMS_EMAIL_DET:::::::"+e.getMessage());
	    		return false;
	    	}
	    	
	    			
	    }
	    
	    
	    public static String readFileContent(String fileName,String vmsHome,String vmsCfgDir) {
	     
	        // Construct file path
	        Path filePath = Path.of(vmsHome, vmsCfgDir, fileName);

	        try {
	            // Read and return file content
	            return Files.readString(filePath);
	        } catch (IOException e) {
	            throw new RuntimeException("Error reading file: " + filePath, e);
	        }
	    }

	    
	    
	   


}