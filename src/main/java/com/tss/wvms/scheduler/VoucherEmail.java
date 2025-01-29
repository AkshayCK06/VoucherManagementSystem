package com.tss.wvms.scheduler;

import java.io.BufferedWriter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.security.SecureRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.tss.wvms.model.DenominationMast;
import com.tss.wvms.model.EmailDet;
import com.tss.wvms.model.VoucherDet;

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
	  
	  public int countSeconds = 0;
	  
	  @Autowired
	  private NamedParameterJdbcTemplate namedDbJdbcTemplate;
	  
	  @Scheduled(fixedDelay = 10000) // Runs every 10 seconds
	  public void generateVoucherPrint() throws Exception {
        	
	    	String query="",fileContent="",fileName="",fileHeader="",fileData="";
	    	String csvFileMailBody="",passwordMailBody="",superiorMailBody="";
	    	String cvsTemplate="",passwordTemplate="",superiorTemplate="";
	    	String cvsEmailTemplate="",passwordEmailTemplate="",superiorEmailTemplate="";
	    	int upperLimit=99999,lowerLimit=10000,filePassword=0;
	    	boolean isEmailDetTableUpdated = false,isZIPFileCreated=false;
	    	Random random = new Random();
	    	HashMap<String,Integer> params = new HashMap<String,Integer>();
	    	
	    	log.info(":::::::::::::::::[generateVoucherPrint]::::::::::::");
	    	
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
	        	
	        	log.info("::::::to fetch email details records from WVMS_EMAIL_DET table::::::"+query);
	        	
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
	        			
	            log.info(":::::::emailDet::::::::::::"+emailDetailsList.get(0));
	            
	            //formatting voucher details
//	            while(emailDetailsList.size()==1)
//	            {	
	            	EmailDet emailDet=emailDetailsList.get(0);
	            	
	            	//formatting csv file name
	            	fileName = "CWS_."+emailDet.getBatchId()+"._."+emailDet.getSerialFrom()+"-"+emailDet.getSerialTo();
	            	
	            	if(!emailDet.getCsvToEmail().equals(""))
	            	{	
	            		//To fetch the voucher numbers configured for the serial number from VOUCHER_DET table
	            		//================================================================================================
	            		
	            		query="SELECT voucherDecrypt(VOUCHER_NUMBER),SERIAL_NUMBER FROM VOUCHER_DET WHERE SERIAL_NUMBER >=:serialFrom and SERIAL_NUMBER<=:serialEnd and BATCH_ID=:batchId";
	            	    params.put("serialFrom",emailDet.getSerialFrom());
	            	    params.put("serialEnd", emailDet.getSerialTo());
	            	    params.put("batchId",emailDet.getBatchId());
	            	    
	            	    log.info("::::::to fetch voucher details from VOUCHER_DET table::::::"+query);
	            	    
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
	            	    	fileContent+="'0+"+String.format("%07d",voucher.getSerialNumber())+"0000"+String.format("%012d",voucher.getVoucherNumber())+"'\n";
	            	    }
	            	    
	            	
	            	}
	            	else
	            	{
	            		log.error("TO MAIL ID IS MANDATORY");
	            	}
	            	
