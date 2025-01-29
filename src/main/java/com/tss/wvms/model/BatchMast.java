package com.tss.wvms.model;


public class BatchMast {
	
	private int batchId;
	private int denomainationId;
	private int creatorId;
	private String creationDate;
	private int voucherQuantity;
	private int bonusId;
	private String serialStart;
	private String serialEnd;
	private String batchName;
	private int rateId;
	private int status;
	private String enableDate;
	
	
	public int getBatchId() {
        return batchId;
    }

    public void setBatchId(int batchId) {
        this.batchId = batchId;
    }

    public int getDenomainationId() {
        return denomainationId;
    }

    public void setDenomainationId(int denomainationId) {
        this.denomainationId = denomainationId;
    }

    public int getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(int creatorId) {
        this.creatorId = creatorId;
    }

    
    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    
    public int getVoucherQuantity() {
        return voucherQuantity;
    }

    public void setVoucherQuantity(int voucherQuantity) {
        this.voucherQuantity = voucherQuantity;
    }

    
    public int getBonusId() {
        return bonusId;
    }

    public void setBonusId(int bonusId) {
        this.bonusId = bonusId;
    }

    
    public String getSerialStart() {
        return serialStart;
    }

    public void setSerialStart(String serialStart) {
        this.serialStart = serialStart;
    }

   
    public String getSerialEnd() {
        return serialEnd;
    }

    public void setSerialEnd(String serialEnd) {
        this.serialEnd = serialEnd;
    }

    
    public String getBatchName() {
        return batchName;
    }

    public void setBatchName(String batchName) {
        this.batchName = batchName;
    }


	public int getRateId() {
        return rateId;
    }

    public void setRateId(int rateId) {
        this.rateId = rateId;
    }
    
    public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}
	
	public String getEnableDate() {
		return enableDate;
	}

	public void setEnableDate(String enableDate) {
		this.enableDate = enableDate;
	}

	public BatchMast()
    {
    	
    }

	public BatchMast(int batchId, int denomainationId, int creatorId, String creationDate, int voucherQuantity,
			int bonusId, String serialStart, String serialEnd, String batchName, int rateId) {
		super();
		this.batchId = batchId;
		this.denomainationId = denomainationId;
		this.creatorId = creatorId;
		this.creationDate = creationDate;
		this.voucherQuantity = voucherQuantity;
		this.bonusId = bonusId;
		this.serialStart = serialStart;
		this.serialEnd = serialEnd;
		this.batchName = batchName;
		this.rateId = rateId;
	}
	
	public BatchMast(int batchId) {
		super();
		this.batchId = batchId;
	}
		
	public BatchMast(int batchId,int status,int denomainationId,int bonusId,String enableDate)
	{
		this.batchId=batchId;
		this.status=status;
		this.denomainationId = denomainationId;
		this.bonusId = bonusId;
		this.enableDate = enableDate;
	}

	public BatchMast(int voucherQuantity, String serialStart, String serialEnd,String batchName) {
		super();
		this.voucherQuantity = voucherQuantity;
		this.serialStart = serialStart;
		this.serialEnd = serialEnd;
		this.batchName = batchName;
	}

	@Override
	public String toString() {
		return "BatchMast [batchId=" + batchId + ", denomainationId=" + denomainationId + ", creatorId=" + creatorId
				+ ", creationDate=" + creationDate + ", voucherQuantity=" + voucherQuantity + ", bonusId=" + bonusId
				+ ", serialStart=" + serialStart + ", serialEnd=" + serialEnd + ", batchName=" + batchName + ", rateId="
				+ rateId + ", status=" + status + ", enableDate=" + enableDate + "]";
	}

	
	

}
