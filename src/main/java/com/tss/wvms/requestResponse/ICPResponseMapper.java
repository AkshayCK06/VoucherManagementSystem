package com.tss.wvms.requestResponse;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tss.wvms.requestResponse.ICPResponse;

@JsonInclude(JsonInclude.Include.NON_NULL) 
public class ICPResponseMapper {
    
    
    @JsonProperty("response")
    private ICPResponse icpResponse;
    
    private String appTxnRefId;
    private String ocsTxnRefId;
    private String accountValidity;
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

    public List<BalanceInfo> getBalances() {
        return balances;
    }

    public void setBalances(List<BalanceInfo> balances) {
        this.balances = balances;
    }

    public String getAccountValidity() {
        return accountValidity;
    }

    public void setAccountValidity(String accountValidity) {
        this.accountValidity = accountValidity;
    }

    public static class BalanceInfo {
        private String name;
        private int id;
        private int changeInBalance;
        private int newBalance;
        private String expiry;

        
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
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

        @Override
        public String toString() {
            return "BalanceInfo [name=" + name + ", id=" + id + ", changeInBalance=" + changeInBalance
                    + ", newBalance=" + newBalance + ", expiry=" + expiry + "]";
        }
    }

    public ICPResponseMapper() {
    }

    public ICPResponseMapper(ICPResponse icpResponse, String appTxnRefId, String ocsTxnRefId, String accountValidity,
            List<BalanceInfo> balances) {
        super();
        this.icpResponse = icpResponse;
        this.appTxnRefId = appTxnRefId;
        this.ocsTxnRefId = ocsTxnRefId;
        this.accountValidity = accountValidity;
        this.balances = balances;
    }

    @Override
    public String toString() {
        return "ICPResponseMapper [icpResponse=" + icpResponse + ", appTxnRefId=" + appTxnRefId + ", ocsTxnRefId="
                + ocsTxnRefId + ", accountValidity=" + accountValidity + ", balances=" + balances + "]";
    }
}
