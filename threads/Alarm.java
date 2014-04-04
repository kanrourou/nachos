package nachos.threads;

import java.util.PriorityQueue;

import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 * 
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */
	public Alarm() {
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() {
				timerInterrupt();
			}
		});
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread that
	 * should be run.
	 */
	public void timerInterrupt() {
		boolean inStatus=Machine.interrupt().disable();

		while(!waitQueue.isEmpty()&&waitQueue.peek().wakeTime<=Machine.timer().getTime()){
			waitQueue.poll().thread.ready();
		}
		KThread.yield();
		Machine.interrupt().restore(inStatus);
	}


	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
	 * in the timer interrupt handler. The thread must be woken up (placed in
	 * the scheduler ready set) during the first timer interrupt where
	 * 
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 * 
	 * @param x the minimum number of clock ticks to wait.
	 * 
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
		// for now, cheat just to get something working (busy waiting is bad)
		long wakeTime = Machine.timer().getTime() + x;
		KThread thread=KThread.currentThread();
		WaitingThread waitThread=new WaitingThread(thread,wakeTime);
		boolean inStatus=Machine.interrupt().disable();
		waitQueue.add(waitThread);
		KThread.sleep();
		Machine.interrupt().restore(inStatus);

	}

	private class WaitingThread implements Comparable<WaitingThread>{
		KThread thread;
		long wakeTime;

		public WaitingThread(KThread thread, long wakeTime){
			this.thread=thread;
			this.wakeTime=wakeTime;
		}

		public int compareTo(WaitingThread thread){
			if(this.wakeTime<thread.wakeTime)return -1;
			else if(this.wakeTime>thread.wakeTime)return 1;
			else return 0;

		}

	}
	private PriorityQueue<WaitingThread> waitQueue=new PriorityQueue<WaitingThread>();
}
