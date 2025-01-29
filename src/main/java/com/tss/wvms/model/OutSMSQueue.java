package com.tss.wvms.model;


public class OutSMSQueue {

	private String msgId;
	private String destMSISDN;
	private String fromMSISDN;
	private String message;
	private String dateTime;
	private String messageStatus;
	private String transactionId;
	
	public String getMsgId() {
		return msgId;
	}
	public void setMsgId(String msgId) {
		this.msgId = msgId;
	}
	public String getDestMSISDN() {
		return destMSISDN;
	}
	public void setDestMSISDN(String destMSISDN) {
		this.destMSISDN = destMSISDN;
	}
	public String getFromMSISDN() {
		return fromMSISDN;
	}
	public void setFromMSISDN(String fromMSISDN) {
		this.fromMSISDN = fromMSISDN;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getDateTime() {
		return dateTime;
	}
	public void setDateTime(String dateTime) {
		this.dateTime = dateTime;
	}
	public String getMessageStatus() {
		return messageStatus;
	}
	public void setMessageStatus(String messageStatus) {
		this.messageStatus = messageStatus;
	}
	public String getTransactionId() {
		return transactionId;
	}
	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}
	
	public OutSMSQueue()
	{
		
	}
	
	public OutSMSQueue(String msgId, String destMSISDN, String fromMSISDN, String message, String dateTime,
			String messageStatus, String transactionId) {
		super();
		this.msgId = msgId;
		this.destMSISDN = destMSISDN;
		this.fromMSISDN = fromMSISDN;
		this.message = message;
		this.dateTime = dateTime;
		this.messageStatus = messageStatus;
		this.transactionId = transactionId;
	}
	
	@Override
	public String toString() {
		return "OutSMSQueue [msgId=" + msgId + ", destMSISDN=" + destMSISDN + ", fromMSISDN=" + fromMSISDN
				+ ", message=" + message + ", dateTime=" + dateTime + ", messageStatus=" + messageStatus
				+ ", transactionId=" + transactionId + "]";
	}
	
	
	
	
	
}
