package com.tss.wvms.model;

import java.math.BigDecimal;
import java.math.BigInteger;

public class TransactionDet {
		
	private int serviceType;
	private String serviceName;
	private int serviceUnit;
	private int serviceStatus;
	private int serviceValidity;
	private String requestDate;
	private String processDate;
	private int transactionType;
	private long transactionId;
	private BigDecimal icpReferenceId;
	private int pendingRetry;
	private String retryDate;
	private int responseCode;
	private String responseDescription;
	private String responseDate;
	private String serviceValididtyDate;
	
	public int getServiceType() {
		return serviceType;
	}
	public void setServiceType(int serviceType) {
		this.serviceType = serviceType;
	}
	public String getServiceName() {
		return serviceName;
	}
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	public int getServiceUnit() {
		return serviceUnit;
	}
	public void setServiceUnit(int serviceUnit) {
		this.serviceUnit = serviceUnit;
	}
	public int getServiceStatus() {
		return serviceStatus;
	}
	public void setServiceStatus(int serviceStatus) {
		this.serviceStatus = serviceStatus;
	}
	public int getServiceValidity() {
		return serviceValidity;
	}
	public void setServiceValidity(int serviceValidity) {
		this.serviceValidity = serviceValidity;
	}
	public String getRequestDate() {
		return requestDate;
	}
	public void setRequestDate(String requestDate) {
		this.requestDate = requestDate;
	}
	public int getTransactionType() {
		return transactionType;
	}
	public void setTransactionType(int transactionType) {
		this.transactionType = transactionType;
	}
	public long getTransactionId() {
		return transactionId;
	}
	public void setTransactionId(int transactionId) {
		this.transactionId = transactionId;
	}

	public String getServiceValididtyDate() {
		return serviceValididtyDate;
	}
	public void setServiceValididtyDate(String serviceValididtyDate) {
		this.serviceValididtyDate = serviceValididtyDate;
	}
	
	public String getProcessDate() {
		return processDate;
	}
	public void setProcessDate(String processDate) {
		this.processDate = processDate;
	}
	public BigDecimal getIcpReferenceId() {
		return icpReferenceId;
	}
	public void setIcpReferenceId(BigDecimal icpReferenceId) {
		this.icpReferenceId = icpReferenceId;
	}
	public int getPendingRetry() {
		return pendingRetry;
	}
	public void setPendingRetry(int pendingRetry) {
		this.pendingRetry = pendingRetry;
	}
	public String getRetryDate() {
		return retryDate;
	}
	public void setRetryDate(String retryDate) {
		this.retryDate = retryDate;
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
	public String getResponseDate() {
		return responseDate;
	}
	public void setResponseDate(String responseDate) {
		this.responseDate = responseDate;
	}
	public void setTransactionId(long transactionId) {
		this.transactionId = transactionId;
	}
	
	public TransactionDet()
	{
		
	}
	
	public TransactionDet(int serviceType, String serviceName, int serviceUnit, int serviceStatus, int serviceValidity,
			String requestDate, int transactionType, long transactionId,String serviceValididtyDate) {
		super();
		this.serviceType = serviceType;
		this.serviceName = serviceName;
		this.serviceUnit = serviceUnit;
		this.serviceStatus = serviceStatus;
		this.serviceValidity = serviceValidity;
		this.requestDate = requestDate;
		this.transactionType = transactionType;
		this.transactionId = transactionId;
		this.serviceValididtyDate = serviceValididtyDate;
	}
	
	
	public TransactionDet(int serviceType, String serviceName, int serviceUnit, int serviceStatus, int serviceValidity,
			String requestDate, String processDate, int transactionType, long transactionId, BigDecimal icpReferenceId,
			int pendingRetry, String retryDate, int responseCode, String responseDescription, String responseDate) {
		super();
		this.serviceType = serviceType;
		this.serviceName = serviceName;
		this.serviceUnit = serviceUnit;
		this.serviceStatus = serviceStatus;
		this.serviceValidity = serviceValidity;
		this.requestDate = requestDate;
		this.processDate = processDate;
		this.transactionType = transactionType;
		this.transactionId = transactionId;
		this.icpReferenceId = icpReferenceId;
		this.pendingRetry = pendingRetry;
		this.retryDate = retryDate;
		this.responseCode = responseCode;
		this.responseDescription = responseDescription;
		this.responseDate = responseDate;
	}
	
	@Override
	public String toString() {
		return "TransactionDet [serviceType=" + serviceType + ", serviceName=" + serviceName + ", serviceUnit="
				+ serviceUnit + ", serviceStatus=" + serviceStatus + ", serviceValidity=" + serviceValidity
				+ ", requestDate=" + requestDate + ", processDate=" + processDate + ", transactionType="
				+ transactionType + ", transactionId=" + transactionId + ", icpReferenceId=" + icpReferenceId
				+ ", pendingRetry=" + pendingRetry + ", retryDate=" + retryDate + ", responseCode=" + responseCode
				+ ", responseDescription=" + responseDescription + ", responseDate=" + responseDate
				+ ", serviceValididtyDate=" + serviceValididtyDate + "]";
	}
	
	
	
	
	
	
}
