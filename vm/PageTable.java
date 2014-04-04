package nachos.vm;

import java.util.HashMap;

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.TranslationEntry;

public class PageTable {

	private HashMap<PidAndVpn,TranslationEntry> table;
	
	private TranslationEntryWithPid[] globalTable;
	
	private static PageTable instance=null;
	
	protected static final char dbgVM='v';

	private PageTable(){
		table=new HashMap<PidAndVpn,TranslationEntry>();
		globalTable=new TranslationEntryWithPid[Machine.processor().getNumPhysPages()];
	}

	public static PageTable getInstance(){
		if(instance==null)instance=new PageTable();
		return instance;
	}

	public boolean insertEntry(int pID,TranslationEntry entry){
		PidAndVpn key=new PidAndVpn(pID,entry.vpn);
		if(table.containsKey(key)){
			Lib.debug(dbgVM, "\tinsertEntry:duplicated key");
			return false;
		}
		Lib.debug(dbgVM, "\tinsertEntry:process "+pID+" insert entry of virtual page "+entry.vpn+
				" and physical page "+entry.ppn+" to pageTable ");
		table.put(key, entry);
//		System.out.println(key.equals(new PidAndVpn(0,1)));
		//System.out.println(table.containsKey(new PidAndVpn(0,1)));
		if(entry.valid){
			globalTable[entry.ppn]=new TranslationEntryWithPid(pID,entry);
			Lib.debug(dbgVM, "\tinsertEntry:insert globalTable["+entry.ppn+"]");
		}
		return true;

	}

	public TranslationEntry deleteEntry(int pID,int vpn){
		PidAndVpn key=new PidAndVpn(pID,vpn);
		TranslationEntry entry=table.get(key);
		if(entry==null){
			Lib.debug(dbgVM, "\tdeleteEntry:entry doesn't exist in table");
			return null;
		}
		if(entry.valid){
			globalTable[entry.ppn]=null;
			Lib.debug(dbgVM, "\tdeleteEntry:delete globalTable["+entry.ppn+"]");
		}
		return entry;

	}

	public void setEntry(int pID,TranslationEntry newEntry){
		PidAndVpn key=new PidAndVpn(pID,newEntry.vpn);
		if(!table.containsKey(key)){
			Lib.debug(dbgVM, "\tsetEntry:entry doesn't exist in table");
			return;
		}
		TranslationEntry oldEntry=table.get(key);
		if(oldEntry.valid){
			if(globalTable[oldEntry.ppn]==null){
				Lib.debug(dbgVM, "\tsetEntry:inconsistent globalTable");
				return;
			}
			globalTable[oldEntry.ppn]=null;
			Lib.debug(dbgVM, "\tsetEntry:delete globalTable["+oldEntry.ppn+"]");
		}
		if(newEntry.valid){
			if(globalTable[newEntry.ppn]!=null){
				Lib.debug(dbgVM, "\tsetEntry:inconsistent globalTable");
				return;
			}
			globalTable[newEntry.ppn]=new TranslationEntryWithPid(pID,newEntry);
			Lib.debug(dbgVM, "\tsetEntry:insert globalTable["+newEntry.ppn+"]");		
		}
		table.put(key, newEntry);

	}

	public TranslationEntry getEntry(int pID,int vpn){
		PidAndVpn key=new PidAndVpn(pID,vpn);
		TranslationEntry entry=null;
		Lib.debug(dbgVM, "\tgetEntry:process "+pID+" get entry of virtual page "+vpn+
				" from pageTable ");
		if(table.containsKey(key)){
			entry=table.get(key);
		}
		return entry;
	}
	
	public void updateEntry(int pID,TranslationEntry entry){
		PidAndVpn key=new PidAndVpn(pID,entry.vpn);
		if(table.containsKey(key)){
			Lib.debug(dbgVM, "\tupdateEntry:entry already exist in table");
			return;
		}
		TranslationEntry oldEntry=table.get(key);
		TranslationEntry newEntry=mix(entry,oldEntry);
		if(oldEntry.valid){
			if(globalTable[oldEntry.ppn]==null){
				Lib.debug(dbgVM, "\tupdateEntry:inconsistent globalTable");
				return;
			}
			globalTable[oldEntry.ppn]=null;
			Lib.debug(dbgVM, "\tupdateEntry:delete globalTable["+oldEntry.ppn+"]");
		}
		if(newEntry.valid){
			if(globalTable[newEntry.ppn]!=null){
				Lib.debug(dbgVM, "\tupdateEntry:inconsistent globalTable");
				return;
			}
			globalTable[newEntry.ppn]=new TranslationEntryWithPid(pID,newEntry);
			Lib.debug(dbgVM, "\tupdateEntry:insert globalTable["+oldEntry.ppn+"]");		
		}
		table.put(key, newEntry);
	}
	
	private TranslationEntry mix(TranslationEntry entry1,TranslationEntry entry2){
		TranslationEntry mixture=entry1;
		if(entry1.dirty||entry2.dirty){
			mixture.dirty=true;
		}
		if(entry1.used||entry1.used){
			mixture.used=true;
		}
		return mixture;
	}

	public TranslationEntryWithPid getVictim(){
		TranslationEntryWithPid entry=null;
		do{
			int index=Lib.random(globalTable.length);
			entry=globalTable[index];
		}while(entry==null||!entry.translationEntry.valid);
		return entry;
	}

}
