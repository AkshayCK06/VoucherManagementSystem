package com.tss.wvms.requestResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

// Specify the root element "WVMS"
@JsonRootName("WVMS")
public class VoucherRedeemRequest {

    @JsonProperty("VOUCHERFLAG")
    private String voucherFlag;

    @JsonProperty("MSISDN")
    private String msisdn;

    @JsonProperty("VOUCHERNO")
    private String voucherNo;

    // Default constructor
    public VoucherRedeemRequest() {}

    // Getters and Setters
    public String getVoucherFlag() {
        return voucherFlag;
    }

    public void setVoucherFlag(String voucherFlag) {
        this.voucherFlag = voucherFlag;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public String getVoucherNo() {
        return voucherNo;
    }

    public void setVoucherNo(String voucherNo) {
        this.voucherNo = voucherNo;
    }

    @Override
    public String toString() {
        return "VoucherRedeemRequest{" +
                "voucherFlag='" + voucherFlag + '\'' +
                ", msisdn='" + msisdn + '\'' +
                ", voucherNo='" + voucherNo + '\'' +
                '}';
    }
}
