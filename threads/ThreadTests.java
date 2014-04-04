/**
 * 
 */
package nachos.threads;

import nachos.ag.BoatGrader;
import nachos.machine.Machine;


/**
 * @author xuke
 *
 */
public class ThreadTests {
	public ThreadTests()
	{
	
	}
	
	//Thread classes for testing
	private static class PingTest implements Runnable {
		PingTest(int which) {
		    this.which = which;
		}
		
		public void run() {
		    for (int i=0; i<5; i++) {
			System.out.println("*** thread " + which + " looped "
					   + i + " times");
			KThread.currentThread().yield();
		    }
		}

		private int which;
	    }

	    private static class Listener implements Runnable {
	    	static int id = 0;
	    	private int localID;
	    	Listener(Communicator comm)
	    	{
	    		communicator = comm;
	    		localID = id++;
	    	}
			public void run() {
				System.out.println("L" + localID + ":I'm listening for a word");
				System.out.println("L" + localID + ":I got the word -> " + communicator.listen());
				
			}
	    	private Communicator communicator;
	    }
	    private static class Speaker implements Runnable {
	    	static int id = 0;
	    	private int localID;
	    	Speaker(Communicator comm)
	    	{
	    		localID= id++;
	    		communicator = comm;
	    	}
			public void run() {
				System.out.println("S" + localID + ":I'm Speaking my ID number -> " + localID);
				communicator.speak(localID);
				
			}
	    	private Communicator communicator;
	    }
	    private static class ThreadHogger implements Runnable{
	    	public int d = 0;
	    		public void run() {
	    			while(d==0){KThread.yield();}
	
	    	}
	    
	    }
	    
	//test cases
	//Simple join test
	public static void joinTest1()
	{
		System.out.println("JOIN TEST #1: Start");
		KThread ping1 = new KThread(new PingTest(1));
		ping1.fork();
		ping1.join();
		new PingTest(0).run();
		System.out.println("JOIN TEST #1: Finished");
	}
	
	//join a thread after is has already finished
	public static void joinTest2()
	{
		System.out.println("JOIN TEST #2: Start");
		KThread ping1 = new KThread(new Runnable()
		{
			public void run() {
				System.out.println("Thread finnishing");
			}
		});
		ping1.fork();
		KThread.yield();
		ping1.join();
		new PingTest(0).run();
		
		System.out.println("JOIN TEST #2: Finished");
	}	
	
	//Test priority donation with join
	public static void joinTest3()
	{
		System.out.println("JOIN TEST #3: Start");
		KThread ping1 = new KThread(new PingTest(1));
		//use the thread hogger to loop forever if it ever gets a change to run
		ThreadHogger th = new ThreadHogger();
		KThread hoggingThread = new KThread(th);
		Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(7);
		ThreadedKernel.scheduler.setPriority(ping1, 4);
		ThreadedKernel.scheduler.setPriority(hoggingThread, 6);
		Machine.interrupt().enable();
		
		ping1.fork();
		hoggingThread.fork();
		System.out.println("joining a low priority thread...");
		ping1.join(); //without priority donation this join will never happen
		new PingTest(0).run();
		th.d = 1;//stop the thread hogger from running
		System.out.println("JOIN TEST #3: Finished");
	}	
	
