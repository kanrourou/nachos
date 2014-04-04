package nachos.threads;

import java.util.*;

import nachos.machine.*;
import nachos.threads.PriorityScheduler.ThreadState;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A scheduler that chooses threads using a lottery.
 * 
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 * 
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 * 
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking the
 * maximum).
 */
public class LotteryScheduler extends PriorityScheduler  {
	/**
	 * Allocate a new lottery scheduler.
	 */
	public LotteryScheduler() {
		super();
		

	}
	public static void selfTest() {
		System.out.println("---------LotteryScheduler test---------------------");
		LotteryScheduler s = new LotteryScheduler();
		ThreadQueue queue = s.newThreadQueue(true);
		ThreadQueue queue2 = s.newThreadQueue(true);
		ThreadQueue queue3 = s.newThreadQueue(true);

		KThread thread1 = new KThread();
		KThread thread2 = new KThread();
		KThread thread3 = new KThread();
		KThread thread4 = new KThread();
		KThread thread5 = new KThread();
		thread1.setName("thread1");
		thread2.setName("thread2");
		thread3.setName("thread3");
		thread4.setName("thread4");
		thread5.setName("thread5");


		boolean intStatus = Machine.interrupt().disable();

		queue.acquire(thread1);
		queue.waitForAccess(thread2);
		queue.waitForAccess(thread3);
		System.out.println("thread1 EP="+s.getThreadState(thread1).getEffectivePriority());
		System.out.println("thread2 EP="+s.getThreadState(thread2).getEffectivePriority());
		System.out.println("thread3 EP="+s.getThreadState(thread3).getEffectivePriority());
		System.out.println("~~~~~~~~Thread4 aquires queue2 thread1 waits~~~~~~~~~`");
		queue2.acquire(thread4);
		queue2.waitForAccess(thread1);
		System.out.println("thread4 EP="+s.getThreadState(thread4).getEffectivePriority());
		System.out.println("thread1 EP="+s.getThreadState(thread1).getEffectivePriority());
		System.out.println("~~~~~~~~thread2 priority changed to 2~~~~~~~~~`");
		s.getThreadState(thread2).setPriority(2);

		System.out.println("thread4 EP="+s.getThreadState(thread4).getEffectivePriority());
		System.out.println("thread1 EP="+s.getThreadState(thread1).getEffectivePriority());
		System.out.println("~~~~~~~~thread2 priority changed to 1~~~~~~~~~`");
		s.getThreadState(thread2).setPriority(1);
		System.out.println("thread4 EP="+s.getThreadState(thread4).getEffectivePriority());
		System.out.println("thread1 EP="+s.getThreadState(thread1).getEffectivePriority());
		System.out.println("~~~~~~~~Thread5 waits on queue1~~~~~~~~~`");
		queue.waitForAccess(thread5);

		System.out.println("thread1 EP="+s.getThreadState(thread1).getEffectivePriority());

		System.out.println("thread4 EP="+s.getThreadState(thread4).getEffectivePriority());
		System.out.println("~~~~~~~~thread2 priority changed to 8~~~~~~~~~`");
		s.getThreadState(thread2).setPriority(8);
		System.out.println("thread4 EP="+s.getThreadState(thread4).getEffectivePriority());
		System.out.println("thread1 EP="+s.getThreadState(thread1).getEffectivePriority());
		ThreadQueue newQueue;

		KThread thread10;
		int tot10 = 0;
		KThread thread20;
		int tot20 = 0;
		for (int i =0; i<999; i++){
			newQueue = s.newThreadQueue(true);
			thread10 = new KThread();
			thread20 = new KThread();
			newQueue.waitForAccess(thread10);
			newQueue.waitForAccess(thread20);
			if (newQueue.nextThread() == thread10)
				tot10 += 1;
			else
				tot20+=1;	
		}

		System.out.println("thread1 Total = " + tot10);
		System.out.println("thread2 Total = " + tot20);
		/*
		queue3.acquire(thread5);
		queue3.waitForAccess(thread4);
		System.out.println("thread5 EP="+s.getThreadState(thread5).getEffectivePriority());
		System.out.println("thread4 EP="+s.getThreadState(thread4).getEffectivePriority());

		queue2.nextThread();
		System.out.println("thread4 EP="+s.getThreadState(thread4).getEffectivePriority());
		System.out.println("thread1 EP="+s.getThreadState(thread1).getEffectivePriority());
		System.out.println("thread5 EP="+s.getThreadState(thread5).getEffectivePriority());
		 */
	}


