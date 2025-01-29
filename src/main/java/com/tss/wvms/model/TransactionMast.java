package com.tss.wvms.model;

public class TransactionMast {

	//SELECT SEQ_ID,TRANSACTION_ID,to_char(REQ_DATE,'dd-mm-yyyy hh24:mi:ss'),
	//SUBSCRIBER_MSISDN,STATUS,BATCH_NUMBER,REQ_MODE,VOUCHER_NUMBER,APPLICABLE_COS,
	//SERVICE_FULFILLMENT_COS,VOUCHER_AMOUNT,SERIAL_NUMBER,VOUCHER_AMOUNT 
	//FROM TRANSACTION_MAST WHERE STATUS=0 and PROCESS_ID=$processId and ROWNUM<10
	
	private int transactionSequenceId;
	private long transactionId;
	private String requestDate;
	private int msisdn;
	private int status;
	private int batchNumber;
	private int requestMode;
	private long voucherNumber;
	private int applicableCOS;
	private int serviceFullfillmentCOS;
	private int voucherAmount;
	private int serialNumber;
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

	public int getMsisdn() {
		return msisdn;
	}

	public void setMsisdn(int msisdn) {
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
	
	public TransactionMast(int transactionSequenceId, long transactionId, String requestDate, int msisdn, int status,
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

	@Override
	public String toString() {
		return "TransactionMast [transactionSequenceId=" + transactionSequenceId + ", transactionId=" + transactionId
				+ ", requestDate=" + requestDate + ", msisdn=" + msisdn + ", status=" + status + ", batchNumber="
				+ batchNumber + ", requestMode=" + requestMode + ", voucherNumber=" + voucherNumber + ", applicableCOS="
				+ applicableCOS + ", serviceFullfillmentCOS=" + serviceFullfillmentCOS + ", voucherAmount="
				+ voucherAmount + ", serialNumber=" + serialNumber + ", responseCode=" + responseCode
				+ ", responseDescription=" + responseDescription + ", ivrBalanceInfo=" + ivrBalanceInfo + "]";
	}

	
	
	
	
	
}
