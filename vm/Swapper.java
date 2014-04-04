package nachos.vm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.OpenFile;
import nachos.threads.ThreadedKernel;

public class Swapper {

	private int pageSize;
	
	private OpenFile swapFile;
	
	private String swapFileName;
	
	private HashMap<PidAndVpn,Integer> swapTable;
	
	private HashSet<PidAndVpn> unallocated;
	
	private LinkedList<Integer> availableLocations;
	
	private static Swapper instance=null;
	
	protected final static char dbgVM='v';

	private Swapper(String swapFileName){
		pageSize=Machine.processor().pageSize;
		this.swapFileName=swapFileName;
		swapTable=new HashMap<PidAndVpn,Integer>();
		unallocated=new HashSet<PidAndVpn>();
		availableLocations=new LinkedList<Integer>();
		swapFile=ThreadedKernel.fileSystem.open(swapFileName, true);
		if(swapFile==null){
			Lib.debug(dbgVM, "constructor:fail to open swapfile");
		}
	}

	public static Swapper getInstance(String swapFileName){
		if(instance==null){
			instance=new Swapper(swapFileName);
		}
		return instance;
	}

	public void insertUnallocatedPage(int pid,int vpn){
		PidAndVpn key=new PidAndVpn(pid,vpn);
		unallocated.add(key);
	}

	public int allocatePosition(int pid,int vpn){
		PidAndVpn key=new PidAndVpn(pid,vpn);
		if(unallocated.contains(key)){
			unallocated.remove(key);
			if(availableLocations.isEmpty()){
				availableLocations.add(swapTable.size());
			}
			int index=availableLocations.removeFirst();
			swapTable.put(key, index);
			return index;
		}else{
			int index=-1;
			index=swapTable.get(key);
			if(index==-1){
				Lib.debug(dbgVM, "allocatePosition:unallocated is inconsistent with swapTable");
			}
			return index;
		}
	}
	
	public void deletePosition(int pid,int vpn){
		PidAndVpn key=new PidAndVpn(pid,vpn);
		if(!swapTable.containsKey(key))return;
		int availableLocation=swapTable.remove(key);
		availableLocations.add(availableLocation);
	}
	
	public byte[] readFromSwapFile(int pid,int vpn){
		int position=findEntry(pid,vpn);
		if(position==-1){
			Lib.debug(dbgVM, "readFromSwapFile:key doesn't exist in swapTable");
			return new byte[pageSize];
		}
		byte[] reader=new byte[pageSize];
		int length=swapFile.read(position*pageSize, reader, 0, pageSize);
		if(length==-1){
			Lib.debug(dbgVM, "readFromSwapFile:fail to read swapfile");
			return new byte[pageSize];
		}
		return reader;
	}
	
	private int findEntry(int pid, int vpn) {
        Integer position = swapTable.get(new PidAndVpn(pid, vpn));
        if (position == null)
            return -1;
        else
            return position.intValue();
    }
	
	
	public int writeToSwapFile(int pid,int vpn,byte[] page,int offset){
		int position=allocatePosition(pid,vpn);
		if(position==-1){
			Lib.debug(dbgVM, "writeToSwapFile:fail to allocate position");
			return -1;
		}
		swapFile.write(position*pageSize, page, offset, pageSize);
		return position;
	}
	
	public void removeSwapFile(){
		if(swapFile!=null){
			swapFile.close();
			ThreadedKernel.fileSystem.remove(swapFileName);
			swapFile=null;
		}
		return;
	}


}
