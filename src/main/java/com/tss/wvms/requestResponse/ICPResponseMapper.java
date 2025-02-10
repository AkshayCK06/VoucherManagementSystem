package com.tss.wvms.requestResponse;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ICPResponseMapper {

    @JsonProperty("response")
    private ICPResponse icpResponse;

    @JsonProperty("appTxnRefId")
    private String appTxnRefId;

    @JsonProperty("ocsTxnRefId")
    private String ocsTxnRefId;

    @JsonProperty("accountValidity")
    private String accountValidity;

    @JsonProperty("balances")
    private List<BalanceInfo> balances;

    public ICPResponse getIcpResponse() {
        return icpResponse;
    }

    public void setIcpResponse(ICPResponse icpResponse) {
        this.icpResponse = icpResponse;
    }

    public String getAppTxnRefId() {
        return appTxnRefId;
    }

    public void setAppTxnRefId(String appTxnRefId) {
        this.appTxnRefId = appTxnRefId;
    }

    public String getOcsTxnRefId() {
        return ocsTxnRefId;
    }

    public void setOcsTxnRefId(String ocsTxnRefId) {
        this.ocsTxnRefId = ocsTxnRefId;
    }

    public String getAccountValidity() {
        return accountValidity;
    }

    public void setAccountValidity(String accountValidity) {
        this.accountValidity = accountValidity;
    }

    public List<BalanceInfo> getBalances() {
        return balances;
    }

    public void setBalances(List<BalanceInfo> balances) {
        this.balances = balances;
    }

    @Override
    public String toString() {
        return "ICPResponseMapper [icpResponse=" + icpResponse + ", appTxnRefId=" + appTxnRefId + ", ocsTxnRefId="
                + ocsTxnRefId + ", accountValidity=" + accountValidity + ", balances=" + balances + "]";
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BalanceInfo {

        @JsonProperty("id")
        private int id;

        @JsonProperty("name")
        private String name;

        @JsonProperty("changeInBalance")
        private int changeInBalance;

        @JsonProperty("newBalance")
        private int newBalance;

        @JsonProperty("expiry")
        private String expiry;

        @JsonProperty("packageId")
        private int packageId;

        @JsonProperty("SmartTag")
        private String smartTag;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getChangeInBalance() {
            return changeInBalance;
        }

        public void setChangeInBalance(int changeInBalance) {
            this.changeInBalance = changeInBalance;
        }

        public int getNewBalance() {
            return newBalance;
        }

        public void setNewBalance(int newBalance) {
            this.newBalance = newBalance;
        }

        public String getExpiry() {
            return expiry;
        }

        public void setExpiry(String expiry) {
            this.expiry = expiry;
        }

        public int getPackageId() {
            return packageId;
        }

        public void setPackageId(int packageId) {
            this.packageId = packageId;
        }

        public String getSmartTag() {
            return smartTag;
        }

        public void setSmartTag(String smartTag) {
            this.smartTag = smartTag;
        }

        @Override
        public String toString() {
            return "BalanceInfo [id=" + id + ", name=" + name + ", changeInBalance=" + changeInBalance
                    + ", newBalance=" + newBalance + ", expiry=" + expiry + ", packageId=" + packageId + ", smartTag=" + smartTag + "]";
        }
    }
}
