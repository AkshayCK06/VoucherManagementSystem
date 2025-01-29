package com.tss.wvms.model;

public class ReasonMast {

	private int responseCode;
	private String responseMessage;
	
	public int getResponseCode() {
		return responseCode;
	}
	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}
	public String getResponseMessage() {
		return responseMessage;
	}
	public void setResponseMessage(String responseMessage) {
		this.responseMessage = responseMessage;
	}
	
	public ReasonMast()
	{
		
	}
	
	public ReasonMast(int responseCode, String responseMessage) {
		super();
		this.responseCode = responseCode;
		this.responseMessage = responseMessage;
	}
	@Override
	public String toString() {
		return "ReasonMast [responseCode=" + responseCode + ", responseMessage=" + responseMessage + "]";
	}
	
	
}