	/**
	 * Allocate a new lottery thread queue.
	 * 
	 * @param transferPriority <tt>true</tt> if this queue should transfer
	 * tickets from waiting threads to the owning thread.
	 * @return a new lottery thread queue.
	 */
/*
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		// implement me
		return null;
	}
*/	

	/**
	 * Allocate a new priority thread queue.
	 * 
	 * @param transferPriority <tt>true</tt> if this queue should transfer
	 * priority from waiting threads to the owning thread.
	 * @return a new priority thread queue.
	 */


    public ThreadQueue newThreadQueue(boolean transferPriority) {
        return new LotteryQueue(transferPriority);
    }
        
	/**
	 * The default priority for a new thread. Do not change this value.
	 */
	public static final int priorityDefault = 1;

	/**
	 * The minimum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMinimum = 0;

	/**
	 * The maximum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMaximum = Integer.MAX_VALUE;

	/**
	 * Return the scheduling state of the specified thread.
	 * 
	 * @param thread the thread whose scheduling state to return.
	 * @return the scheduling state of the specified thread.
	 */
/*
	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);

		return (ThreadState) thread.schedulingState;
		
	}
	*/
	protected LotteryThreadState getLotteryThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new LotteryThreadState(thread);

		return (LotteryThreadState) thread.schedulingState;
		
	}

	/**
	 * A <tt>ThreadQueue</tt> that sorts threads by priority.
	 */
	protected class LotteryQueue extends ThreadQueue {
		
		LotteryQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
		}

		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getLotteryThreadState(thread).waitForAccess(this);
		}

		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getLotteryThreadState(thread).acquire(this);
		}


		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me
			int totaltickets = 0;
			
			Iterator<ThreadState> queue = LotteryPQueue.iterator();
			while ( queue.hasNext())
			{
				int tickes = queue.next().effectivePriority;
				totaltickets += tickes;
			}
			
			Random generator = new Random();
			Integer ticketChoice = 0;
			
			if(totaltickets != 0)
			{
				 ticketChoice = generator.nextInt(totaltickets);
			}
		
			
			int TickSubSum = 0;
			
			KThread thread =null;
			for(ThreadState ThState : LotteryPQueue)
			{
				TickSubSum += ThState.effectivePriority;
				if(ticketChoice < TickSubSum)
				{
					thread = ThState.thread;
					LotteryPQueue.remove(ThState);
					break;
				}
			}

			return thread;
		}
		/**
		 * Return the next thread that <tt>nextThread()</tt> would return,
		 * without modifying the state of this queue.
		 * 
		 * @return the next thread that <tt>nextThread()</tt> would return.
		 */
		protected ThreadState pickNextThread() {
			// implement me
			//return null;
			//return this.LotteryPQueue.peek();
			return null;
		}

		public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me (if you want)
		}

		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting
		 * threads to the owning thread.
		 */
		public boolean transferPriority;
		
		protected LotteryThreadState LotteryresourceHolder=null;
		
		protected LinkedList<ThreadState> LotteryPQueue = new LinkedList<ThreadState>();
		
		
	}

	/**
	 * The scheduling state of a thread. This should include the thread's
	 * priority, its effective priority, any objects it owns, and the queue it's
	 * waiting for, if any.
	 * 
	 * @see nachos.threads.KThread#schedulingState
	 */

	//protected class ThreadState implements Comparable<ThreadState>{
	protected class LotteryThreadState extends ThreadState{
	/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 * 
		 * @param thread the thread this state belongs to.
		 */
        public LotteryThreadState(KThread thread) {
            this.thread = thread;

    }

		/**
		 * Return the priority of the associated thread.
		 * 
		 * @return the priority of the associated thread.
		 */
		public int getPriority() {
			return priority;
		}

		/**
		 * Return the effective priority of the associated thread.
		 * 
		 * @return the effective priority of the associated thread.
		 */
		public int getEffectivePriority() {
			
			return this.effectivePriority;
		}
	
		/**
		 * Set the priority of the associated thread to the specified value.
		 * 
		 * @param priority the new priority.
		 */
		public void setPriority(int priority) {
			if (this.priority == priority)
				return;

			this.priority = priority;
			this.effectivePriority = this.priority;
			// implement me
			LotteryCorrectPriority();
			
		}
		
		private void LotteryCorrectPriority() {
			if(this.LotteryHoldingQueues !=null)
			{
				if(!this.LotteryHoldingQueues.isEmpty())
				{
					Iterator<LotteryQueue> TPQueueNT =this.LotteryHoldingQueues.iterator();			
					while(TPQueueNT.hasNext())
					{
						LotteryThreadState HoldResource=TPQueueNT.next().LotteryresourceHolder;
						int size = HoldResource.LotteryWaitingTSQueue.size();
						int SumPriority =0;
						for(int i=0; i<size;i++)
						{
							SumPriority += HoldResource.LotteryWaitingTSQueue.get(i).effectivePriority;
						}
						HoldResource.effectivePriority = HoldResource.priority + SumPriority;
						
						HoldResource.LotteryCorrectPriority();
					}
				}
			}


		}

		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
		 * the associated thread) is invoked on the specified priority queue.
		 * The associated thread is therefore waiting for access to the resource
		 * guarded by <tt>waitQueue</tt>. This method is only called if the
		 * associated thread cannot immediately obtain access.
		 * 
		 * @param waitQueue the queue that the associated thread is now waiting
		 * on.
		 * 
		 * @see nachos.threads.ThreadQueue#waitForAccess
		 */
		public void waitForAccess(LotteryQueue waitQueue) {
			// implement me

			this.age = Machine.timer().getTime();
			waitQueue.LotteryPQueue.add(this);
			
			if(waitQueue.transferPriority==true)
			{
				LotteryHoldingQueues.add(waitQueue);
				if(waitQueue.LotteryresourceHolder != null)
				{
					waitQueue.LotteryresourceHolder.LotteryWaitingTSQueue.add(this);
					waitQueue.LotteryresourceHolder.effectivePriority +=this.effectivePriority;
					waitQueue.LotteryresourceHolder.LotteryCorrectPriority();
				}
				
			}

		}

		/**
		 * Called when the associated thread has acquired access to whatever is
		 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 * 
		 * @see nachos.threads.ThreadQueue#acquire
		 * @see nachos.threads.ThreadQueue#nextThread
		 */
		public void acquire(LotteryQueue waitQueue) {
			// implement me
			if(waitQueue.transferPriority)
			{
				waitQueue.LotteryresourceHolder = this;
			}		
			
		}

		/** The thread with which this object is associated. */
		protected KThread thread ;

		protected long age = Machine.timer().getTime();
		/** The priority of the associated thread. */
		protected int priority = priorityDefault;
		
		protected int effectivePriority =priorityDefault;
		
		protected LinkedList<LotteryQueue> LotteryHoldingQueues = new LinkedList<LotteryQueue>();

		protected LinkedList<LotteryThreadState> LotteryWaitingTSQueue = new LinkedList<LotteryThreadState>();		
	
		
	}
	
}
