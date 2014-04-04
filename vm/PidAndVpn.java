package nachos.vm;

public class PidAndVpn {
	
	private int pID;
	private int vpn;
	
	public PidAndVpn(int pID,int vpn){
		this.pID=pID;
		this.vpn=vpn;
	}
	
	@Override
	public boolean equals(Object that){
		if(that==null)return false;
		return this.pID==((PidAndVpn)that).pID&&this.vpn==((PidAndVpn)that).vpn;
	}
	
	@Override
	public int hashCode(){
		return (new Integer(pID).toString()+new Integer(vpn).toString()).hashCode();
	}

}
