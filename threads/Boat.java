package nachos.threads;
import nachos.ag.BoatGrader;
import java.util.LinkedList;
import nachos.machine.*;


public class Boat
{
	static BoatGrader bg;

	public static void selfTest()
	{
		BoatGrader b = new BoatGrader();

		System.out.println("\n ***Testing Boats with only 2 children***");
		begin(0, 2, b);

	}

	public static void begin( int adults, int children, BoatGrader b )
	{
		bg = b;

		// Instantiate global variables here
		int tc = children;  //total children
		int ta = adults;    //total adults
		moloChildCount = 0;  
		moloAdultCount = 0;   
		oahuChildCount = 0; 
		oahuAdultCount = 0;	
		lastoahuChildCount = 0;
		lastoahuAdultCount = 0;	
		lastmoloChildCount = 0;
		lastmoloChildCount = 0;
		myLock = new Lock(); //lock
		oahuWaitAdult = new Condition(myLock);  //conditions
		oahuWaitChildren = new Condition(myLock);
		moloWaitChildren = new Condition(myLock);
		boatWait = new Condition(myLock);
		problemOfBoat = new Condition(myLock);
		positionOfBoat = "Oahu"; //initialize boat position
		childrenWait = new LinkedList<KThread>();

		for(int i=0; i<adults; i++){
			Runnable r = new Runnable() {
				public void run() {
					AdultItinerary();
				}
			};
			KThread t = new KThread(r);
			t.setName("Adult Thread on Oahu");
			t.fork(); 	
		}

		for(int j=0; j<children; j++){
			Runnable r = new Runnable() {
				public void run() {
					ChildItinerary();
				}
			};
			KThread t = new KThread(r);
			t.setName("Child Thread on Oahu");
			t.fork(); 
		}

		complete = false;
		boatTring = false;
		isOnBoat = false;
		waitedPerson = false;
		myLock.acquire();
		while(!done(tc, ta))
		{ problemOfBoat.sleep();}
		
		complete = true; 
		oahuWaitChildren.wakeAll();
		moloWaitChildren.wakeAll();
		oahuWaitAdult.wakeAll();
		myLock.release();
	}
	
	static void ChildItinerary() //children thread
	{
		myLock.acquire();
		oahuChildCount++;
		while (!complete){
			while (situationChild() == 0){      
				if (positionOfBoat.equals("Oahu"))
				{
					oahuWaitChildren.wake(); 
					moloWaitChildren.sleep();
				}
				else
				{
					moloWaitChildren.wake(); 
					oahuWaitChildren.sleep();
				}
				oahuWaitAdult.wake(); //try waking up adult
			}	
			if (!complete){
				goChild(positionOfBoat); 	//update
				
				if (mayDone())
				{    
					problemOfBoat.wake();
					oahuWaitAdult.wake(); 
					moloWaitChildren.sleep();
				}
				else
				{
					if (boatTring)
					{
						oahuWaitChildren.wake();						
						isOnBoat = true;
						boatTring = false;
						boatWait.sleep();			
					}
					else
					{
						if (positionOfBoat.equals("Oahu"))
						{
							oahuWaitChildren.wake();
							oahuWaitAdult.wake();
							oahuWaitChildren.sleep();
						}
						else
						{
							if (isOnBoat)
							{
								boatWait.wake();
								isOnBoat = false;
							}
							moloWaitChildren.wake();	
							oahuWaitAdult.wake();
							moloWaitChildren.sleep(); 
						}
					}
				}
			}
		}
		myLock.release();
	}
	
	
	static void AdultItinerary()
	{
		myLock.acquire();
		oahuAdultCount++; 		
		while (situationAdult() == 0){
			if ( positionOfBoat.equals("Oahu"))
				oahuWaitChildren.wake();
			else
				moloWaitChildren.wake();
			oahuWaitAdult.sleep();  
		}
		goAdult(positionOfBoat); 	 
		moloWaitChildren.wake(); 	//try waking up child on Molokai
		myLock.release();
	}

