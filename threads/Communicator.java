package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>, and multiple
 * threads can be waiting to <i>listen</i>. But there should never be a time
 * when both a speaker and a listener are waiting, because the two threads can
 * be paired off at this point.
 */
public class Communicator {
	/**
	 * Allocate a new communicator.
	 */
	public Communicator() {
		lock=new Lock();
		listener=new Condition2(lock);
		speaker=new Condition2(lock);
		waitingReturn=new Condition2(lock);
		
		
	}

	/**
	 * Wait for a thread to listen through this communicator, and then transfer
	 * <i>word</i> to the listener.
	 * 
	 * <p>
	 * Does not return until this thread is paired up with a listening thread.
	 * Exactly one listener should receive <i>word</i>.
	 * 
	 * @param word the integer to transfer.
	 */
	public void speak(int word) {
		lock.acquire();
		while(sendingSpeaker!=0){
			waitingSpeaker++;
			speaker.sleep();
			waitingSpeaker--;
		}
		sendingSpeaker++;
		data=word;
		if(receivingListener!=0){
			waitingReturn.wake();
		}else{
			waitingReturn.sleep();
		}
		sendingSpeaker--;
		if(waitingSpeaker!=0){
			speaker.wake();
		}
		lock.release();
		return;		
	}

	/**
	 * Wait for a thread to speak through this communicator, and then return the
	 * <i>word</i> that thread passed to <tt>speak()</tt>.
	 * 
	 * @return the integer transferred.
	 */
	public int listen() {
        lock.acquire();
		while(receivingListener!=0){
			waitingListener++;
			listener.sleep();
			waitingListener--;
		}
		receivingListener++;
		if(sendingSpeaker!=0){
			waitingReturn.wake();
		}else{
			waitingReturn.sleep();
		}
		if(waitingListener!=0){
			listener.wake();
		}
		int word=data;
		receivingListener--;
		lock.release();
		return word;
	}
	
	private int waitingSpeaker=0;
	private int sendingSpeaker=0;
	private int waitingListener=0;
	private int receivingListener=0;
	private int data;
	private Lock lock;
	private Condition2 speaker;
	private Condition2 listener;
	private Condition2 waitingReturn;
}
