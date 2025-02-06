package com.tss.wvms.model;

public class CustomerMast 
{
    private long msisdn;
    private int failCount;
    private String lastModifyDate;
   
	public long getMsisdn() {
		return msisdn;
	}
	public void setMsisdn(int msisdn) {
		this.msisdn = msisdn;
	}
	public int getFailCount() {
		return failCount;
	}
	public void setFailCount(int failCount) {
		this.failCount = failCount;
	}
	public String getLastModifyDate() {
		return lastModifyDate;
	}
	public void setLastModifyDate(String lastModifyDate) {
		this.lastModifyDate = lastModifyDate;
	}
	
	public CustomerMast()
	{
		
	}
	
	public CustomerMast(long msisdn, int failCount, String lastModifyDate) {
		super();
		this.msisdn = msisdn;
		this.failCount = failCount;
		this.lastModifyDate = lastModifyDate;
	}
	
	@Override
	public String toString() {
		return "CustomerMast [msisdn=" + msisdn + ", failCount=" + failCount + ", lastModifyDate=" + lastModifyDate
				+ "]";
	}
   
}
