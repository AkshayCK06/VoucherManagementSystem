package com.tss.wvms.model;

public class EmailDet {

	//SELECT ESEQ_ID,BATCH_ID,BATCH_NAME,SERIAL_FROM,SERIAL_TO,
	//CSV_TO_EMAIL,CSV_CC_EMAIL,CSV_BCC_EMAIL,PASS_TO_EMAIL,
	//PASS_CC_EMAIL,PASS_BCC_EMAIL,CREATION_DATE,
	//SEND_DATE,STATUS,SUPERIOR_EMAIL FROM WVMS_EMAIL_DET WHERE SERIAL_FROM=? and SERIAL_TO=? and BATCH_ID
	private int serialFrom;
	private int serialTo;
	private int batchId;
	
	private int seqId;
	private String batchName;
	private String csvToEmail;
	private String csvCCEmail;
	private String csvBCCEmail;
	private String passToEmail;
	private String passCCEmail;
	private String passBCCEmail;
	private String creationDate;
	private String sendDate;
	private int status;
	private String superiorEmail;
	
	public int getSerialFrom() {
		return serialFrom;
	}
	public void setSerialFrom(int serialFrom) {
		this.serialFrom = serialFrom;
	}
	public int getSerialTo() {
		return serialTo;
	}
	public void setSerialTo(int serialTo) {
		this.serialTo = serialTo;
	}
	public int getBatchId() {
		return batchId;
	}
	public void setBatchId(int batchId) {
		this.batchId = batchId;
	}
	public int getSeqId() {
		return seqId;
	}
	public void setSeqId(int seqId) {
		this.seqId = seqId;
	}
	public String getBatchName() {
		return batchName;
	}
	public void setBatchName(String batchName) {
		this.batchName = batchName;
	}
	public String getCsvToEmail() {
		return csvToEmail;
	}
	public void setCsvToEmail(String csvToEmail) {
		this.csvToEmail = csvToEmail;
	}
	public String getCsvCCEmail() {
		return csvCCEmail;
	}
	public void setCsvCCEmail(String csvCCEmail) {
		this.csvCCEmail = csvCCEmail;
	}
	public String getCsvBCCEmail() {
		return csvBCCEmail;
	}
	public void setCsvBCCEmail(String csvBCCEmail) {
		this.csvBCCEmail = csvBCCEmail;
	}
	public String getPassToEmail() {
		return passToEmail;
	}
	public void setPassToEmail(String passToEmail) {
		this.passToEmail = passToEmail;
	}
	public String getPassCCEmail() {
		return passCCEmail;
	}
	public void setPassCCEmail(String passCCEmail) {
		this.passCCEmail = passCCEmail;
	}
	public String getPassBCCEmail() {
		return passBCCEmail;
	}
	public void setPassBCCEmail(String passBCCEmail) {
		this.passBCCEmail = passBCCEmail;
	}
	public String getCreationDate() {
		return creationDate;
	}
	public void setCreationDate(String creationDate) {
		this.creationDate = creationDate;
	}
	public String getSendDate() {
		return sendDate;
	}
	public void setSendDate(String sendDate) {
		this.sendDate = sendDate;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public String getSuperiorEmail() {
		return superiorEmail;
	}
	public void setSuperiorEmail(String superiorEmail) {
		this.superiorEmail = superiorEmail;
	}
	
	public EmailDet() {
		
	}
	
	public EmailDet(int serialFrom, int serialTo, int batchId) {
		super();
		this.serialFrom = serialFrom;
		this.serialTo = serialTo;
		this.batchId = batchId;
	}
	
	public EmailDet(int serialFrom, int serialTo, int batchId, int seqId, String batchName, String csvToEmail,
			String csvCCEmail, String csvBCCEmail, String passToEmail, String passCCEmail, String passBCCEmail,
			String creationDate, String sendDate, int status, String superiorEmail) {
		super();
		this.serialFrom = serialFrom;
		this.serialTo = serialTo;
		this.batchId = batchId;
		this.seqId = seqId;
		this.batchName = batchName;
		this.csvToEmail = csvToEmail;
		this.csvCCEmail = csvCCEmail;
		this.csvBCCEmail = csvBCCEmail;
		this.passToEmail = passToEmail;
		this.passCCEmail = passCCEmail;
		this.passBCCEmail = passBCCEmail;
		this.creationDate = creationDate;
		this.sendDate = sendDate;
		this.status = status;
		this.superiorEmail = superiorEmail;
	}
	
	@Override
	public String toString() {
		return "EmailDet [serialFrom=" + serialFrom + ", serialTo=" + serialTo + ", batchId=" + batchId + ", seqId="
				+ seqId + ", batchName=" + batchName + ", csvToEmail=" + csvToEmail + ", csvCCEmail=" + csvCCEmail
				+ ", csvBCCEmail=" + csvBCCEmail + ", passToEmail=" + passToEmail + ", passCCEmail=" + passCCEmail
				+ ", passBCCEmail=" + passBCCEmail + ", creationDate=" + creationDate + ", sendDate=" + sendDate
				+ ", status=" + status + ", superiorEmail=" + superiorEmail + "]";
	}
	
	
	
		
	
	
	
}
