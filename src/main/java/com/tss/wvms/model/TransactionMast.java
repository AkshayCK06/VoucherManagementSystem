package com.tss.wvms.model;

public class TransactionMast {
	
	private int transactionSequenceId;
	private long transactionId;
	private String requestDate;
	private String requestDateMonthWise;
	private String msisdn;
	private int status;
	private int batchNumber;
	private int requestMode;
	private long voucherNumber;
	private int applicableCOS;
	private int serviceFullfillmentCOS;
	private int voucherAmount;
	private int serialNumber;
	private int requestType;
	private int responseCode;
	private String responseDescription;
	private String  ivrBalanceInfo;
	
	
	public int getTransactionSequenceId() {
		return transactionSequenceId;
	}

	public void setTransactionSequenceId(int transactionSequenceId) {
		this.transactionSequenceId = transactionSequenceId;
	}

	public long getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(int transactionId) {
		this.transactionId = transactionId;
	}

	public String getRequestDate() {
		return requestDate;
	}

	public void setRequestDate(String requestDate) {
		this.requestDate = requestDate;
	}

	public String getMsisdn() {
		return msisdn;
	}

	public void setMsisdn(String msisdn) {
		this.msisdn = msisdn;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public int getBatchNumber() {
		return batchNumber;
	}

	public void setBatchNumber(int batchNumber) {
		this.batchNumber = batchNumber;
	}

	public int getRequestMode() {
		return requestMode;
	}

	public void setRequestMode(int requestMode) {
		this.requestMode = requestMode;
	}

	public long getVoucherNumber() {
		return voucherNumber;
	}

	public void setVoucherNumber(long voucherNumber) {
		this.voucherNumber = voucherNumber;
	}

	public int getApplicableCOS() {
		return applicableCOS;
	}

	public void setApplicableCOS(int applicableCOS) {
		this.applicableCOS = applicableCOS;
	}

	public int getServiceFullfillmentCOS() {
		return serviceFullfillmentCOS;
	}

	public void setServiceFullfillmentCOS(int serviceFullfillmentCOS) {
		this.serviceFullfillmentCOS = serviceFullfillmentCOS;
	}

	public int getVoucherAmount() {
		return voucherAmount;
	}

	public void setVoucherAmount(int voucherAmount) {
		this.voucherAmount = voucherAmount;
	}

	public int getSerialNumber() {
		return serialNumber;
	}

	public void setSerialNumber(int serialNumber) {
		this.serialNumber = serialNumber;
	}

	public int getResponseCode() {
		return responseCode;
	}

	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}

	public String getResponseDescription() {
		return responseDescription;
	}

	public void setResponseDescription(String responseDescription) {
		this.responseDescription = responseDescription;
	}

	public String getIvrBalanceInfo() {
		return ivrBalanceInfo;
	}

	public void setIvrBalanceInfo(String ivrBalanceInfo) {
		this.ivrBalanceInfo = ivrBalanceInfo;
	}

	public String getRequestDateMonthWise() {
		return requestDateMonthWise;
	}

	public void setRequestDateMonthWise(String requestDateMonthWise) {
		this.requestDateMonthWise = requestDateMonthWise;
	}

	public int getRequestType() {
		return requestType;
	}

	public void setRequestType(int requestType) {
		this.requestType = requestType;
	}

	public TransactionMast()
	{
		
	}
	
	public TransactionMast(int status, int responseCode, String responseDescription, String ivrBalanceInfo) {
		super();
		this.status = status;
		this.responseCode = responseCode;
		this.responseDescription = responseDescription;
		this.ivrBalanceInfo = ivrBalanceInfo;
	}
	
	
	public TransactionMast(int transactionSequenceId, long transactionId, String requestDate, String msisdn, int status,
			int batchNumber, int requestMode, long voucherNumber, int applicableCOS, int serviceFullfillmentCOS,
			int voucherAmount, int serialNumber) {
		super();
		this.transactionSequenceId = transactionSequenceId;
		this.transactionId = transactionId;
		this.requestDate = requestDate;
		this.msisdn = msisdn;
		this.status = status;
		this.batchNumber = batchNumber;
		this.requestMode = requestMode;
		this.voucherNumber = voucherNumber;
		this.applicableCOS = applicableCOS;
		this.serviceFullfillmentCOS = serviceFullfillmentCOS;
		this.voucherAmount = voucherAmount;
		this.serialNumber = serialNumber;
	}
	

