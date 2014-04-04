package nachos.threads;

import nachos.machine.*;
import nachos.threads.LotteryScheduler.LotteryQueue;
import nachos.threads.LotteryScheduler.LotteryThreadState;

import java.util.*;

/**
 * A scheduler that chooses threads based on their priorities.
 * 
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the thread
 * that has been waiting longest.
 * 
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has the
 * potential to starve a thread if there's always a thread waiting with higher
 * priority.
 * 
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
	/**
	 * Allocate a new priority scheduler.
	 */
	public PriorityScheduler() {
	}

	/**
	 * Allocate a new priority thread queue.
	 * 
	 * @param transferPriority <tt>true</tt> if this queue should transfer
	 * priority from waiting threads to the owning thread.
	 * @return a new priority thread queue.
	 */
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new ThreadPriorityQueue(transferPriority);
	}

	public int getPriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getPriority();
	}

	public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getEffectivePriority();
	}

	public void setPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());

		Lib.assertTrue(priority >= priorityMinimum
				&& priority <= priorityMaximum);

		getThreadState(thread).setPriority(priority);
	}

	public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable();
		boolean ret = true;

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMaximum)
			ret = false;
		else
			setPriority(thread, priority + 1);

		Machine.interrupt().restore(intStatus);
		return ret;
	}

	public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable();
		boolean ret = true;

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
			ret = false;
		else
			setPriority(thread, priority - 1);

		Machine.interrupt().restore(intStatus);
		return ret;
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
	public static final int priorityMaximum = 7;

	/**
	 * Return the scheduling state of the specified thread.
	 * 
	 * @param thread the thread whose scheduling state to return.
	 * @return the scheduling state of the specified thread.
	 */
	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);

		return (ThreadState) thread.schedulingState;
	}

	/**
	 * A <tt>ThreadQueue</tt> that sorts threads by priority.
	 */
	protected class ThreadPriorityQueue extends ThreadQueue {
		ThreadPriorityQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
		}

		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).waitForAccess(this);
		}

		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).acquire(this);
		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me
			ThreadState nextthreadstate = pickNextThread();
			if(nextthreadstate != null)
			{
				return nextthreadstate.thread;
			}
			else
				return null;
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
			/*
			if (PQueue.isEmpty())
				return null;
							
			return PQueue.poll();
			*/
			
			
			int max = -1;
			if(PQueue.isEmpty())
				return null;
			
			ThreadState threads = PQueue.peek();
			
			for(ThreadState ths: PQueue)
			{
				int eff =ths.getEffectivePriority();
				if(eff > max)
				{
					threads = ths;
					max = eff;
				}
				else if(eff == max)
				{
					if(ths.age >= threads.age)
					{
						threads = ths;
						max = eff;
					}

				}
			}
			
			PQueue.remove(threads);
			return threads;
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
		
		protected ThreadState resourceHolder=null;
		
		protected LinkedList<ThreadState> PQueue = new LinkedList<ThreadState>();
		
	}

	/**
	 * The scheduling state of a thread. This should include the thread's
	 * priority, its effective priority, any objects it owns, and the queue it's
	 * waiting for, if any.
	 * 
	 * @see nachos.threads.KThread#schedulingState
	 */
	protected class ThreadState  {
		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 * 
		 * @param thread the thread this state belongs to.
		 */
		public ThreadState()
		{
			
		}
		public ThreadState(KThread thread) {
			
			this.thread = thread;
			//age=Machine.timer().getTime();
			//priority=priorityDefault;
			//effectivePriority=priority;

			//setPriority(priorityDefault);
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
			// implement me
			//return priority;
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
			
			// implement me
			this.effectivePriority = this.priority;
			CorrectPriority();

		}
		
		private void CorrectPriority() {
			if(this.HoldingQueues != null)
			{
				if(!this.HoldingQueues.isEmpty())
				{
					Iterator<ThreadPriorityQueue> TPQueueNT =this.HoldingQueues.iterator();			
					while(TPQueueNT.hasNext())
					{
						ThreadState HoldResource=TPQueueNT.next().resourceHolder;
						
						int size = HoldResource.WaitingTSQueue.size();
						int max =0;
						for(int i=0; i<size;i++)
						{
							int eff = HoldResource.WaitingTSQueue.get(i).effectivePriority;
							if (max <eff)
								max = eff;
				
						}
						HoldResource.effectivePriority=Math.max(HoldResource.priority, max);
						
						HoldResource.CorrectPriority();
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
		public void waitForAccess(ThreadPriorityQueue waitQueue) {
			// implement me
			this.age = Machine.timer().getTime();
			waitQueue.PQueue.add(this);
			
			if(waitQueue.transferPriority==true)
			{
				HoldingQueues.add(waitQueue);
				if(waitQueue.resourceHolder != null)
				{
					waitQueue.resourceHolder.WaitingTSQueue.add(this);
					//waitQueue.resourceHolder.CorrectPriority();
					CorrectPriority();
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
		public void acquire(ThreadPriorityQueue waitQueue) {
			// implement me
			
			if(waitQueue.transferPriority)
			{
				waitQueue.resourceHolder = this;
				
			}
			
		}

		/** The thread with which this object is associated. */
		protected KThread thread;

		/** The priority of the associated thread. */
		//protected int priority;
		protected long age = Machine.timer().getTime();
		/** The priority of the associated thread. */
		protected int priority = priorityDefault;
		
		protected int effectivePriority =priorityDefault;
		
		protected LinkedList<ThreadPriorityQueue> HoldingQueues = new LinkedList<ThreadPriorityQueue>();

		protected LinkedList<ThreadState> WaitingTSQueue = new LinkedList<ThreadState>();		
	
	}
}
