package com.tss.wvms.model;

public class VoucherDet {
    
	private int batchId;
	private long voucherNumber;
    private int serialNumber;
    private int status;
    
    public int getBatchId() {
		return batchId;
	}
	public void setBatchId(int batchId) {
		this.batchId = batchId;
	}
	public long getVoucherNumber() {
		return voucherNumber;
	}
	public void setVoucherNumber(long voucherNumber) {
		this.voucherNumber = voucherNumber;
	}
	public int getSerialNumber() {
		return serialNumber;
	}
	public void setSerialNumber(int serialNumber) {
		this.serialNumber = serialNumber;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	
	public VoucherDet()
	{
		
	}
	
	public VoucherDet(int batchId, long voucherNumber, int serialNumber, int status) {
		super();
		this.batchId = batchId;
		this.voucherNumber = voucherNumber;
		this.serialNumber = serialNumber;
		this.status = status;
	}
	
	public VoucherDet(long voucherNumber, int serialNumber) {
		super();
		this.voucherNumber = voucherNumber;
		this.serialNumber = serialNumber;
	}
	
	
	@Override
	public String toString() {
		return "VoucherDet [batchId=" + batchId + ", voucherNumber=" + voucherNumber + ", serialNumber=" + serialNumber
				+ ", status=" + status + "]";
	}
	
	
    
    
}