	public TransactionMast(int transactionSequenceId, long transactionId, String requestDate, String msisdn, int status,
			int batchNumber, int requestMode, long voucherNumber, int applicableCOS, int serviceFullfillmentCOS) {
		super();
		this.transactionSequenceId = transactionSequenceId;
		this.transactionId = transactionId;
		this.requestDate = requestDate;
		this.msisdn = msisdn;
		this.status = status;
		this.batchNumber = batchNumber;
		this.requestMode = requestMode;
		this.voucherNumber = voucherNumber;
		this.applicableCOS = applicableCOS;
		this.serviceFullfillmentCOS = serviceFullfillmentCOS;
	}
	
	
	
	public TransactionMast(int transactionSequenceId, long transactionId, String requestDate,
			String requestDateMonthWise, String msisdn, int status, int batchNumber, int requestMode, long voucherNumber,
			int applicableCOS, int serviceFullfillmentCOS,int responseCode, int serialNumber, int requestType,int voucherAmount) {
		super();
		this.transactionSequenceId = transactionSequenceId;
		this.transactionId = transactionId;
		this.requestDate = requestDate;
		this.requestDateMonthWise = requestDateMonthWise;
		this.msisdn = msisdn;
		this.status = status;
		this.batchNumber = batchNumber;
		this.requestMode = requestMode;
		this.voucherNumber = voucherNumber;
		this.applicableCOS = applicableCOS;
		this.serviceFullfillmentCOS = serviceFullfillmentCOS;
		this.responseCode = responseCode;	
		this.serialNumber = serialNumber;
		this.requestType = requestType;
		this.voucherAmount = voucherAmount;
	}
	
	public TransactionMast(int transactionSequenceId, long transactionId, String requestDate, String msisdn, int status,
			int batchNumber, int requestMode, long voucherNumber, int applicableCOS, int serviceFullfillmentCOS,
			int voucherAmount, int serialNumber, String responseDescription) {
		super();
		this.transactionSequenceId = transactionSequenceId;
		this.transactionId = transactionId;
		this.requestDate = requestDate;
		this.msisdn = msisdn;
		this.status = status;
		this.batchNumber = batchNumber;
		this.requestMode = requestMode;
		this.voucherNumber = voucherNumber;
		this.applicableCOS = applicableCOS;
		this.serviceFullfillmentCOS = serviceFullfillmentCOS;
		this.voucherAmount = voucherAmount;
		this.serialNumber = serialNumber;
		this.responseDescription = responseDescription;
	}

	@Override
	public String toString() {
		return "TransactionMast [transactionSequenceId=" + transactionSequenceId + ", transactionId=" + transactionId
				+ ", requestDate=" + requestDate + ", requestDateMonthWise=" + requestDateMonthWise + ", msisdn="
				+ msisdn + ", status=" + status + ", batchNumber=" + batchNumber + ", requestMode=" + requestMode
				+ ", voucherNumber=" + voucherNumber + ", applicableCOS=" + applicableCOS + ", serviceFullfillmentCOS="
				+ serviceFullfillmentCOS + ", voucherAmount=" + voucherAmount + ", serialNumber=" + serialNumber
				+ ", requestType=" + requestType + ", responseCode=" + responseCode + ", responseDescription="
				+ responseDescription + ", ivrBalanceInfo=" + ivrBalanceInfo + "]";
	}

	
	
	
	
	
	
}
