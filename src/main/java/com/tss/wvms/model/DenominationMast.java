package com.tss.wvms.model;

public class DenominationMast {

	//SELECT SLAB_ID,FREEBEE_ID,COS_ID,MODE_TYPE,DENOMINATION_VALIDITY,
	//VALIDITY_TYPE,NVL(AMOUNT,0),ACCESS_TYPE FROM DENOMINATION_MAST WHERE DENOMINATION_ID = ?
	private int slabId;
	private int amount;
	private int accessType;
	private String denominationDescription;
	private int cardType;
	private int denominationValidity;
	private int validationType;
	private int denominationId;
	private int voucherQuantity;
	private int freeBeeId;
	private int cosId;
	private int modeType;
	
	
	public int getSlabId() {
		return slabId;
	}
	public void setSlabId(int slabId) {
		this.slabId = slabId;
	}
	public int getAmount() {
		return amount;
	}
	public void setAmount(int amount) {
		this.amount = amount;
	}
	public int getAccessType() {
		return accessType;
	}
	public void setAccessType(int accessType) {
		this.accessType = accessType;
	}
	public String getDenominationDescription() {
		return denominationDescription;
	}
	public void setDenominationDescription(String denominationDescription) {
		this.denominationDescription = denominationDescription;
	}
	public int getCardType() {
		return cardType;
	}
	public void setCardType(int cardType) {
		this.cardType = cardType;
	}
	public int getDenominationValidity() {
		return denominationValidity;
	}
	public void setDenominationValidity(int denominationValidity) {
		this.denominationValidity = denominationValidity;
	}
	public int getValidationType() {
		return validationType;
	}
	public void setValidationType(int validationType) {
		this.validationType = validationType;
	}
	public int getDenominationId() {
		return denominationId;
	}
	public void setDenominationId(int denominationId) {
		this.denominationId = denominationId;
	}
	public int getVoucherQuantity()
	{
		return voucherQuantity;
	}
	public void setVoucherQuantity(int voucherQuantity)
	{
		this.voucherQuantity = voucherQuantity;
	}
	
	public int getFreeBeeId() {
		return freeBeeId;
	}
	public void setFreeBeeId(int freeBeeId) {
		this.freeBeeId = freeBeeId;
	}
	public int getCosId() {
		return cosId;
	}
	public void setCosId(int cosId) {
		this.cosId = cosId;
	}
	public int getModeType() {
		return modeType;
	}
	public void setModeType(int modeType) {
		this.modeType = modeType;
	}
	
	public DenominationMast()
	{
		
	}
	
	public DenominationMast(int slabId, int amount, int accessType, String denominationDescription, int cardType,
			int denominationValidity, int validationType) {
		super();
		this.slabId = slabId;
		this.amount = amount;
		this.accessType = accessType;
		this.denominationDescription = denominationDescription;
		this.cardType = cardType;
		this.denominationValidity = denominationValidity;
		this.validationType = validationType;
	}
	
	public DenominationMast(int slabId,int freeBeeId,int cosId,int modeType, int denominationValidity, int validationType,int amount,
			 int accessType) {
	
		this.slabId = slabId;
		this.freeBeeId = freeBeeId;
		this.cosId = cosId;
		this.modeType = modeType;
		this.denominationValidity = denominationValidity;
		this.validationType = validationType;
		this.amount = amount;
		this.accessType = accessType;
		
		
	}
	
	public DenominationMast(int amount,int voucherQuantity)
	{
		   this.amount=amount;
		   this.voucherQuantity=voucherQuantity;
	}
	@Override
	public String toString() {
		return "DenominationMast [slabId=" + slabId + ", amount=" + amount + ", accessType=" + accessType
				+ ", denominationDescription=" + denominationDescription + ", cardType=" + cardType
				+ ", denominationValidity=" + denominationValidity + ", validationType=" + validationType
				+ ", denominationId=" + denominationId + ", voucherQuantity=" + voucherQuantity + ", freeBeeId="
				+ freeBeeId + ", cosId=" + cosId + ", modeType=" + modeType + "]";
	}
	
	
	
}
