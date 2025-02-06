package com.tss.wvms.requestResponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;


@JsonRootName("response")
@JsonInclude(JsonInclude.Include.NON_NULL) 
public class ICPResponse 
{
	
    @JsonProperty("errorCode")
    private int errorCode;
    
    @JsonProperty("status")
    private int status;
    
    @JsonProperty("description")
    private String description;

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	
	public ICPResponse() {
		
	}
	public ICPResponse(int errorCode, int status, String description) {
		super();
		this.errorCode = errorCode;
		this.status = status;
		this.description = description;
	}

	@Override
	public String toString() {
		return "ICPResponse [errorCode=" + errorCode + ", status=" + status + ", description=" + description + "]";
	}
    
    
	
}