	public static boolean mayDone(){
		if (lastoahuAdultCount == 0 && lastoahuChildCount == 0 && positionOfBoat.equals("Molokai"))
			return true;
		else
			return false;
	}
	
	public static boolean done(int tc, int ta){
		if (moloAdultCount == ta && moloChildCount == tc && positionOfBoat.equals("Molokai"))
			return true;
		else
			return false;
	}
	
	public static void goChild(String position){
		if (position.equals("Oahu")){
			if (!waitedPerson){ //if false, then nobody wait
				oahuChildCount--;
				lastoahuAdultCount = oahuAdultCount;
				lastoahuChildCount = oahuChildCount;
				
				bg.ChildRowToMolokai();		// pilot is child
			
				KThread.currentThread().setName("Child Thread on Boat");
				
				boatTring = true;
				waitedPerson = true;
				childrenWait.add(KThread.currentThread());
			}else{ 
				oahuChildCount--; 	
				lastoahuChildCount = oahuChildCount;
				lastoahuAdultCount = oahuAdultCount;
				bg.ChildRideToMolokai(); 	// passenger child to Molo		
				positionOfBoat = "Molokai"; 	// there are two children on Molokai
				KThread.currentThread().setName("Child Thread on Molokai");
				KThread kidFirst = childrenWait.removeFirst();
				kidFirst.setName("Child Thread on Molokai");
				moloChildCount = moloChildCount + 2; //update for both children
				waitedPerson = false;
			}
		}else{ 
			moloChildCount--;
			lastmoloChildCount = moloChildCount;
			lastmoloAdultCount = moloAdultCount;

			bg.ChildRowToOahu();
			oahuChildCount++;
			KThread.currentThread().setName("Child Thread on Oahu");
			positionOfBoat = "Oahu";
		}
	}

	public static void goAdult(String position){
		if (position.equals("Oahu")){
			oahuAdultCount--;
			lastoahuAdultCount = oahuAdultCount;
			lastoahuChildCount = oahuChildCount;

			bg.AdultRowToMolokai();
			moloAdultCount++;
			positionOfBoat = "Molokai"; 
			KThread.currentThread().setName("Adult Thread on Molokai");

		}
	}


	public static int situationChild(){
		if( positionOfBoat.equals("Oahu") ){
			if( KThread.currentThread().getName().equals("Child Thread on Oahu") && childrenWait.size() < 2)
				return 1; // Oahu boat still has seats, children can go
			else
				return 0;//wait
		}else{
			if( KThread.currentThread().getName().equals("Child Thread on Molokai")) 
				return 1;
			else
				return 0; 
		}
	}

	public static int situationAdult(){
		if( positionOfBoat.equals("Oahu") ){
			if( KThread.currentThread().getName().equals("Adult Thread on Oahu") && !waitedPerson && lastmoloChildCount > 0 )			
				return 1; //go
			else
				return 0;	//wait
		}else
			return 0; //wait
	}
	
	

	static void SampleItinerary()
	{
		System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
		bg.AdultRowToMolokai();
		bg.ChildRideToMolokai();
		bg.AdultRideToMolokai();
		bg.ChildRideToMolokai();
	}

	
	private static Condition problemOfBoat;
	private static Lock myLock;
	private static String positionOfBoat; 
	private static int lastmoloChildCount; 
	private static int lastoahuAdultCount;
	private static int lastoahuChildCount;
	private static int oahuChildCount; 
	private static int oahuAdultCount;   
	private static int moloChildCount;
	private static int moloAdultCount;
	private static LinkedList<KThread> childrenWait;
	private static boolean complete;
	private static boolean waitedPerson; 
	private static int lastmoloAdultCount; 
	private static Condition oahuWaitAdult;
	private static Condition oahuWaitChildren;
	private static Condition moloWaitChildren;
	private static Condition boatWait;
	private static boolean boatTring;
	private static boolean isOnBoat;
}