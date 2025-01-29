package com.tss.wvms.model;

public class RechargeMast{

	private int slabAmount;
	private int slabValidity;
	public int getSlabAmount() {
		return slabAmount;
	}
	public void setSlabAmount(int slabAmount) {
		this.slabAmount = slabAmount;
	}
	public int getSlabValidity() {
		return slabValidity;
	}
	public void setSlabValidity(int slabValidity) {
		this.slabValidity = slabValidity;
	}
	
	public RechargeMast(int slabAmount, int slabValidity) {
		super();
		this.slabAmount = slabAmount;
		this.slabValidity = slabValidity;
	}
	
	public RechargeMast() {
		
	}
	
	@Override
	public String toString() {
		return "RechargeMast [slabAmount=" + slabAmount + ", slabValidity=" + slabValidity + "]";
	}
	
	
	
	
	
}