//	            }
	            
	            //formatting csv file header
	            query="SELECT a.AMOUNT, b.VOUCHER_QUANTITY FROM DENOMINATION_MAST a, BATCH_MAST b WHERE a.DENOMINATION_ID=b.DENOMINATION_ID and b.BATCH_ID=:batchId and b.SERIAL_START>=:serialFrom and b.SERIAL_END<=:serialEnd";
	            params.put("batchId", emailDetailsList.get(0).getBatchId());
	            params.put("serialFrom",emailDetailsList.get(0).getSerialFrom());
        	    params.put("serialEnd", emailDetailsList.get(0).getSerialTo());
        	    
        	    log.info("::::::to fetch denomaination and voucher quantity from DENOMINATION_MAST and BATCH_MAST table::::::"+query);
        	    
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
		    		  
		    	}
		    	catch(Exception e)
		    	{
		    		log.error(":::::Error while updation WVMS_EMAIL_DET:::::::"+e.getMessage());
		    		isEmailDetTableUpdated=false;
		    	}
	        	
	        	isZIPFileCreated=createZipFile(vmsHome,vmsCfgDir,csvFilePath,fileName,String.valueOf(filePassword));
	        
	          }
	        	
	        }  	
	        
	   }
	    
	    private static boolean createZipFile(String vmsHome,String vmsCfgDir,String filePathTemp,String fileName,String filePassword) throws Exception
	    {
	    	

	        boolean isCSVFileCreated = false,isZIPFileCreated=false;

            // Build the file path
            String filePath = vmsHome + "/" + vmsCfgDir + "/" + filePathTemp;
            String csvFilePath = filePath + "/" + fileName + ".csv";
            
            // Log the file path (you can use a logger instead of System.out)
            log.info("File path : " + csvFilePath);

            // The content you want to write to the CSV file
            String fileContent = "Your CSV content goes here.";  // Replace with actual content

            // Create the CSV file
            isCSVFileCreated=createFile(csvFilePath, fileContent);

            // Encrypt the created file
            String encryptedFilePath = filePath + "/" + fileName + "_encrypted.csv"; // Encrypted file path
            isZIPFileCreated=encryptFile(csvFilePath, encryptedFilePath, filePassword);

            log.info(isCSVFileCreated && isZIPFileCreated  ? "File created and encrypted successfully!" : "Problem in csv file creation");
            return isCSVFileCreated && isZIPFileCreated;
	    }
	    
	    private static boolean createFile(String filePath, String content) throws IOException {
	    	try
	        {
	    		File file = new File(filePath);
		        file.getParentFile().mkdirs();  // Ensure directories are created
		        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
		            writer.write(content);
		        }
		        catch (Exception e)
		    	{
		    		log.error("Error in creating csv file "+e.getMessage());
		    		return false;
		    	}
		        
	        }
	    	catch (Exception e)
	    	{
	    		log.error("Error in creating csv file "+e.getMessage());
	    		return false;
	    	}
	    	log.info("::::Successfully created the cvs file::::::::");
    		return true;
	    	
	    }
	    
	    public static boolean encryptFile(String sourceFilePath, String encryptedFilePath, String password) throws Exception {
	        // Generate AES key from password
	        SecureRandom sr = new SecureRandom(password.getBytes());
	        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
	        keyGen.init(128, sr);
	        SecretKey secretKey = keyGen.generateKey();

	        // Initialize Cipher
	        Cipher cipher = Cipher.getInstance("AES");
	        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

	        // Encrypt the file
	        try (FileInputStream fis = new FileInputStream(sourceFilePath);
	             FileOutputStream fos = new FileOutputStream(encryptedFilePath);
	             CipherOutputStream cos = new CipherOutputStream(fos, cipher)) {

	            byte[] buffer = new byte[1024];
	            int length;
	            while ((length = fis.read(buffer)) > 0) {
	                cos.write(buffer, 0, length);
	            }
	        }
	        catch (Exception e)
	    	{
	    		log.error("Error in creating zip file "+e.getMessage());
	    		return false;
	    	}
	        log.info("::::Successfully created the zip file::::::::");
    		return true;
	
	        
	    }

	    
	    public boolean updateEmailDet(int status,int serialStart ,int serialEnd)
	    {
	    	boolean isRecordsUpdated = false;
	    	String query = "";
	    	HashMap<String,Integer> params = new HashMap<String,Integer>();
	    	
	    	query = "UPDATE WVMS_EMAIL_DET SET STATUS=:status where SERIAL_FROM=:serialFrom and SERIAL_TO=:serialEnd";
	    	log.info(":::::query to update WVMS_EMAIL_DET:::::::"+query);
	    	params.put("status",status);
	    	params.put("serialFrom",serialStart);
	    	params.put("serialEnd",serialEnd);
	    	
	    	try {
	    		  isRecordsUpdated = namedDbJdbcTemplate.update(query, params) > 0 ;
	    		  return isRecordsUpdated;
	    	}
	    	catch(Exception e)
	    	{
	    		log.error(":::::Error while updation WVMS_EMAIL_DET:::::::"+e.getMessage());
	    		return false;
	    	}
	    	
	    			
	    }
	    
	    
	   


}