package com.tss.wvms.model;

public class MessageMast {

	private String messageId;
	private String message;
	
	public String getMessageId() {
		return messageId;
	}
	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	
	public MessageMast () {
		
	}
	public MessageMast(String messageId, String message) {
		super();
		this.messageId = messageId;
		this.message = message;
	}
	@Override
	public String toString() {
		return "MessageMast [messageId=" + messageId + ", message=" + message + "]";
	}
	
	
	
}
