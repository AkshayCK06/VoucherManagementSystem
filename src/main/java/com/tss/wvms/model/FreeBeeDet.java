package com.tss.wvms.model;

public class FreeBeeDet {

	private int freeBeeType;
	private int freeBeeValue;
	private int freebeeValidity;
	private int freebeeId;
	
	public int getFreeBeeType() {
		return freeBeeType;
	}
	public void setFreeBeeType(int freeBeeType) {
		this.freeBeeType = freeBeeType;
	}
	public int getFreeBeeValue() {
		return freeBeeValue;
	}
	public void setFreeBeeValue(int freeBeeValue) {
		this.freeBeeValue = freeBeeValue;
	}
	public int getFreebeeValidity() {
		return freebeeValidity;
	}
	public void setFreebeeValidity(int freebeeValidity) {
		this.freebeeValidity = freebeeValidity;
	}
	public int getFreebeeId() {
		return freebeeId;
	}
	public void setFreebeeId(int freebeeId) {
		this.freebeeId = freebeeId;
	}
	
	public FreeBeeDet()
	{
		
	}
	
	public FreeBeeDet(int freeBeeType, int freeBeeValue, int freebeeValidity) {
		super();
		this.freeBeeType = freeBeeType;
		this.freeBeeValue = freeBeeValue;
		this.freebeeValidity = freebeeValidity;
	}
	
	public FreeBeeDet(int freeBeeType, int freeBeeValue, int freebeeValidity, int freebeeId) {
		super();
		this.freeBeeType = freeBeeType;
		this.freeBeeValue = freeBeeValue;
		this.freebeeValidity = freebeeValidity;
		this.freebeeId = freebeeId;
	}
	@Override
	public String toString() {
		return "FreeBeeDet [freeBeeType=" + freeBeeType + ", freeBeeValue=" + freeBeeValue + ", freebeeValidity="
				+ freebeeValidity + ", freebeeId=" + freebeeId + "]";
	}
	
	
}
