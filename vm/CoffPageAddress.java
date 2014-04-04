package nachos.vm;

public class CoffPageAddress {
	
	private int sectionNumber;
	private int pageOffset;
	
	public CoffPageAddress(int sectionNumber,int pageOffset){
		this.sectionNumber=sectionNumber;
		this.pageOffset=pageOffset;
	}
	
	public int getSectionNumber(){
		return sectionNumber;
	}
	
	public int getPageOffset(){
		return pageOffset;
	}
	
	public boolean equals(CoffPageAddress that){
		if(that==null)return false;
		return this.sectionNumber==that.sectionNumber&&this.pageOffset==that.pageOffset;
	}
	
	@Override
	public int hashCode(){
		return (new Integer(sectionNumber).toString()+new Integer(pageOffset).toString()).hashCode();
	}
	
	

}
