package com.tss.wvms.requestResponse;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

import java.util.List;

@JsonRootName("WVMS")
@JsonInclude(JsonInclude.Include.NON_NULL) 
public class VoucherRedeemResponse {
	
	    @JsonProperty("RESPCODE")
	    private String respCode;

	    @JsonProperty("RESPDESC")
	    private String respDesc;

	    @JsonProperty("BALINFO")
	    private List<BalanceInfo> balInfo;

	    @JsonProperty("DENOMAMOUNT")
	    private String denomAmount;

	    // Getters and Setters
	    public String getRespCode() {
	        return respCode;
	    }

	    public void setRespCode(String respCode) {
	        this.respCode = respCode;
	    }

	    public String getRespDesc() {
	        return respDesc;
	    }

	    public void setRespDesc(String respDesc) {
	        this.respDesc = respDesc;
	    }

	    public List<BalanceInfo> getBalInfo() {
	        return balInfo;
	    }

	    public void setBalInfo(List<BalanceInfo> balInfo) {
	        this.balInfo = balInfo;
	    }

	    public String getDenomAmount() {
	        return denomAmount;
	    }

	    public void setDenomAmount(String denomAmount) {
	        this.denomAmount = denomAmount;
	    }
	    
	    
	    public static class BalanceInfo {

	        @JsonProperty("NAME")
	        private String name;

	        @JsonProperty("NEWBALNCE")
	        private String newBalance;

	        @JsonProperty("NEWEXPIRY")
	        private String newExpiry;

	        // Getters and Setters
	        public String getName() {
	            return name;
	        }

	        public void setName(String name) {
	            this.name = name;
	        }

	        public String getNewBalance() {
	            return newBalance;
	        }

	        public void setNewBalance(String newBalance) {
	            this.newBalance = newBalance;
	        }

	        public String getNewExpiry() {
	            return newExpiry;
	        }

	        public void setNewExpiry(String newExpiry) {
	            this.newExpiry = newExpiry;
	        }
	        
	        public BalanceInfo()
	        {
	        	
	        }
	    }

	    @Override
	    public String toString() {
	        return "WVMSResponse{" +
	                "respCode='" + respCode + '\'' +
	                ", respDesc='" + respDesc + '\'' +
	                ", balInfo=" + balInfo +
	                ", denomAmount='" + denomAmount + '\'' +
	                '}';
	    }

}
