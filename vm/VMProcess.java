package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.threads.Lock;

import java.util.*;
import java.io.EOFException;

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
	/**
	 * Allocate a new process.
	 */

	public VMProcess() {
		super();
		lazyLoadPages=new HashMap<Integer,CoffPageAddress>();
		allocatedPages=new LinkedList<Integer>();
		tlbBackUp=new TranslationEntry[Machine.processor().getTLBSize()];
		for(int i=0;i<tlbBackUp.length;i++){
			tlbBackUp[i]=new TranslationEntry(0,0,false,false,false,false);
		}

	}

	protected TranslationEntry lookUpPageTable(int vpn) {
		return PageTable.getInstance().getEntry(pID ,vpn);
	}

	protected String getSwapFileName(){
		return "Swap";
	}


	protected void lazyLoadPage(int vpn,int ppn){

		CoffPageAddress coffPageAddress=lazyLoadPages.remove(vpn);
		if(coffPageAddress==null){
			Lib.debug(dbgVM, "\tlazyLoadPage:failed to find page in lazyloadsection");
			return;
		}
		Lib.debug(dbgVM, "\tlazyLoadPage:load virtual page "+vpn+" on physical page "+ppn);
		CoffSection section=coff.getSection(coffPageAddress.getSectionNumber());
		section.loadPage(coffPageAddress.getPageOffset(), ppn);

	}

	protected int getFreePage() {
		int ppn = VMKernel.newPage();

		if (ppn == -1) {
			TranslationEntryWithPid victim = PageTable.getInstance().getVictim();

			ppn = victim.translationEntry.ppn;
			swapOut(victim.pID, victim.translationEntry.vpn);
		}

		return ppn;
	}

	protected void swapOut(int pID,int vpn){
		//		PidAndVpn key=new PidAndVpn(pID,vpn);
		TranslationEntry entry=PageTable.getInstance().getEntry(pID, vpn);
		if(entry==null){
			Lib.debug(dbgVM, "\tswapOut:failed to find entry in pagetable");
			return;
		}
		if(!entry.valid){
			Lib.debug(dbgVM, "\tswapOut:page doesn't exist in memory");
			return;
		}

		Lib.debug(dbgVM, "\tswapOut:swapping out virtualpage "+vpn+
				" of pID "+pID+" on physicalpage "+entry.ppn);

		for(int i=0;i<Machine.processor().getTLBSize();i++){
			TranslationEntry tlbEntry=Machine.processor().readTLBEntry(i);
			if(tlbEntry.vpn==entry.vpn&&tlbEntry.ppn==entry.ppn&&tlbEntry.valid){
				PageTable.getInstance().updateEntry(pID, tlbEntry);
				entry=PageTable.getInstance().getEntry(pID, vpn);
				tlbEntry.valid=false;
				Machine.processor().writeTLBEntry(i, tlbEntry);
				break;
			}
		}
		if(entry.dirty){
			byte[] memory=Machine.processor().getMemory();
			Swapper.getInstance(getSwapFileName()).writeToSwapFile(pID, vpn, memory,entry.ppn*pageSize);
		}
	}

	protected void swapIn(int ppn,int vpn){
		TranslationEntry entry=PageTable.getInstance().getEntry(pID, vpn);
		if(entry==null){
			Lib.debug(dbgVM, "\tswapIn:failed to find entry in pagetable");
			return;
		}
		if(entry.valid){
			Lib.debug(dbgVM, "\tswapIn:page is valid");
			return;
		}

		Lib.debug(dbgVM, "\tswapIn:swapping in virtualpage "+vpn+
				" of pID "+pID+" on physicalpage "+ppn);

		boolean dirty,used;
		//should set dirty when first load
		//		System.out.println(lazyLoadPages.containsKey(vpn));
		if(lazyLoadPages.containsKey(vpn)){
			lazyLoadPage(vpn,ppn);
			dirty=true;
			used=true;
		}else{
			byte[] memory=Machine.processor().getMemory();
			byte[] page=Swapper.getInstance(getSwapFileName()).readFromSwapFile(pID, vpn);
			System.arraycopy(page, 0, memory, ppn*pageSize, pageSize);
			dirty=false;
			used=false;
		}
		TranslationEntry newEntry=new TranslationEntry(vpn,ppn,true,false,used,dirty);
		PageTable.getInstance().setEntry(pID, newEntry);

	}

	public int readVirtualMemory(int vaddr,byte[] data,int offset,int length){
		pageLock.acquire();

		int vpn=Machine.processor().pageFromAddress(vaddr);
		TranslationEntry entry=PageTable.getInstance().getEntry(pID, vpn);
		if(entry==null){
			Lib.debug(dbgVM, "\treadVirtualMemory:failed to find entry in pagetable");
		}
		if(!entry.valid){
			//			Lib.debug(dbgVM, "\treadVirtualMemory:entry is invalid");
			int ppn=getFreePage();
			swapIn(ppn,vpn);
		}
		entry.used=true;
		PageTable.getInstance().setEntry(pID, entry);

		pageLock.release();

		return super.readVirtualMemory(vaddr, data, offset, length);
	}

	public int writeVirtualMemory(int vaddr,byte[] data,int offset,int length){
		pageLock.acquire();

		int vpn=Machine.processor().pageFromAddress(vaddr);
		swap(VMKernel.getVirtualPageNumber(vaddr));
		TranslationEntry entry = translate(vaddr);
		if(entry==null){
			Lib.debug(dbgVM, "\treadVirtualMemory:failed to find entry in pagetable");
		}
		if(!entry.valid){
			Lib.debug(dbgVM, "\treadVirtualMemory:entry is invalid");
		}
		entry.dirty=true;
		entry.used=true;
		PageTable.getInstance().setEntry(pID, entry);

		pageLock.release();

		return super.writeVirtualMemory(vaddr, data, offset, length);
	}


	protected boolean allocate(int vpn, int desiredPages, boolean readOnly) {

		for (int i = 0; i < desiredPages; ++i) {
			PageTable.getInstance().insertEntry(pID, new TranslationEntry(vpn + i, 0, false, readOnly, false, false));
			Swapper.getInstance(getSwapFileName()).insertUnallocatedPage(pID, vpn + i);
			allocatedPages.add(vpn + i);
		}

		numPages += desiredPages;

		return true;
	}

	protected int getTLBVictim(){
		for(int i=0;i<Machine.processor().getTLBSize();i++){
			if(!Machine.processor().readTLBEntry(i).valid){
				return i;
			}
		}
		return Lib.random(Machine.processor().getTLBSize());
	}


	protected void replaceTLBEntry(int index,TranslationEntry newEntry){
		TranslationEntry oldEntry=Machine.processor().readTLBEntry(index);
		if(oldEntry.valid){
			PageTable.getInstance().updateEntry(pID, oldEntry);
		}
		Lib.debug(dbgVM, "\treplaceTLBEntry:replacing entry "+index+" of vpn "
				+oldEntry.vpn+" and ppn "+oldEntry.ppn+" by vpn "+newEntry.vpn+
				" and ppn "+newEntry.ppn);
		Machine.processor().writeTLBEntry(index, newEntry);
	}


	protected boolean handleTLBFault(int vaddr){
		Lib.debug(dbgVM, "\thandleTLBFault:TLB fault");
		int vpn=Machine.processor().pageFromAddress(vaddr);
		TranslationEntry entry = translate(vaddr);
		if(entry==null){
			Lib.debug(dbgVM, "\thandleTLBFault:failed to find page in pagetable");
			return false;
		}
		if(!entry.valid){
			Lib.debug(dbgVM, "\thandleTLBFault:page fault");
			int ppn=getFreePage();
			swapIn(ppn,vpn);
			entry = translate(vaddr);
		}
		int victim=getTLBVictim();
		replaceTLBEntry(victim,entry);
		return true;
	}

	protected int swap(int vpn) {
		TranslationEntry entry = lookUpPageTable(vpn);
		//	        Lib.assertTrue(entry != null, "page " + vpn + " not in PageTable");

		if (entry.valid)
			return entry.ppn;

		int ppn = getFreePage();
		swapIn(ppn, vpn);

		return ppn;
	}


	protected void releaseResource() {
		for (int vpn: allocatedPages) {
			pageLock.acquire();

			TranslationEntry entry = PageTable.getInstance().deleteEntry(pID, vpn);
			if (entry.valid)
				VMKernel.deletePage(entry.ppn);

			Swapper.getInstance(getSwapFileName()).deletePosition(pID, vpn);

			pageLock.release();
		}
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */

	public void saveState() {
		//		super.saveState();
		Lib.debug(dbgVM, "\tsaveState:process "+pID+" save state for context switch");
		for(int i=0;i<Machine.processor().getTLBSize();i++){
			tlbBackUp[i]=Machine.processor().readTLBEntry(i);
			if(tlbBackUp[i].valid){
				PageTable.getInstance().updateEntry(pID, tlbBackUp[i]);
			}
		}
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		//		super.restoreState();
		Lib.debug(dbgVM, "\trestoreState:process "+pID+" restore state for context switch");
		for(int i=0;i<tlbBackUp.length;i++){
			if(tlbBackUp[i].valid){
				Machine.processor().writeTLBEntry(i, tlbBackUp[i]);
				//can be swapped out by other processes
				TranslationEntry entry=PageTable.getInstance().getEntry(pID, tlbBackUp[i].vpn);
				if(entry!=null&&entry.valid){
					Machine.processor().writeTLBEntry(i, entry);
				}else{
					Machine.processor().writeTLBEntry(i, new TranslationEntry(0,0,false,false,false,false));
				}
			}else{
				Machine.processor().writeTLBEntry(i, new TranslationEntry(0,0,false,false,false,false));
			}
		}
	}


	/**
	 * Initializes page tables for this process so that the executable can be
	 * demand-paged.
	 * 
	 * @return <tt>true</tt> if successful.
	 */

	protected boolean loadSections() {
//		UserKernel.availPagesLock.acquire();
//		//allocate physical pages from free pages list
//		for (int i=0; i<numPages; i++){
//			TranslationEntry entry= new TranslationEntry(i,0,false,false,false,false);
//
//			PageTable.getInstance().insertEntry(pID, entry);
//			Swapper.getInstance(getSwapFileName()).insertUnallocatedPage(pID, i);
//			allocatedPages.add(i);
//		}
//		UserKernel.availPagesLock.release();
		//return super.loadSections();
		for(int s=0;s<coff.getNumSections();s++){
			CoffSection section=coff.getSection(s);

			Lib.debug(dbgVM, "\tloadSection:loading "+section.getName()+
					" section("+section.getLength()+" pages) to lazyloadsection");

			for(int i=0;i<section.getLength();i++){
				int vpn=section.getFirstVPN()+i;
				CoffPageAddress coffPageAddress=new CoffPageAddress(s,i);
				lazyLoadPages.put(vpn, coffPageAddress);

			}
		}
		return true;
	}


	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
		super.unloadSections();
	}

	protected void acquireLock(String msg) {
		pageLock.acquire();
	}

	protected void releaseLock() {
		pageLock.release();
	}

	/**
	 * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>
	 * . The <i>cause</i> argument identifies which exception occurred; see the
	 * <tt>Processor.exceptionZZZ</tt> constants.
	 * 
	 * @param cause
	 *            the user exception that occurred.
	 */
	
	public void handleException(int cause) {
		Processor processor = Machine.processor();

		switch (cause) {
		case Processor.exceptionTLBMiss:
			int address=processor.readRegister(processor.regBadVAddr);
			int vpn=Machine.processor().pageFromAddress(address);
			Lib.debug(dbgVM, "\thandleException:TLB miss exception:address "+address+" virtual page "+vpn);
			pageLock.acquire();
			boolean isSuccessful=handleTLBFault(address);
			if(isSuccessful){
				Lib.debug(dbgVM, "\thandleException:TLB miss handled sucessfully");
			}else{
				UThread.finish();
			}
			pageLock.release();
			break;
		default:
			super.handleException(cause);
			break;
		}
	}

	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';

	private static final char dbgVM = 'v';

	protected HashMap<Integer,CoffPageAddress> lazyLoadPages;

	protected LinkedList<Integer> allocatedPages;

	protected TranslationEntry[] tlbBackUp;

	protected static Lock pageLock=new Lock();

}
