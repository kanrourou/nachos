package nachos.vm;

import nachos.machine.TranslationEntry;

public class TranslationEntryWithPid {
	
	public TranslationEntry translationEntry;
	public int pID;
	
	public TranslationEntryWithPid(int pID,TranslationEntry translationEntry){
		this.translationEntry=translationEntry;
		this.pID=pID;
	}

}