	public static void alarmTest1()
	{
		/*
		 *  Picks a certain amount of ticks between 0 and 1 million and calls 
		 *  Alarm.waitUntil with this amount of ticks. Does this several times
		 *  just to show that it works properly.
		 */
		long ticks;
		Alarm test = new Alarm();
		for (int i =0;i<5;i++)
		{
			ticks=(long)(Math.random()*1000000);
			System.out.println("I'm about to wait for " + ticks + " ticks.");
			test.waitUntil(ticks);
			System.out.println(ticks + " ticks later, I'm done waiting!");
		}
	}
	public static void communicatorTest1()
	{
		/*
		 * Tests 1 listener and 1 speaker, listener spawns first
		 */
		Communicator comm = new Communicator();
		new KThread(new Listener(comm)).fork();
		new KThread(new Speaker(comm)).fork();
		
	}
	public static void communicatorTest2()
	{
		/*
		 * Tests 1 listener and 1 speaker, speaker spawns first
		 */
		Communicator comm = new Communicator();
		new KThread(new Speaker(comm)).fork();
		new KThread(new Listener(comm)).fork();
		
	}
	public static void communicatorTest3()
	{
		/*
		 * Tests many listeners and one speaker
		 */
		Communicator comm = new Communicator();
		new KThread(new Speaker(comm)).fork();
		new KThread(new Listener(comm)).fork();
		new KThread(new Listener(comm)).fork();
		new KThread(new Speaker(comm)).fork();
		new KThread(new Listener(comm)).fork();
		new KThread(new Listener(comm)).fork();
		new KThread(new Speaker(comm)).fork();
		new KThread(new Speaker(comm)).fork();
		
	}
	public static void conditionTest1()
	{
		/*
		 * Tests condition2 by spawning a thread that goes to sleep and
		 * is waken up by the main thread
		 */
		System.out.println("Condition TEST #1: Start");
		final Lock lock = new Lock();
		final Condition2 condition = new Condition2(lock);
		KThread thread1 = new KThread(new Runnable(){
			public void run(){
				lock.acquire();
				System.out.println("Thread is going to sleep");
				condition.sleep();
				System.out.println("Thread has been woken up");
				lock.release();
			}
		});	
		KThread thread2 = new KThread(new Runnable(){
			public void run(){
				lock.acquire();
				System.out.println("Thread is going to sleep");
				condition.sleep();
				System.out.println("Thread has been woken up");
				lock.release();
			}
		});	
		KThread thread3 = new KThread(new Runnable(){
			public void run(){
				lock.acquire();
				System.out.println("Thread is going to sleep");
				condition.sleep();
				System.out.println("Thread has been woken up");
				lock.release();
			}
		});	
		thread1.fork();
		thread2.fork();
		thread3.fork();
		System.out.println("Main: yielding to run the other thread");
		KThread.yield();
		System.out.println("Main: sending the wake signal then yeilding");
		lock.acquire();
		condition.wakeAll();
		lock.release();
		KThread.yield();
		System.out.println("Condition TEST #1: End");
	}
	
	public static void priorityTest1()
	{
		/*
		 * Tests priority donation by attempting to create a deadlock
		 */
		System.out.println("Priority TEST #1: Start");
		final Lock theBestLock = new Lock();
		theBestLock.acquire();
		KThread thread1 = new KThread(new Runnable(){
			public void run(){
				System.out.println("Important thread wants the lock");
				theBestLock.acquire();
				System.out.println("Important thread got what it wanted");
				theBestLock.release();
			}
		});
		
		ThreadHogger th = new ThreadHogger();
		KThread thread2 = new KThread(th);
		Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(3);
		ThreadedKernel.scheduler.setPriority(thread1, 7);
		ThreadedKernel.scheduler.setPriority(thread2, 4);
		Machine.interrupt().enable();
		thread1.fork();
		thread2.fork();
		//cant get back without donation
		KThread.yield();
		System.out.println("Main thread letting go of the lock");
		theBestLock.release();
		th.d = 1;
		KThread.yield();
		System.out.println("Priority TEST #1: END");
	}
	
	public static void priorityTest2()
	{
		/*
		 * Creates 3 threads with different priorities and runs them
		 */
		System.out.println("Priority TEST #2: START");
		KThread thread1 = new KThread(new Runnable(){
			public void run(){
				System.out.println("Im first to run");
			}
		});
		KThread thread2 = new KThread(new Runnable(){
			public void run(){
				System.out.println("Im Second to run");
			}
		});
		KThread thread3 = new KThread(new Runnable(){
			public void run(){
				System.out.println("Im Third to run");
			}
		});
		Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(3);
		ThreadedKernel.scheduler.setPriority(thread1, 7);
		ThreadedKernel.scheduler.setPriority(thread2, 5);
		ThreadedKernel.scheduler.setPriority(thread3, 4);
		Machine.interrupt().enable();
		
		thread3.fork();
		thread2.fork();
		thread1.fork();
		KThread.yield();
		System.out.println("Priority TEST #2: END");
	}
	public static void boatTest(int children, int adults)
	{
		/*
		 * Tests the boat simulator the the given number of children and adults
		 */
		Boat.begin(children, adults, new BoatGrader());
	}


}
