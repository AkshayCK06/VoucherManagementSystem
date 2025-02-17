package com.tss.wvms.requestResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

import jakarta.validation.constraints.NotBlank;

// Specify the root element "WVMS"
@JsonRootName("WVMS")
public class VoucherRedeemRequest {

    @JsonProperty("VOUCHERFLAG")
    @NotBlank(message = "Voucher flag is mandatory")
    private String voucherFlag;

    @JsonProperty("MSISDN")
    @NotBlank(message = "MSISDN is mandatory")
    private String msisdn;

    @JsonProperty("VOUCHERNO")
    @NotBlank(message = "Voucher number is mandatory")
    private String voucherNo;

   
    public VoucherRedeemRequest() {}


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
