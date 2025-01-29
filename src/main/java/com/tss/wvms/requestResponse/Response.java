package com.tss.wvms.requestResponse;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class Response {
	
	private int errorCode;
	private String errorDescription;

   public int getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}
	public String getErrorDescription() {
		return errorDescription;
	}
	public void setErrorDescription(String errorDescription) {
		this.errorDescription = errorDescription;
	}
	public Response(int errorCode, String errorDescription) {
		super();
		this.errorCode = errorCode;
		this.errorDescription = errorDescription;
	}

	
}
