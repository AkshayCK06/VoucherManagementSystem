package com.tss.wvms.requestResponse;

public class CWSResponse 
{
     private String responseCode;
     private String responseDescription;
     private String status;
     
     public String getResponseCode() {
		return responseCode;
	 }
	 public void setResponseCode(String responseCode) {
		this.responseCode = responseCode;
	 }
	 public String getResponseDescription() {
		return responseDescription;
	 }
	 public void setResponseDescription(String responseDescription) {
		this.responseDescription = responseDescription;
	 }
	 public String getStatus() {
		return status;
     }
	 public void setStatus(String status) {
		this.status = status;
	 }
	 
	 public CWSResponse()
	 {
		 
	 }
	 
	 public CWSResponse(String responseCode, String responseDescription, String status) {
		super();
		this.responseCode = responseCode;
		this.responseDescription = responseDescription;
		this.status = status;
	 }
	 
	 @Override
	 public String toString() {
		return "CWSResponse [responseCode=" + responseCode + ", responseDescription=" + responseDescription
				+ ", status=" + status + "]";
	 } 
     
	 
     
}
