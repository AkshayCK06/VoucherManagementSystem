package com.tss.wvms.model;

public class TransactionDet {
	
	private int serviceType;
	private String serviceName;
	private int serviceUnit;
	private int serviceStatus;
	private int serviceValidity;
	private String requestDate;
	private int transactionType;
	private long transactionId;
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
	
	@Override
	public String toString() {
		return "TransactionDet [serviceType=" + serviceType + ", serviceName=" + serviceName + ", serviceUnit="
				+ serviceUnit + ", serviceStatus=" + serviceStatus + ", serviceValidity=" + serviceValidity
				+ ", requestDate=" + requestDate + ", transactionType=" + transactionType + ", transactionId="
				+ transactionId + "]";
	}
	
	
	
	
}
